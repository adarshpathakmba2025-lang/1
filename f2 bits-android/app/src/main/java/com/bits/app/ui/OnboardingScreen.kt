package com.bits.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bits.app.R
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import com.bits.app.widget.BitsWidgetReceiver
import kotlinx.coroutines.delay

private fun widgetCount(context: Context): Int =
    AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, BitsWidgetReceiver::class.java)).size

private fun requestPin(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, BitsWidgetReceiver::class.java), null, null)
    }
}

/**
 * The first thing a new user sees. Bits lives on the home screen, so this walks them
 * through placing the widget before anything else.
 *
 * Some launchers don't support the one-tap pin request, and a user can always decline
 * the system dialog, so there's a manual path and a way through regardless. Trapping
 * someone on this screen with no exit would be worse than an unplaced widget.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContextCompat()
    val canPin = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    val startCount = remember { widgetCount(context) }
    var asked by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }

    val count by produceState(initialValue = startCount) {
        while (true) {
            delay(700)
            value = widgetCount(context)
        }
    }
    val placed = count > startCount || startCount > 0

    // Once the widget lands on the home screen, celebrate briefly and move on.
    LaunchedEffect(placed) {
        if (placed) {
            delay(900)
            onDone()
        }
    }

    // After a while without success, offer the manual route rather than leaving them stuck.
    LaunchedEffect(asked) {
        if (asked) {
            delay(6000)
            showManual = true
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "scale",
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            text = if (placed) "That's it \u2014 you're set." else "First, put Bits\non your home screen",
            style = BitsText.Title,
            textAlign = TextAlign.Center,
        )
        Text(
            text = if (placed) {
                "Nice. Everything you add shows up there."
            } else {
                "Bits is built to be glanced at, not opened. The widget is the whole point."
            },
            style = BitsText.Small,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )

        Spacer(Modifier.height(28.dp))

        Image(
            painter = painterResource(R.drawable.widget_preview),
            contentDescription = "A preview of the Bits widget",
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(0.82f)
                .scale(if (placed) 1f else scale)
                .clip(RoundedCornerShape(26.dp)),
        )

        Spacer(Modifier.height(24.dp))

        if (placed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CheckVisual(checked = true, size = 22.dp)
                Text("Widget added", style = BitsText.BodyBold, modifier = Modifier.padding(start = 10.dp))
            }
        } else {
            if (canPin) {
                FilledAction(
                    text = if (asked) "Try again" else "Add the widget",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    asked = true
                    requestPin(context)
                }
            }

            AnimatedVisibility(
                visible = showManual || !canPin,
                enter = fadeIn() + slideInVertically { it / 3 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Or do it by hand: long-press an empty spot on your home screen, tap Widgets, and find Bits.",
                        style = BitsText.Small,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    TextAction("Skip for now", BitsColors.Muted, onDone)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/** Small indirection so this file doesn't need the Compose platform import twice. */
@Composable
private fun LocalContextCompat(): Context = androidx.compose.ui.platform.LocalContext.current
