package com.bits.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bits.app.data.BitsRepository
import com.bits.app.data.BitsState
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

/**
 * Everything about how the widgets look and what they carry, in one place.
 *
 * This is what the home page's Edit button and the widget's Customise icon both open.
 *
 * Naming, ordering and deleting lists used to sit in a separate block above this, which
 * meant the same list of categories appeared twice on one page: once to manage, once to
 * choose from. Those controls now live on the category rows inside each widget card, so
 * a list is renamed in the same place it is switched on.
 */
@Composable
fun CustomizeScreen(
    state: BitsState,
    repository: BitsRepository,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    var clockPreview by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BitsColors.Ink)
            }
            Text("Edit", style = BitsText.Brand, modifier = Modifier.padding(start = 4.dp))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            // Customising is where most of the Pro perks actually live, so the pitch
            // belongs here as much as in Settings.
            ProBanner(state = state, onOpenPaywall = onOpenPaywall)

            Spacer(Modifier.height(18.dp))

            WidgetListsSection(
                state = state,
                repository = repository,
                onOpenPaywall = onOpenPaywall,
                clockPreview = clockPreview,
                onPreviewClock = { clockPreview = it },
            )
        }
    }
}
