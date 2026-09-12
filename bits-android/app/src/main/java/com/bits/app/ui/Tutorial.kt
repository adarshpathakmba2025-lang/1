package com.bits.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

/** Where each highlighted part of the home page is on screen. */
class TutorialTargets {
    val bounds = mutableStateMapOf<String, Rect>()
}

val LocalTutorialTargets = staticCompositionLocalOf<TutorialTargets?> { null }

object TutorialTarget {
    const val SEARCH = "search"
    const val CHIPS = "chips"
    const val EDIT = "edit"
    const val CAPTURE = "capture"
    const val ITEM = "item"
    const val HANDLE = "handle"
    const val SETTINGS = "settings"
}

/** Marks a part of the screen the tour can highlight. */
fun Modifier.tutorialTarget(key: String): Modifier = composed {
    val targets = LocalTutorialTargets.current
    if (targets == null) {
        Modifier
    } else {
        Modifier.onGloballyPositioned { coordinates ->
            val rect = coordinates.boundsInRoot()
            if (targets.bounds[key] != rect) targets.bounds[key] = rect
        }
    }
}

private data class TourStep(val title: String, val body: String, val target: String?)

private val tourSteps = listOf(
    TourStep(
        title = "Welcome to Bits",
        body = "One place for everything you don't want to forget: tasks, groceries, books, ideas, anything. Here's a quick tour.",
        target = null,
    ),
    TourStep(
        title = "Add anything",
        body = "Type here, then press Enter or tap Add. No forms, no setup. The keyboard stays open for the next one.",
        target = TutorialTarget.CAPTURE,
    ),
    TourStep(
        title = "Check it off",
        body = "Tap the box when something's done. Finished items stay visible, faded, so nothing gets lost. Tap the text to edit or delete it.",
        target = TutorialTarget.ITEM,
    ),
    TourStep(
        title = "Put things in order",
        body = "Press and drag this handle to move an item up or down.",
        target = TutorialTarget.HANDLE,
    ),
    TourStep(
        title = "Switch lists",
        body = "Each category is its own list. Today and Tomorrow are built in.",
        target = TutorialTarget.CHIPS,
    ),
    TourStep(
        title = "Plan with Tomorrow",
        body = "Anything unfinished in Tomorrow moves into Today at midnight, all by itself. No dates to set.",
        target = TutorialTarget.CHIPS,
    ),
    TourStep(
        title = "Make it yours",
        body = "Tap Edit to add, rename, delete, or reorder categories. The switch next to each one decides whether it shows on your widget.",
        target = TutorialTarget.EDIT,
    ),
    TourStep(
        title = "Find anything",
        body = "Search every list at once, including things you've already finished.",
        target = TutorialTarget.SEARCH,
    ),
    TourStep(
        title = "Keep it in view",
        body = "Add Bits to your home screen: long-press an empty spot, tap Widgets, and find Bits. You can tick items off right from the widget.",
        target = null,
    ),
    TourStep(
        title = "Settings",
        body = "Turn the widget clock on or off, change its transparency, clear finished items daily, back up your data, or replay this tour.",
        target = TutorialTarget.SETTINGS,
    ),
)

@Composable
fun TutorialOverlay(targets: TutorialTargets, onFinish: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = tourSteps[index]
    val isLast = index == tourSteps.lastIndex
    val rect = step.target?.let { targets.bounds[it] }

    BackHandler {
        if (index > 0) index -= 1 else onFinish()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            // Blocks taps on the app underneath while the tour is open.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
    ) {
        val cardAtTop = rect != null && rect.center.y > constraints.maxHeight * 0.5f

        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = BitsColors.Scrim)
            if (rect != null) {
                val pad = 6.dp.toPx()
                val topLeft = Offset(rect.left - pad, rect.top - pad)
                val holeSize = Size(rect.width + pad * 2, rect.height + pad * 2)
                val radius = CornerRadius(14.dp.toPx())
                drawRoundRect(color = Color.Black, topLeft = topLeft, size = holeSize, cornerRadius = radius, blendMode = BlendMode.Clear)
                drawRoundRect(color = BitsColors.Amber, topLeft = topLeft, size = holeSize, cornerRadius = radius, style = Stroke(width = 2.dp.toPx()))
            }
        }

        Column(
            modifier = Modifier
                .align(
                    when {
                        rect == null -> Alignment.Center
                        cardAtTop -> Alignment.TopCenter
                        else -> Alignment.BottomCenter
                    }
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BitsColors.PanelBase)
                .padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                tourSteps.indices.forEach { i ->
                    Spacer(
                        Modifier
                            .size(if (i == index) 18.dp else 6.dp, 6.dp)
                            .clip(CircleShape)
                            .background(if (i == index) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.5f))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(step.title, style = BitsText.Subtitle)
            Text(step.body, style = BitsText.Body, modifier = Modifier.padding(top = 6.dp, end = 8.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isLast) {
                    TextAction("Skip tour", BitsColors.Muted, onFinish)
                }
                Spacer(Modifier.weight(1f))
                if (index > 0) {
                    TextAction("Back", BitsColors.Ink) { index -= 1 }
                    Spacer(Modifier.width(4.dp))
                }
                FilledAction(if (isLast) "Get started" else "Next") {
                    if (isLast) onFinish() else index += 1
                }
            }
        }
    }
}
