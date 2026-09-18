package com.bits.app.data

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The one switch that turns paid Pro on.
 *
 * Bits 1.7 ships free: every perk is unlocked for everyone, and the Pro page is a
 * thank-you rather than a shop. The billing implementation is written and complete
 * (see PlayProStore) - it simply isn't switched on.
 *
 * To start charging, in order:
 *
 *  1. In Play Console, create the two products using EXACTLY the ids below. A typo here
 *     is silent: the product just never loads, and the buy button does nothing.
 *       - one-time product, id `bits_pro_lifetime`
 *       - subscription,     id `bits_pro_monthly`  (with a base plan)
 *  2. Flip [ENABLED] to true and ship an update.
 *  3. Test with a licence-tester account before the update reaches anyone else.
 *     Purchases made by licence testers are real flows but are not charged.
 *
 * Nothing else needs changing. The gates in [BitsState] and the Pro page already read
 * through this object.
 */
object Monetization {

    /** False means Bits is free and no billing code ever runs. */
    const val ENABLED = false

    /** Must match the one-time product id in Play Console, character for character. */
    const val LIFETIME_PRODUCT_ID = "bits_pro_lifetime"

    /** Must match the subscription id in Play Console, character for character. */
    const val MONTHLY_PRODUCT_ID = "bits_pro_monthly"

    /**
     * Whether people who installed Bits while it was free keep everything, for good,
     * after Pro starts costing money.
     *
     * This is why [Preferences.foundingUser] is recorded from day one even though
     * nothing reads it yet: who was an early user cannot be worked out later from a
     * database nobody kept. Recording it now costs a boolean; not recording it means
     * the choice below is gone forever.
     *
     * Set this to false if early installs should lose their perks when Pro goes live.
     * Consider that they will have been using those games and themes for months.
     */
    const val GRANDFATHER_EARLY_USERS = true
}

/**
 * Digs the hosting Activity out of a Compose context.
 *
 * Play will not open a purchase sheet from an application context, and the context
 * Compose hands you is usually a wrapper rather than the Activity itself, so it has to
 * be unwrapped. Returns null rather than throwing: an app being torn down mid-tap is a
 * thing that happens, and it isn't worth a crash.
 */
fun android.content.Context.findActivity(): Activity? {
    var context: android.content.Context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/** One purchasable plan, as Play describes it, with the real localised price. */
data class ProOffer(
    val productId: String,
    /** [ProPlan.LIFETIME] or [ProPlan.MONTHLY]. */
    val plan: String,
    /** Play's own formatted price for this user's country, e.g. "₹229" or "$4.99". */
    val formattedPrice: String,
    /** Subscriptions need the offer token to start a purchase. Null for one-time buys. */
    val offerToken: String?,
)

/**
 * Where Pro plans come from and how one gets bought.
 *
 * Deliberately a plain interface with no Play Billing types anywhere in it, so the rest
 * of the app never imports the billing library. [PlayProStore] is the only file that
 * does, which keeps the dependency in exactly one place.
 */
interface ProStore {
    /** The plans on sale, with live prices. Empty until Play answers, or if it never does. */
    val offers: StateFlow<List<ProOffer>>

    /** Whether Play Billing is usable on this device at all. */
    val available: StateFlow<Boolean>

    fun start()
    fun stop()

    /** Opens Play's purchase sheet. Needs a real Activity; Play will not accept less. */
    fun purchase(activity: Activity, productId: String)

    /** Re-checks what this account owns, e.g. after a reinstall. */
    fun restore()
}

/**
 * The store used while [Monetization.ENABLED] is false: there is nothing to sell, so it
 * reports nothing and does nothing. No billing connection is ever opened.
 */
object FreeStore : ProStore {
    private val noOffers = MutableStateFlow<List<ProOffer>>(emptyList())
    private val notAvailable = MutableStateFlow(false)

    override val offers: StateFlow<List<ProOffer>> = noOffers.asStateFlow()
    override val available: StateFlow<Boolean> = notAvailable.asStateFlow()

    override fun start() = Unit
    override fun stop() = Unit
    override fun purchase(activity: Activity, productId: String) = Unit
    override fun restore() = Unit
}
