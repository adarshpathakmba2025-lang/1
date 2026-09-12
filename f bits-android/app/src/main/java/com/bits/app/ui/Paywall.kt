package com.bits.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bits.app.R
import com.bits.app.data.BitsState
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

private const val FOUNDER_MESSAGE = "Hey, I'm \u0101darsh! I initially built this app for myself, but once it took shape, I knew I wanted to share it with everyone for free. It runs entirely offline, meaning your data stays strictly on your local device \u2014 no cloud uploads, no tracking, and never for sale.\n\nWhen you upgrade to Pro, you're not just unlocking extra features; you're directly keeping a tiny independent project alive and evolving. That genuinely means the world to me. Thank you for being here! ^_^"

private const val THANK_YOU_MESSAGE = "You went Pro \u2014 thank you, really. Bits is a tiny independent project, and you're the reason it keeps growing.\n\nEverything's unlocked below. Your data still never leaves your device. If there's something you'd love to see in Bits, I'd genuinely like to hear it. ^_^"

/** Shown once before the Pro page. Pro users get a thank-you instead of a pitch. */
@Composable
fun FounderDialog(isPro: Boolean, onContinue: () -> Unit) {
    Dialog(onDismissRequest = onContinue) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BitsColors.PanelBase)
                .padding(22.dp)
        ) {
            Text(if (isPro) THANK_YOU_MESSAGE else FOUNDER_MESSAGE, style = BitsText.Body)
            Row(Modifier.padding(top = 18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledAction(if (isPro) "See what's unlocked" else "Continue", onClick = onContinue)
            }
        }
    }
}

private data class Perk(val title: String, val body: String, val icon: Int)

private val perks = listOf(
    Perk("Four more mini-games", "Memory Match, X and O, Word Guess and Flappy, all unlocked.", R.drawable.ic_game_flappy),
    Perk("Eight widget themes", "Full palettes, not just accent colours. Try any before you buy.", R.drawable.ic_theme),
    Perk("Premium clock styles", "Stacked, monospace, statement and seconds.", R.drawable.ic_clock),
    Perk("Independent widget lists", "Give each widget its own categories.", R.drawable.ic_widget_add),
    Perk("Keeps Bits going", "No ads, no tracking, no subscriptions required. Just this.", R.drawable.ic_heart),
)

@Composable
fun PaywallScreen(state: BitsState, onBack: () -> Unit, onPurchase: () -> Unit) {
    var selectedPlan by remember { mutableStateOf("lifetime") }
    var note by remember { mutableStateOf(false) }
    val isPro = state.preferences.isPro

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = BitsColors.Muted)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Text(
                text = if (isPro) "You're Pro" else "Bits Pro",
                style = BitsText.Small.copy(color = BitsColors.Amber),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = if (isPro) "Everything's unlocked.\nThank you." else "Everything in Bits,\nunlocked for good.",
                style = BitsText.Title,
            )

            Spacer(Modifier.height(24.dp))

            perks.forEach { perk ->
                Row(Modifier.padding(bottom = 18.dp)) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BitsColors.Amber.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painter = painterResource(perk.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(perk.title, style = BitsText.BodyBold)
                        Text(perk.body, style = BitsText.Small, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            if (!isPro) {
                Spacer(Modifier.height(4.dp))
                PlanCard(
                    title = "Lifetime",
                    price = "\u20b9229",
                    caption = "Pay once. Yours forever.",
                    badge = "BEST VALUE",
                    selected = selectedPlan == "lifetime",
                    onClick = { selectedPlan = "lifetime" },
                )
                Spacer(Modifier.height(10.dp))
                PlanCard(
                    title = "Monthly",
                    price = "\u20b949",
                    caption = "Billed monthly. Cancel any time.",
                    badge = null,
                    selected = selectedPlan == "monthly",
                    onClick = { selectedPlan = "monthly" },
                )
            }

            if (note) {
                Text(
                    text = "Payments aren't switched on yet \u2014 this page is a preview of what's coming.",
                    style = BitsText.Small.copy(color = BitsColors.Muted),
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        if (!isPro) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(BitsColors.PanelBase)
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Text(
                    text = if (selectedPlan == "lifetime") "ONE-TIME \u00b7 YOURS FOREVER" else "MONTHLY \u00b7 CANCEL ANY TIME",
                    style = BitsText.Small.copy(color = BitsColors.Muted),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (selectedPlan == "lifetime") "Get Lifetime \u2014 \u20b9229" else "Start Monthly \u2014 \u20b949",
                    style = BitsText.BodyBold.copy(color = BitsColors.Bg),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BitsColors.Amber)
                        .clickable {
                            note = true
                            onPurchase()
                        }
                        .padding(vertical = 15.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    caption: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BitsColors.Amber.copy(alpha = 0.12f) else BitsColors.PanelBase.copy(alpha = 0.6f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) BitsColors.Amber else Color.Transparent)
                .border(1.5.dp, if (selected) BitsColors.Amber else BitsColors.Muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = BitsColors.Bg, modifier = Modifier.size(13.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = BitsText.BodyBold)
                if (badge != null) {
                    Text(
                        text = badge,
                        style = BitsText.Small.copy(color = BitsColors.Bg),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(BitsColors.Amber)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Text(caption, style = BitsText.Small, modifier = Modifier.padding(top = 3.dp))
        }
        Text(price, style = BitsText.Subtitle.copy(color = if (selected) BitsColors.Amber else BitsColors.Ink))
    }
}
