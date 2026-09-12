package com.bits.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bits.app.data.BitsRepository
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

private const val FOUNDER_MESSAGE = "Hey, I'm \u0101darsh! I initially built this app for myself, but once it took shape, I knew I wanted to share it with everyone for free. It runs entirely offline, meaning your data stays strictly on your local device \u2014 no cloud uploads, no tracking, and never for sale.\n\nWhen you upgrade to Pro, you're not just unlocking extra features; you're directly keeping a tiny independent project alive and evolving. That genuinely means the world to me. Thank you for being here! ^_^"

@Composable
fun FounderDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BitsColors.PanelBase)
                .padding(22.dp)
        ) {
            Text(FOUNDER_MESSAGE, style = BitsText.Body)
            Row(Modifier.padding(top = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextAction("Maybe later", BitsColors.Muted, onDismiss)
                Spacer(Modifier.width(4.dp))
                FilledAction("Continue", onClick = onContinue)
            }
        }
    }
}

@Composable
fun PaywallScreen(repository: BitsRepository, onBack: () -> Unit) {
    var billingNote by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(50)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BitsColors.Ink)
            }
            Text("Bits Pro", style = BitsText.Brand, modifier = Modifier.padding(start = 4.dp))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "A small thank-you that keeps Bits going",
                style = BitsText.Title.copy(color = BitsColors.Ink),
            )

            Column(Modifier.padding(top = 18.dp)) {
                Perk("A couple more mini-games", "Unlocks the extra games in the Games section as they're built.")
                Perk("Widget color themes", "Midnight, Forest, Ember, Ocean, and Royal, in Settings \u2192 Widget theme.")
                Perk("A second widget list \u2014 coming soon", "Its own categories, not tied to Today or Tomorrow.")
            }

            Column(
                Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BitsColors.Panel)
                    .padding(18.dp)
            ) {
                Text("One-time \u2014 \u20b9179", style = BitsText.Subtitle.copy(color = BitsColors.Amber))
                Text("Pay once, keep it always.", style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
                FilledAction(
                    text = "Get Bits Pro",
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    onClick = { billingNote = true },
                )
                Text(
                    text = "or \u20b949/month, cancel any time",
                    style = BitsText.Small,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { billingNote = true },
                )
            }

            if (billingNote) {
                Text(
                    text = "Payments aren't live yet \u2014 this screen previews what's coming. Once billing is set up, tapping here will complete a real purchase.",
                    style = BitsText.Small.copy(color = BitsColors.Muted),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Perk(title: String, body: String) {
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(title, style = BitsText.BodyBold)
        Text(body, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
    }
}
