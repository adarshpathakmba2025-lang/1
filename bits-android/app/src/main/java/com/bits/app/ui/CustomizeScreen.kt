package com.bits.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bits.app.data.BitsRepository
import com.bits.app.data.BitsState
import com.bits.app.data.Category
import com.bits.app.data.addCategory
import com.bits.app.data.deleteCategory
import com.bits.app.data.isSystemCategory
import com.bits.app.data.renameCategory
import com.bits.app.data.reorderCategories
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

/**
 * Everything about how the widgets look and what they carry, in one place.
 *
 * This is what the home page's Edit button and the widget's Customise icon both open.
 * It hosts the same widget-list controls that used to live in Settings, plus category
 * renaming and reordering, which Settings never offered.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomizeScreen(
    state: BitsState,
    repository: BitsRepository,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
) {
    var clockPreview by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

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

            CategoriesSection(
                state = state,
                repository = repository,
                renaming = renaming,
                onStartRename = { renaming = it },
                onStopRename = { renaming = null },
                confirmDelete = confirmDelete,
                onAskDelete = { confirmDelete = it },
                onCancelDelete = { confirmDelete = null },
                onConfirmDelete = { id ->
                    repository.edit { it.deleteCategory(id) }
                    confirmDelete = null
                },
            )

            Spacer(Modifier.height(22.dp))

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

/**
 * The category list: drag to reorder, tap a name to rename, and add or remove lists.
 * Today and Tomorrow are fixed, so they can't be renamed, moved or deleted.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoriesSection(
    state: BitsState,
    repository: BitsRepository,
    renaming: String?,
    onStartRename: (String) -> Unit,
    onStopRename: () -> Unit,
    confirmDelete: String?,
    onAskDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    SectionLabel("Your lists")
    Text(
        text = "Drag to reorder, tap a name to rename. Today and Tomorrow stay put.",
        style = BitsText.Small,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
    )

    SettingsCard {
        val ordered = state.sortedCategories
        ordered.forEach { category ->
            CategoryRow(
                category = category,
                isRenaming = renaming == category.id,
                canMoveUp = ordered.indexOf(category) > 0 && !isSystemCategory(category.id),
                canMoveDown = ordered.indexOf(category) < ordered.lastIndex && !isSystemCategory(category.id),
                onStartRename = { onStartRename(category.id) },
                onRename = { newName ->
                    repository.edit { it.renameCategory(category.id, newName) }
                    onStopRename()
                },
                onCancelRename = onStopRename,
                onMove = { up ->
                    val ids = ordered.map { it.id }.toMutableList()
                    val from = ids.indexOf(category.id)
                    val to = if (up) from - 1 else from + 1
                    if (to in ids.indices) {
                        ids.add(to, ids.removeAt(from))
                        repository.edit { it.reorderCategories(ids) }
                    }
                },
                confirming = confirmDelete == category.id,
                onDelete = { onAskDelete(category.id) },
                onCancelDelete = onCancelDelete,
                onConfirmDelete = { onConfirmDelete(category.id) },
            )
        }

        Divider()
        var newCategory by remember { mutableStateOf("") }
        InputPill(
            value = newCategory,
            onValueChange = { newCategory = it },
            placeholder = "New list",
            onSubmit = {
                val name = newCategory.trim()
                if (name.isNotEmpty()) {
                    repository.edit { it.addCategory(name) }
                    newCategory = ""
                }
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isRenaming: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onStartRename: () -> Unit,
    onRename: (String) -> Unit,
    onCancelRename: () -> Unit,
    onMove: (Boolean) -> Unit,
    confirming: Boolean,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val system = isSystemCategory(category.id)

    // Deleting a list takes its bits with it, so it asks first, inline.
    if (confirming) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                "Delete \u201C${category.name}\u201D? Everything in it goes too.",
                style = BitsText.Small.copy(color = BitsColors.Ink),
            )
            Row(Modifier.padding(top = 4.dp)) {
                Spacer(Modifier.weight(1f))
                TextAction("Cancel", BitsColors.Muted, onCancelDelete)
                TextAction("Delete", BitsColors.Danger, onConfirmDelete)
            }
        }
        return
    }

    if (isRenaming) {
        var draft by remember(category.id) { mutableStateOf(category.name) }
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            InputPill(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "List name",
                onSubmit = { onRename(draft) },
            )
            Row(Modifier.padding(top = 4.dp)) {
                Spacer(Modifier.weight(1f))
                TextAction("Cancel", BitsColors.Muted, onCancelRename)
                TextAction("Save", BitsColors.Ink) { onRename(draft) }
            }
        }
        return
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = category.name,
            style = if (system) BitsText.Body.copy(color = BitsColors.Muted) else BitsText.Body,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .then(if (system) Modifier else Modifier.clickable(onClick = onStartRename))
                .padding(vertical = 9.dp),
        )

        if (system) {
            // Today and Tomorrow are part of how Bits works, so they are fixed.
            Text("Fixed", style = BitsText.Small.copy(color = BitsColors.Muted.copy(alpha = 0.7f)))
        } else {
            MoveArrow("\u25B2", canMoveUp) { onMove(true) }
            MoveArrow("\u25BC", canMoveDown) { onMove(false) }
            TextAction("Delete", BitsColors.Danger, onDelete)
        }
    }
}

@Composable
private fun MoveArrow(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = glyph,
        style = BitsText.Small.copy(
            color = if (enabled) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.3f),
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}
