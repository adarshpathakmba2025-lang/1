package com.bits.app.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import com.bits.app.data.ClockStyle
import com.bits.app.data.Item
import com.bits.app.time.MidnightScheduler

private val Muted = Color(0xFF8A95A3)
private val FooterIcon = 34.dp
private val FooterInset = 6.dp

private sealed class WidgetLine {
    class Header(val category: Category, val first: Boolean) : WidgetLine()
    class Entry(val item: Item) : WidgetLine()
    object Hint : WidgetLine()
}

/** Layout and height for each clock style. Heights match the in-app preview. */
private fun clockLayout(styleId: String): Pair<Int, Int> = when (styleId) {
    ClockStyle.COMPACT -> R.layout.widget_clock_compact to 40
    ClockStyle.STACKED -> R.layout.widget_clock_stacked to 84
    ClockStyle.MONO -> R.layout.widget_clock_mono to 78
    ClockStyle.BOLD -> R.layout.widget_clock_bold to 88
    ClockStyle.DOTTED -> R.layout.widget_clock_seconds to 80
    else -> R.layout.widget_clock to 88
}

class BitsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BitsRepository.get(context)
        repository.load()
        MidnightScheduler.schedule(context)
        // Which widget this is, so Pro users can give each one its own list.
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(-1)

        provideContent {
            val state by repository.state.collectAsState()
            val current = state
            if (current != null) {
                WidgetBody(context, current, appWidgetId)
            }
        }
    }
}

@Composable
private fun WidgetBody(context: Context, state: BitsState, appWidgetId: Int) {
    val settings = state.settingsFor(appWidgetId)
    val theme = state.themeFor(settings)
    val ink = Color(theme.ink)
    val done = Color(theme.doneColor)
    val accent = Color(theme.accent)
    val background = Color(theme.backgroundTint)

    val categories = state.categoriesFor(settings)
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
            .background(background.copy(alpha = settings.opacity))
            .cornerRadius(22.dp)
            // Extra end padding leaves a clear channel for the scrollbar, so long
            // item text is never drawn underneath it.
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 4.dp)
    ) {
        if (settings.showClock) {
            val (layout, heightDp) = clockLayout(state.clockStyleFor(settings).id)
            AndroidRemoteViews(
                remoteViews = RemoteViews(context.packageName, layout),
                modifier = GlanceModifier.fillMaxWidth().height(heightDp.dp).padding(end = 10.dp),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
        }

        LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
            items(lines) { line ->
                when (line) {
                    is WidgetLine.Header -> HeaderLine(
                        context = context,
                        category = line.category,
                        first = line.first,
                        accent = accent,
                        appWidgetId = appWidgetId,
                        pixel = settings.pixelHeadings && state.canUsePixelHeadings,
                    )
                    is WidgetLine.Entry -> EntryLine(context, line.item, ink, done, appWidgetId)
                    is WidgetLine.Hint -> Text(
                        text = "Nothing to show. In the app, tap Edit and tap a category name to bring it back.",
                        style = TextStyle(color = ColorProvider(Muted), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        modifier = GlanceModifier.padding(end = 14.dp),
                    )
                }
            }
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bottom-left: quick access to the mini-games section.
            // All three icons share one size and one inset, so the row reads evenly.
            Image(
                provider = ImageProvider(R.drawable.ic_game_controller),
                contentDescription = "Play a quick game",
                modifier = GlanceModifier
                    .size(FooterIcon)
                    .padding(FooterInset)
                    .clickable(actionStartActivity(Launch.games(context))),
            )
            // Reorder sits beside the games button, so the top of the widget stays
            // entirely given over to the lists.
            Image(
                provider = ImageProvider(R.drawable.ic_reorder_pixel),
                contentDescription = "Reorder your bits",
                modifier = GlanceModifier
                    .size(FooterIcon)
                    .padding(FooterInset)
                    .clickable(actionStartActivity(Launch.reorder(context, appWidgetId))),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "Bits",
                style = TextStyle(color = ColorProvider(Muted), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .padding(vertical = 10.dp, horizontal = 8.dp)
                    .clickable(actionStartActivity(Launch.app(context))),
            )
            // Customise, not settings: it opens the page that actually governs how this
            // widget looks and which lists it carries.
            Image(
                provider = ImageProvider(R.drawable.ic_customize_pixel),
                contentDescription = "Customise this widget",
                modifier = GlanceModifier
                    .size(FooterIcon)
                    .padding(FooterInset)
                    .clickable(actionStartActivity(Launch.customize(context))),
            )
        }
    }
}

@Composable
private fun HeaderLine(
    context: Context,
    category: Category,
    first: Boolean,
    accent: Color,
    appWidgetId: Int,
    pixel: Boolean,
) {
    // Tapping a heading opens the floating "add to this list" card rather than the whole app.
    val tap = GlanceModifier
        .fillMaxWidth()
        .padding(top = if (first) 0.dp else 15.dp, bottom = 5.dp, end = 14.dp)
        .clickable(actionStartActivity(Launch.quickAdd(context, category.id, appWidgetId)))

    // The pixel face is drawn into a bitmap rather than asked for by name, because the
    // launcher renders the widget and is free to ignore an app font. If the drawing
    // fails for any reason the ordinary heading still appears.
    val pixelBitmap = if (pixel) PixelHeading.render(context, category.name.uppercase(), accent.toArgb()) else null

    if (pixelBitmap != null) {
        val views = RemoteViews(context.packageName, R.layout.widget_heading_pixel).apply {
            setImageViewBitmap(R.id.pixel_heading, pixelBitmap)
            setContentDescription(R.id.pixel_heading, category.name)
        }
        AndroidRemoteViews(remoteViews = views, modifier = tap)
    } else {
        Text(
            text = category.name.uppercase(),
            style = TextStyle(color = ColorProvider(accent), fontSize = 16.sp, fontWeight = FontWeight.Bold),
            modifier = tap,
        )
    }
}

@Composable
private fun EntryLine(context: Context, item: Item, ink: Color, done: Color, appWidgetId: Int) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Checkbox: toggles done/undone in place.
        // The tap area is a box wider and taller than the drawn checkbox, reaching into
        // the gap that used to be a plain Spacer. The checkbox itself is unmoved and
        // unchanged; only the area that responds to a tap grows, so it is far harder
        // to miss.
        Box(
            modifier = GlanceModifier
                .size(width = 31.dp, height = 26.dp)
                .clickable(
                    actionRunCallback<ToggleItemAction>(
                        actionParametersOf(ToggleItemAction.ItemIdKey to item.id)
                    )
                ),
            contentAlignment = Alignment.TopStart,
        ) {
            Image(
                provider = ImageProvider(if (item.done) R.drawable.ic_check_on else R.drawable.ic_check_off),
                contentDescription = if (item.done) "Mark not done" else "Mark done",
                modifier = GlanceModifier
                    // Nudged down so the box sits on the text baseline rather than above it.
                    .padding(top = 3.dp)
                    .size(width = 22.dp, height = 17.dp),
            )
        }
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
                .padding(end = 14.dp)
                .clickable(actionStartActivity(Launch.quickEdit(context, item.id, appWidgetId))),
        )
    }
}
