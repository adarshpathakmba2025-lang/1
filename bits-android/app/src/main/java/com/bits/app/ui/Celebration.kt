package com.bits.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bits.app.data.WidgetTheme
import com.bits.app.data.WidgetThemes
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlin.math.sin
import kotlin.random.Random

private data class Confetto(val x: Float, val delay: Float, val drift: Float, val color: Color, val size: Float)

/**
 * The easter-egg reward: pick one paid theme to keep, free, forever.
 * Only reachable once per device.
 */
/**
 * The easter-egg reward: one theme, one game and one clock style, chosen together.
 *
 * Nothing is granted until all three are picked and Claim is tapped, and the claim is a
 * single atomic edit, so there is no way to take one reward now and come back for more.
 */
@Composable
fun EasterEggDialog(
    lockedThemes: List<WidgetTheme>,
    lockedGames: List<Pair<String, String>>,
    lockedClocks: List<Pair<String, String>>,
    onClaim: (themeId: String, gameId: String, clockId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var theme by remember { mutableStateOf<String?>(null) }
    var game by remember { mutableStateOf<String?>(null) }
    var clock by remember { mutableStateOf<String?>(null) }
    val ready = theme != null && game != null && clock != null

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BitsColors.PanelBase)
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Text("\u2728 You found something", style = BitsText.Subtitle.copy(color = BitsColors.Amber))
            Text(
                text = "Nice tapping. Pick one theme, one game and one clock style \u2014 yours to keep, free. One of each, one time only.",
                style = BitsText.Body,
                modifier = Modifier.padding(top = 8.dp),
            )

            PickerBlock("Theme") {
                lockedThemes.chunked(2).forEach { pair ->
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { option ->
                            SwatchOption(
                                label = option.displayName,
                                selected = theme == option.id,
                                background = Color(option.backgroundTint),
                                accent = Color(option.accent),
                                ink = Color(option.ink),
                                modifier = Modifier.weight(1f),
                            ) { theme = option.id }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            PickerBlock("Game") {
                lockedGames.chunked(2).forEach { pair ->
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { (id, label) ->
                            PlainOption(label, game == id, Modifier.weight(1f)) { game = id }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            PickerBlock("Clock style") {
                lockedClocks.chunked(2).forEach { pair ->
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pair.forEach { (id, label) ->
                            PlainOption(label, clock == id, Modifier.weight(1f)) { clock = id }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Row(Modifier.padding(top = 14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!ready) {
                    Text("Pick one of each", style = BitsText.Small, modifier = Modifier.padding(12.dp))
                } else {
                    FilledAction("Claim all three") { onClaim(theme!!, game!!, clock!!) }
                }
            }
        }
    }
}

@Composable
private fun PickerBlock(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        style = BitsText.Small.copy(color = BitsColors.Muted),
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
    content()
}

@Composable
private fun SwatchOption(
    label: String,
    selected: Boolean,
    background: Color,
    accent: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else BitsColors.Muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Text(label, style = BitsText.Small.copy(color = ink))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(accent, ink).forEach {
                Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(it))
            }
        }
    }
}

@Composable
private fun PlainOption(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        style = BitsText.Small.copy(color = if (selected) BitsColors.Amber else BitsColors.Ink),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BitsColors.Amber.copy(alpha = 0.16f) else BitsColors.Bg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.25f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    )
}

/** A short burst of falling pixels. Purely decorative, and it stops on its own. */
@Composable
fun ConfettiBurst(modifier: Modifier = Modifier, onFinished: () -> Unit = {}) {
    val pieces = remember {
        val random = Random(System.currentTimeMillis())
        val palette = listOf(
            Color(0xFFF2B544), Color(0xFF7FD68A), Color(0xFF5BD3D3),
            Color(0xFF9DBBFF), Color(0xFFCFA6FF), Color(0xFFF3A6B8),
        )
        List(34) {
            Confetto(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                drift = (random.nextFloat() - 0.5f) * 0.25f,
                color = palette[random.nextInt(palette.size)],
                size = 7f + random.nextFloat() * 9f,
            )
        }
    }

    var running by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { running = true }
    val transition = updateTransition(targetState = running, label = "confetti")
    val progress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 2200, easing = LinearEasing) },
        label = "fall",
    ) { if (it) 1f else 0f }

    LaunchedEffect(progress) { if (progress >= 1f) onFinished() }

    Canvas(modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val local = ((progress - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val x = (piece.x + piece.drift * local + sin(local * 6f) * 0.02f) * size.width
            val y = local * (size.height + 80f) - 40f
            drawRect(
                color = piece.color.copy(alpha = (1f - local).coerceIn(0f, 1f)),
                topLeft = Offset(x, y),
                size = Size(piece.size, piece.size),
            )
        }
    }
}
