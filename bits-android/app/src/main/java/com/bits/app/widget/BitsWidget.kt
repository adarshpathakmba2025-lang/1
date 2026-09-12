package com.bits.app.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bits.app.Launch
import com.bits.app.R
import com.bits.app.data.BitsRepository
import com.bits.app.data.BitsState
import com.bits.app.data.Category
import com.bits.app.data.Item
import com.bits.app.data.WidgetThemes
import com.bits.app.time.MidnightScheduler

private val Muted = Color(0xFF8A95A3)

/** Must match the clock height in the in-app preview. */
private val ClockHeight = 88.dp

private sealed class WidgetLine {
    class Header(val category: Category, val first: Boolean) : WidgetLine()
    class Entry(val item: Item) : WidgetLine()
    object Hint : WidgetLine()
}

class BitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BitsRepository.get(context)
        repository.load()
        MidnightScheduler.schedule(context)

        provideContent {
            val state by repository.state.collectAsState()
            val current = state
            if (current != null) {
                WidgetBody(context, current)
            }
        }
    }
}

@Composable
private fun WidgetBody(context: Context, state: BitsState) {
    val theme = WidgetThemes.find(state.preferences.widgetThemeId)
    val ink = Color(0xFFEAE6DA)
    val done = Color(theme.doneColor)
    val accent = Color(theme.accent)
    val background = Color(theme.backgroundTint)

    val categories = state.widgetCategories
    val lines = buildList<WidgetLine> {
        categories.forEachIndexed { index, category ->
            add(WidgetLine.Header(category, index == 0))
            state.itemsIn(category.id).forEach { add(WidgetLine.Entry(it)) }
        }
        if (categories.isEmpty()) add(WidgetLine.Hint)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(background.copy(alpha = state.widget.opacity))
            .cornerRadius(22.dp)
            .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 4.dp)
    ) {
        if (state.widget.showClock) {
            // Fixed height: the clock stays pinned and the list below keeps the remaining space.
            AndroidRemoteViews(
                remoteViews = RemoteViews(context.packageName, R.layout.widget_clock),
                modifier = GlanceModifier.fillMaxWidth().height(ClockHeight),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
        }

        LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            items(lines) { line ->
                when (line) {
                    is WidgetLine.Header -> HeaderLine(context, line.category, line.first, accent)
                    is WidgetLine.Entry -> EntryLine(context, line.item, ink, done)
                    is WidgetLine.Hint -> Text(
                        text = "Nothing to show. In the app, tap Edit and switch on a category.",
                        style = TextStyle(color = ColorProvider(Muted), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    )
                }
            }
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bottom-left: quick access to the mini-games section.
            Image(
                provider = ImageProvider(R.drawable.ic_game_controller),
                contentDescription = "Play a quick game",
                modifier = GlanceModifier
                    .size(36.dp)
                    .padding(9.dp)
                    .clickable(actionStartActivity(Launch.games(context))),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "Bits",
                style = TextStyle(color = ColorProvider(Muted), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .padding(vertical = 8.dp)
                    .clickable(actionStartActivity(Launch.app(context))),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_tune),
                contentDescription = "Bits settings",
                modifier = GlanceModifier
                    .size(36.dp)
                    .padding(9.dp)
                    .clickable(actionStartActivity(Launch.settings(context))),
            )
        }
    }
}

@Composable
private fun HeaderLine(context: Context, category: Category, first: Boolean, accent: Color) {
    Text(
        text = category.name.uppercase(),
        style = TextStyle(color = ColorProvider(accent), fontSize = 13.sp, fontWeight = FontWeight.Bold),
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = if (first) 0.dp else 15.dp, bottom = 5.dp)
            .clickable(actionStartActivity(Launch.category(context, category.id))),
    )
}

@Composable
private fun EntryLine(context: Context, item: Item, ink: Color, done: Color) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Checkbox: toggles done/undone in place.
        Image(
            provider = ImageProvider(if (item.done) R.drawable.ic_check_on else R.drawable.ic_check_off),
            contentDescription = if (item.done) "Mark not done" else "Mark done",
            modifier = GlanceModifier
                .size(17.dp)
                .clickable(
                    actionRunCallback<ToggleItemAction>(
                        actionParametersOf(ToggleItemAction.ItemIdKey to item.id)
                    )
                ),
        )
        Spacer(modifier = GlanceModifier.width(9.dp))
        // Text: opens the small floating editor instead of toggling completion.
        Text(
            text = item.text,
            style = TextStyle(
                color = ColorProvider(if (item.done) done else ink),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            ),
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(Launch.quickEdit(context, item.id))),
        )
    }
}
