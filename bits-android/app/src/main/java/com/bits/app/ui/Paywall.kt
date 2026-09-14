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
import androidx.compose.ui.platform.LocalContext
import com.bits.app.data.BitsState
import com.bits.app.data.ProPlan
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

private val FOUNDER_LINES = listOf(
    "Hey, I'm \u0101darsh. I built Bits for myself first \u2014 then it took shape, and I wanted everyone to have it free.",
    "It runs entirely offline. Your data stays on your device. No cloud, no tracking, never for sale.",
    "Going Pro unlocks the extras, but really it keeps a tiny independent project alive and growing. That means the world. ^_^",
)

private val THANK_YOU_LINES = listOf(
    "You went Pro. Bits is a tiny independent project, and you're the reason it keeps growing.",
    "Everything's unlocked below. Your data still never leaves your device.",
    "If there's something you'd love to see in Bits, I'd really like to hear it. ^_^",
)

/**
 * Shown once before the Pro page, in the arcade styling of the games section so it feels
 * like part of Bits rather than a standard dialog. Pro users get a thank-you instead.
 */
@Composable
fun FounderDialog(isPro: Boolean, onContinue: () -> Unit) {
    val lines = if (isPro) THANK_YOU_LINES else FOUNDER_LINES
    Dialog(onDismissRequest = onContinue) {
        Box {
            // Offset block behind the panel, the same trick the games screens use.
            Box(
                Modifier
                    .padding(start = 5.dp, top = 5.dp)
                    .matchParentSize()
                    .background(Color(0x99000000))
            )
            Column(
                Modifier
                    .padding(end = 5.dp, bottom = 5.dp)
                    .background(Arcade.Border)
                    .padding(2.dp)
                    .background(Arcade.Panel)
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isPro) "THANKS, REALLY" else "A NOTE FROM THE DEV",
                    style = BitsText.PixelBody.copy(color = Arcade.Glow),
                )
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Arcade.Border)
                )
                lines.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = BitsText.Body.copy(color = if (index == 0) BitsColors.Ink else BitsColors.Ink.copy(alpha = 0.82f)),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Text(
                    text = "\u2014 \u0101darsh",
                    style = BitsText.PixelBody.copy(color = BitsColors.Muted),
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                )
                PixelButton(
                    label = if (isPro) "See what's unlocked" else "Continue",
                    modifier = Modifier.align(Alignment.End),
                    onClick = onContinue,
                )
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

            if (state.preferences.proPlan == ProPlan.MONTHLY) {
                val context = LocalContext.current
                Text(
                    text = "Cancel membership",
                    style = BitsText.BodyBold.copy(color = BitsColors.Danger),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .clickable {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://play.google.com/store/account/subscriptions"),
                                )
                            )
                        },
                )
                Text(
                    "Opens Play Store subscription settings.",
                    style = BitsText.Small,
                    modifier = Modifier.padding(top = 3.dp),
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
