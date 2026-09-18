package com.bits.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.bits.app.data.Monetization
import com.bits.app.data.ProOffer
import com.bits.app.data.ProPlan
import com.bits.app.data.ProStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play Billing, in one file.
 *
 * This is the only place in Bits that imports the billing library. Everything else talks
 * to the [ProStore] interface, so if this file were deleted the rest of the app would
 * still build.
 *
 * It is complete but dormant: nothing constructs it while [Monetization.ENABLED] is
 * false. See that object for the switch-on steps.
 *
 * @param onEntitlement called whenever ownership is established or lost. Bits stores the
 *   result and reads it offline, so perks keep working with no signal - Play is asked
 *   again on the next launch that can reach it.
 */
class PlayProStore(
    context: Context,
    private val onEntitlement: (isPro: Boolean, plan: String) -> Unit,
) : ProStore, PurchasesUpdatedListener {

    private val appContext = context.applicationContext

    private val _offers = MutableStateFlow<List<ProOffer>>(emptyList())
    override val offers: StateFlow<List<ProOffer>> = _offers.asStateFlow()

    private val _available = MutableStateFlow(false)
    override val available: StateFlow<Boolean> = _available.asStateFlow()

    private var client: BillingClient? = null
    private var retries = 0

    /**
     * What this Google account owns, one flag per product type. Kept separately because
     * the two are queried separately and neither answer may be read as covering both.
     */
    private var ownsLifetime = false
    private var ownsMonthly = false

    /** Product details, keyed by product id, so a purchase can be started later. */
    private val productDetails = mutableMapOf<String, ProductDetails>()

    override fun start() {
        if (client != null) return
        val billing = BillingClient.newBuilder(appContext)
            .setListener(this)
            // Required from Billing 7: a one-time product can end up pending (cash
            // payment methods in some countries), and the library refuses to build
            // without being told the app understands that.
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        client = billing
        connect()
    }

    override fun stop() {
        client?.endConnection()
        client = null
        _available.value = false
    }

    private fun connect() {
        val billing = client ?: return
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retries = 0
                    _available.value = true
                    loadProducts()
                    restore()
                } else {
                    // Play Services missing or billing unsupported in this country: the
                    // Pro page shows its unavailable state rather than a dead button.
                    _available.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _available.value = false
                // Play kills the connection routinely (updates, low memory). Back off
                // rather than hammering it; anything owned is already stored locally, so
                // a gap here costs the user nothing.
                if (retries < MAX_RETRIES) {
                    retries++
                    connect()
                }
            }
        })
    }

    private fun loadProducts() {
        queryDetails(BillingClient.ProductType.INAPP, Monetization.LIFETIME_PRODUCT_ID)
        queryDetails(BillingClient.ProductType.SUBS, Monetization.MONTHLY_PRODUCT_ID)
    }

    private fun queryDetails(type: String, productId: String) {
        val billing = client ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(type)
                        .build()
                )
            )
            .build()

        // Billing 8 changed this callback: it hands back a QueryProductDetailsResult
        // rather than a plain list, so that products it could not fetch come back
        // separately with a reason instead of silently missing. On 7 this second
        // parameter was the list itself.
        billing.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            productDetailsResult.productDetailsList.forEach { productDetails[it.productId] = it }
            rebuildOffers()
        }
    }

    /** Turns whatever Play has told us about into the plain offers the Pro page shows. */
    private fun rebuildOffers() {
        val built = mutableListOf<ProOffer>()

        productDetails[Monetization.LIFETIME_PRODUCT_ID]?.let { details ->
            details.oneTimePurchaseOfferDetails?.let { offer ->
                built += ProOffer(
                    productId = details.productId,
                    plan = ProPlan.LIFETIME,
                    formattedPrice = offer.formattedPrice,
                    offerToken = null,
                )
            }
        }

        productDetails[Monetization.MONTHLY_PRODUCT_ID]?.let { details ->
            // A subscription can carry several offers (intro pricing, free trials). The
            // first is Play's own best-for-this-user pick, which is the right default.
            details.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
                val price = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice
                if (price != null) {
                    built += ProOffer(
                        productId = details.productId,
                        plan = ProPlan.MONTHLY,
                        formattedPrice = price,
                        offerToken = offer.offerToken,
                    )
                }
            }
        }

        _offers.value = built
    }

    override fun purchase(activity: Activity, productId: String) {
        val billing = client ?: return
        val details = productDetails[productId] ?: return

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                // Subscriptions must say which offer is being bought; one-time products
                // must not set a token at all.
                _offers.value.firstOrNull { it.productId == productId }?.offerToken
                    ?.let { setOfferToken(it) }
            }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        billing.launchBillingFlow(activity, flowParams)
    }

    override fun restore() {
        queryOwned(BillingClient.ProductType.INAPP)
        queryOwned(BillingClient.ProductType.SUBS)
    }

    private fun queryOwned(type: String) {
        val billing = client ?: return
        val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
        billing.queryPurchasesAsync(params) { result, purchases ->
            // Only a query that actually succeeded may take Pro away. A failed one means
            // we don't know, and "don't know" must never read as "didn't buy".
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync

            val owned = purchases.any { it.isUsable() }
            purchases.filter { it.isUsable() && !it.isAcknowledged }.forEach(::acknowledge)

            // Each query speaks only for its own product type. A successful "no
            // subscriptions" answer says nothing whatsoever about a lifetime purchase,
            // so the two are tracked separately and only combined below.
            when (type) {
                BillingClient.ProductType.INAPP -> ownsLifetime = owned
                BillingClient.ProductType.SUBS -> ownsMonthly = owned
            }
            emitEntitlement()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases.orEmpty().forEach { purchase ->
                    // PENDING is not ownership: the money hasn't moved yet. Play sends
                    // another update when it resolves.
                    if (!purchase.isUsable()) return@forEach

                    if (Monetization.LIFETIME_PRODUCT_ID in purchase.products) ownsLifetime = true
                    if (Monetization.MONTHLY_PRODUCT_ID in purchase.products) ownsMonthly = true

                    // Play refunds anything left unacknowledged for three days, so this
                    // is not optional bookkeeping - it is what keeps the purchase.
                    if (!purchase.isAcknowledged) acknowledge(purchase)
                }
                emitEntitlement()
            }

            // Already owned but not reflected here - usually a reinstall. Ask Play what
            // this account owns rather than treating it as a failure.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()

            // USER_CANCELED and the error codes need no action: nothing was bought, and
            // whatever was already owned is untouched.
            else -> Unit
        }
    }

    private fun Purchase.isUsable(): Boolean = purchaseState == Purchase.PurchaseState.PURCHASED

    /** Lifetime wins over monthly, being the one that cannot lapse. */
    private fun emitEntitlement() {
        val plan = when {
            ownsLifetime -> ProPlan.LIFETIME
            ownsMonthly -> ProPlan.MONTHLY
            else -> ProPlan.NONE
        }
        onEntitlement(plan != ProPlan.NONE, plan)
    }

    private fun acknowledge(purchase: Purchase) {
        val billing = client ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billing.acknowledgePurchase(params) { /* Play retries on the next query if this fails. */ }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
