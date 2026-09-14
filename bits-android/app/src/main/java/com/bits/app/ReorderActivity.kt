package com.bits.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.bits.app.data.BitsRepository
import com.bits.app.data.BitsState
import com.bits.app.data.WidgetRow
import com.bits.app.data.WidgetThemes
import com.bits.app.data.applyWidgetRows
import com.bits.app.data.widgetRows
import com.bits.app.ui.CheckVisual
import com.bits.app.ui.TextAction
import com.bits.app.ui.theme.BitsText
import com.bits.app.ui.theme.BitsTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Drag-to-reorder for one widget's list.
 *
 * A home screen widget is drawn from RemoteViews inside the launcher's process, so the app
 * can never attach touch listeners to it and real dragging is impossible there. This sheet
 * is the workaround: it opens instantly over the home screen, shows that widget's exact
 * list, and supports genuine dragging, including across category headings.
 */
class ReorderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(Launch.EXTRA_WIDGET_ID, -1)
        val repository = BitsRepository.get(this)

        setContent {
            BitsTheme {
                ReorderSheet(appWidgetId, repository) { finish() }
            }
        }
    }
}

@Composable
private fun ReorderSheet(appWidgetId: Int, repository: BitsRepository, onDone: () -> Unit) {
    val state by repository.state.collectAsState()
    LaunchedEffect(Unit) { repository.load() }

    val current = state
    // Colours come from this widget's own settings, so two widgets never swap looks.
    val theme = when {
        current == null -> WidgetThemes.Classic
        appWidgetId >= 0 -> current.themeFor(current.settingsFor(appWidgetId))
        else -> current.activeTheme
    }
    val ink = Color(theme.ink)
    val accent = Color(theme.accent)
    val surface = Color(theme.backgroundTint)
    val done = Color(theme.doneColor)
    val muted = ink.copy(alpha = 0.55f)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        if (current == null) return@Box
        val settings = current.settingsFor(appWidgetId)

        // Local copy so dragging feels immediate; saved when the drag ends.
        var rows by remember(current.items, current.categories) {
            mutableStateOf(current.widgetRows(settings))
        }
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            val fromIndex = rows.indexOfFirst { rowKey(it) == from.key }
            val toIndex = rows.indexOfFirst { rowKey(it) == to.key }
            // Index 0 is always the first heading. A bit dropped above it would belong to
            // no category at all, so that slot is refused and the drag simply stops there.
            if (fromIndex >= 0 && toIndex >= 1) {
                rows = rows.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            }
        }

        Column(
            Modifier
                // Covers most of the widget area without filling the whole screen.
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(22.dp))
                .background(surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("REORDER", style = BitsText.WidgetHeading.copy(color = accent), modifier = Modifier.weight(1f))
                TextAction("Done", accent) {
                    repository.edit { it.applyWidgetRows(rows) }
                    onDone()
                }
            }
            Text(
                "Drag a bit anywhere, even past a heading to move it there.",
                style = BitsText.WidgetItem.copy(color = muted),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp, end = 8.dp),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(rows, key = { rowKey(it) }) { row ->
                    ReorderableItem(reorderState, key = rowKey(row)) { dragging ->
                        when (row) {
                            is WidgetRow.Header -> {
                                val name = current.categories.firstOrNull { it.id == row.categoryId }?.name.orEmpty()
                                Text(
                                    text = name.uppercase(),
                                    style = BitsText.WidgetHeading.copy(color = accent),
                                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                                )
                            }
                            is WidgetRow.Entry -> {
                                val item = current.items.firstOrNull { it.id == row.itemId }
                                if (item != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (dragging) ink.copy(alpha = 0.12f) else Color.Transparent)
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CheckVisual(checked = item.done, size = 16.dp)
                                        Spacer(Modifier.width(9.dp))
                                        Text(
                                            text = item.text,
                                            style = if (item.done) {
                                                BitsText.WidgetItem.copy(color = done, textDecoration = TextDecoration.LineThrough)
                                            } else {
                                                BitsText.WidgetItem.copy(color = ink)
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                        Box(
                                            modifier = Modifier
                                                .draggableHandle()
                                                .size(width = 40.dp, height = 36.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_drag),
                                                contentDescription = "Drag ${item.text}",
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A stable key per row, so dragging never confuses a heading with a bit. */
private fun rowKey(row: WidgetRow): String = when (row) {
    is WidgetRow.Header -> "h_${row.categoryId}"
    is WidgetRow.Entry -> "e_${row.itemId}"
}
