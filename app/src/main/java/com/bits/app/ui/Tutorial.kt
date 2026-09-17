package com.bits.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    const val GAMES = "games"
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

private data class TourStep(val title: AnnotatedString, val body: String, val target: String?)

private fun plain(text: String) = AnnotatedString(text)

/** “Tomorrow ~never~ comes.” — all italic; “never” is regular weight and struck through, the rest bold. */
private val tomorrowNeverComes = buildAnnotatedString {
    withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) { append("\u201CTomorrow ") }
    withStyle(
        SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, textDecoration = TextDecoration.LineThrough)
    ) { append("never") }
    withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)) { append(" comes.\u201D") }
}

private val tourSteps = listOf(
    TourStep(
        title = plain("Welcome to Bits!"),
        body = "One box for everything you don\u2019t wanna forget \u2014 tasks, book lists, ideas that usually slip out. Here\u2019s a quick walkthrough\u2026",
        target = null,
    ),
    TourStep(
        title = plain("Add literally anything\u2026"),
        body = "Here is where you type the \u201Cbits\u201D and pieces of your life and add \u2019em in. It\u2019s all divided category-wise.",
        target = TutorialTarget.CAPTURE,
    ),
    TourStep(
        title = plain("Task Completed"),
        body = "Once you\u2019ve completed a bit, you can check it off by tapping on the box. Tap the text to edit or delete it.",
        target = TutorialTarget.ITEM,
    ),
    TourStep(
        title = plain("Put things in order"),
        body = "Press and drag this handle to reposition it vertically.",
        target = TutorialTarget.HANDLE,
    ),
    TourStep(
        title = plain("Categories and lists"),
        body = "You can maintain separate lists under multiple categories for all the widgets you maintain. Swipe sideways to flip between them.",
        target = TutorialTarget.CHIPS,
    ),
    TourStep(
        title = tomorrowNeverComes,
        body = "Anything unfinished in Tomorrow automatically moves into Today when the clock hits midnight.",
        target = TutorialTarget.CHIPS,
    ),
    TourStep(
        title = plain("Shift it across"),
        body = "Tap a bit, then use the arrows to send it to the category on either side.",
        target = TutorialTarget.ITEM,
    ),
    TourStep(
        title = plain("Make it yours"),
        body = "Tap Edit for everything about your lists and widgets \u2014 add, rename, reorder or remove lists, and change each widget\u2019s theme, clock and look.",
        target = TutorialTarget.EDIT,
    ),
    TourStep(
        title = plain("Find anything"),
        body = "I guess it\u2019s kinda self-explanatory\u2026",
        target = TutorialTarget.SEARCH,
    ),
    TourStep(
        title = plain("Keep it in your reach"),
        body = "Long-press an empty spot, tap Widgets, and look for Bits. You can tick items off and edit them right from the widget itself!",
        target = null,
    ),
    TourStep(
        title = plain("Bored? Take a break"),
        body = "Tap the little controller for a quick game. It\u2019s on your widget too, bottom left!",
        target = TutorialTarget.GAMES,
    ),
    TourStep(
        title = plain("The other stuff"),
        body = "Backups, the tour, and all that boring complicated stuff lives in here.",
        target = TutorialTarget.SETTINGS,
    ),
)

@Composable
fun TutorialOverlay(targets: TutorialTargets, onFinish: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = tourSteps[index]
    val isLast = index == tourSteps.lastIndex
    val rect = step.target?.let { targets.bounds[it] }

    val goBack = { if (index > 0) index -= 1 }
    // On the last step, only "Get started" finishes the tour.
    val goNext = { if (!isLast) index += 1 }

    BackHandler {
        if (index > 0) index -= 1 else onFinish()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
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

        // Tap zones: the left 40% of the screen goes back, the right 60% goes forward.
        // They also block taps from reaching the app underneath while the tour is open.
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
                    .semantics { contentDescription = "Previous step" }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { goBack() }
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .semantics { contentDescription = if (isLast) "Last step" else "Next step" }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { goNext() }
            )
        }

        // The card has no tap handler of its own, so taps on its text fall through to the zones
        // beneath it. Only its buttons (Skip tour, Get started) catch taps directly.
        // The card wears the same arcade frame as the games section: square corners
        // and a solid offset shadow.
        Box(
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
        ) {
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
                    .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 10.dp)
            ) {
                // Square pixel blips instead of round dots.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tourSteps.indices.forEach { i ->
                        Spacer(
                            Modifier
                                .size(if (i == index) 16.dp else 6.dp, 6.dp)
                                .background(if (i == index) Arcade.Glow else BitsColors.Muted.copy(alpha = 0.45f))
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // The frame is pixel-styled; the words stay in the readable body face.
                Text(step.title, style = BitsText.Subtitle.copy(color = Arcade.Glow))
                Text(
                    text = step.body,
                    style = BitsText.Body.copy(color = BitsColors.Ink),
                    modifier = Modifier.padding(top = 8.dp, end = 6.dp),
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLast) {
                        Spacer(Modifier.weight(1f))
                        PixelButton("Get started", onClick = onFinish)
                    } else {
                        Text(
                            text = "Skip tour",
                            style = BitsText.Small.copy(color = BitsColors.Muted),
                            modifier = Modifier
                                .clickable(onClick = onFinish)
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (index == 0) "Tap right to continue" else "Tap left or right",
                            style = BitsText.Small.copy(color = BitsColors.Muted.copy(alpha = 0.75f)),
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
