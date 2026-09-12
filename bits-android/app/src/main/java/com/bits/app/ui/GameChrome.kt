package com.bits.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

/**
 * Shared retro chrome for the games section: hard square corners, chunky offset
 * borders, and the pixel typeface. Deliberately unlike the rest of the app.
 */
object Arcade {
    val Screen = Color(0xFF0B0F14)
    val Panel = Color(0xFF14202B)
    val Border = Color(0xFF3C4C5E)
    val Glow = Color(0xFFF2B544)
}

/** A square-cornered panel with a solid offset shadow, like an old UI frame. */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    fill: Color = Arcade.Panel,
    border: Color = Arcade.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier) {
        // Offset block behind, giving the raised arcade-cabinet look.
        Box(
            Modifier
                .padding(start = 4.dp, top = 4.dp)
                .matchParentSize()
                .background(Color(0x66000000))
        )
        Column(
            Modifier
                .padding(end = 4.dp, bottom = 4.dp)
                .background(border)
                .padding(2.dp)
                .background(fill)
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
fun PixelButton(
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = Arcade.Glow,
    onClick: () -> Unit,
) {
    Box(modifier) {
        Box(
            Modifier
                .padding(start = 3.dp, top = 3.dp)
                .matchParentSize()
                .background(Color(0x80000000))
        )
        Text(
            text = label.uppercase(),
            style = BitsText.PixelBody.copy(color = Arcade.Screen),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(end = 3.dp, bottom = 3.dp)
                .background(accent)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun ArcadeHeader(title: String, onBack: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BitsColors.Ink)
        }
        Text(
            text = title.uppercase(),
            style = BitsText.PixelHeading,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        trailing?.invoke()
    }
}

/** Score readout used across the games. */
@Composable
fun ScoreBar(score: Int, best: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScoreChip("SCORE", score, Arcade.Glow, Modifier.weight(1f))
        ScoreChip("BEST", best, BitsColors.Muted, Modifier.weight(1f))
    }
}

@Composable
private fun ScoreChip(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Arcade.Border)
            .padding(2.dp)
            .background(Arcade.Panel)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = BitsText.PixelBody)
        Spacer(Modifier.height(6.dp))
        Text(value.toString(), style = BitsText.PixelScore.copy(color = accent))
    }
}
