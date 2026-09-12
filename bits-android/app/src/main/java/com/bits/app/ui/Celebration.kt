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
@Composable
fun ThemeUnlockDialog(onPick: (WidgetTheme) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf<WidgetTheme?>(null) }
    val lockedThemes = remember { WidgetThemes.all.filterNot { it.free } }

    Dialog(onDismissRequest = onDismiss) {
        Box {
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BitsColors.PanelBase)
                    .padding(22.dp)
            ) {
                Text("\u2728 You found something", style = BitsText.Subtitle.copy(color = BitsColors.Amber))
                Text(
                    text = "Nice tapping. Pick any one theme below and it's yours to keep \u2014 free, forever. Just the one, though!",
                    style = BitsText.Body,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))

                lockedThemes.chunked(2).forEach { pair ->
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { theme ->
                            val selected = chosen?.id == theme.id
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(theme.backgroundTint))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) Color(theme.accent) else BitsColors.Muted.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .clickable { chosen = theme }
                                    .padding(11.dp)
                            ) {
                                Text(theme.displayName, style = BitsText.Small.copy(color = Color(theme.ink)))
                                Spacer(Modifier.height(7.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(theme.accent, theme.ink, theme.doneColor).forEach { swatch ->
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(swatch))
                                        )
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                Row(Modifier.padding(top = 10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    val pick = chosen
                    if (pick == null) {
                        Text("Pick one to continue", style = BitsText.Small, modifier = Modifier.padding(12.dp))
                    } else {
                        FilledAction("Keep ${pick.displayName}") { onPick(pick) }
                    }
                }
            }
        }
    }
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
