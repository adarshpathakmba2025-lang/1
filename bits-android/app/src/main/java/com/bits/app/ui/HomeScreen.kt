package com.bits.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bits.app.R
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import com.bits.app.data.canShiftItem
import com.bits.app.data.shiftItemCategory
import com.bits.app.data.BitsRepository
import com.bits.app.data.BitsState
import com.bits.app.data.Category
import com.bits.app.data.Item
import com.bits.app.data.TODAY_ID
import com.bits.app.data.TOMORROW_ID
import com.bits.app.data.addCategory
import com.bits.app.data.addItem
import com.bits.app.data.deleteCategory
import com.bits.app.data.editItem
import com.bits.app.data.isSystemCategory
import com.bits.app.data.renameCategory
import com.bits.app.data.reorderCategories
import com.bits.app.data.reorderItems
import com.bits.app.data.setShownOnWidget
import com.bits.app.data.toggleItem
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: BitsState,
    repository: BitsRepository,
    selectedCategoryId: String,
    onSelectCategory: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGames: () -> Unit,
    onTitleTap: () -> Unit,
    onHiddenCategory: () -> Unit,
    tutorialActive: Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var managing by rememberSaveable { mutableStateOf(false) }
    var quickAddCategoryId by remember { mutableStateOf<String?>(null) }
    val categories = state.sortedCategories
    val selected = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
    val selectedIndex = categories.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)

    // Leaving Edit with the back button counts as Done rather than closing the app.
    BackHandler(enabled = managing) { managing = false }
    // Backing out of search clears it first.
    BackHandler(enabled = !managing && query.isNotBlank()) { query = "" }

    // The tour always starts from a clean home page.
    LaunchedEffect(tutorialActive) {
        if (tutorialActive) {
            query = ""
            managing = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Bits",
                style = BitsText.Brand,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTitleTap,
                    )
                    .padding(vertical = 4.dp),
            )
            Box(
                modifier = Modifier
                    .tutorialTarget(TutorialTarget.GAMES)
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenGames),
                contentAlignment = Alignment.Center,
            ) {
                // No tint here: the icon's two tones are baked into the drawable itself.
                Image(
                    painter = painterResource(R.drawable.ic_game_controller),
                    contentDescription = "Games",
                    modifier = Modifier.size(30.dp),
                )
            }
            Box(
                modifier = Modifier
                    .tutorialTarget(TutorialTarget.SETTINGS)
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = BitsColors.Muted, modifier = Modifier.size(24.dp))
            }
        }

        SearchField(value = query, onValueChange = { query = it })

        Spacer(Modifier.height(10.dp))

        CategoryChips(
            categories = categories,
            selectedId = selected.id,
            managing = managing,
            onSelect = { id ->
                managing = false
                query = ""
                onSelectCategory(id)
            },
            onToggleManage = {
                managing = !managing
                query = ""
            },
        )

        Spacer(Modifier.height(10.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BitsColors.Panel)
        ) {
            val addTarget = quickAddCategoryId
            if (addTarget != null) {
                val targetName = categories.firstOrNull { it.id == addTarget }?.name.orEmpty()
                QuickAddSheet(
                    categoryName = targetName,
                    onDismiss = { quickAddCategoryId = null },
                    onAdd = { text ->
                        repository.edit { it.addItem(addTarget, text) }
                        quickAddCategoryId = null
                    },
                )
            }
            when {
                query.isNotBlank() -> SearchResults(
                    state = state,
                    query = query,
                    onToggle = { id -> repository.edit { it.toggleItem(id) } },
                    onOpenCategory = { id ->
                        query = ""
                        managing = false
                        onSelectCategory(id)
                    },
                )

                managing -> ManageCategories(
                    state = state,
                    repository = repository,
                    onHiddenCategory = onHiddenCategory,
                )

                else -> key(selected.id) {
                    CategoryPanel(
                        category = selected,
                        items = state.itemsIn(selected.id),
                        repository = repository,
                        state = state,
                        onTitleTapped = { quickAddCategoryId = selected.id },
                        // Swiping sideways moves to the next or previous category.
                        onSwipeToPrevious = {
                            if (selectedIndex > 0) onSelectCategory(categories[selectedIndex - 1].id)
                        },
                        onSwipeToNext = {
                            if (selectedIndex < categories.lastIndex) onSelectCategory(categories[selectedIndex + 1].id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .tutorialTarget(TutorialTarget.SEARCH)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x800A1017))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = BitsColors.Muted, modifier = Modifier.size(18.dp))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp)) {
            if (value.isEmpty()) {
                Text("Search everything", style = BitsText.Body.copy(color = BitsColors.Muted))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = BitsText.Body,
                cursorBrush = SolidColor(BitsColors.Amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = BitsColors.Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedId: String,
    managing: Boolean,
    onSelect: (String) -> Unit,
    onToggleManage: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .weight(1f)
                .tutorialTarget(TutorialTarget.CHIPS),
        ) {
            items(categories, key = { it.id }) { category ->
                Chip(
                    text = category.name,
                    active = !managing && category.id == selectedId,
                    subtle = false,
                    onClick = { onSelect(category.id) },
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        // Edit sits outside the scrolling row so it's always reachable, however many categories there are.
        Chip(
            text = if (managing) "Done" else "Edit",
            active = managing,
            subtle = true,
            onClick = onToggleManage,
            modifier = Modifier.tutorialTarget(TutorialTarget.EDIT),
        )
    }
}

/**
 * A category chip in the arcade style: square corners and a solid offset shadow,
 * matching the games section rather than the rounded pill it used to be.
 */
@Composable
private fun Chip(text: String, active: Boolean, subtle: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val fill = when {
        active -> BitsColors.Amber
        subtle -> Color(0xFF1B2735)
        else -> BitsColors.PanelBase
    }
    val ink = when {
        active -> BitsColors.Bg
        subtle -> BitsColors.Muted
        else -> BitsColors.Ink
    }
    Box(modifier.padding(bottom = 3.dp, end = 3.dp)) {
        Box(
            Modifier
                .padding(start = 3.dp, top = 3.dp)
                .matchParentSize()
                .background(Color(0x73000000))
        )
        Text(
            text = text.uppercase(),
            style = BitsText.ChipLabel.copy(color = ink),
            maxLines = 1,
            modifier = Modifier
                .background(if (active) BitsColors.Amber else Color(0xFF33445A))
                .padding(1.5.dp)
                .background(fill)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

private fun subtitleFor(categoryId: String): String? = when (categoryId) {
    TODAY_ID -> LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    TOMORROW_ID -> "Unfinished items move to Today at midnight."
    else -> null
}

@Composable
private fun CategoryPanel(
    category: Category,
    items: List<Item>,
    repository: BitsRepository,
    state: BitsState,
    onSwipeToPrevious: () -> Unit,
    onSwipeToNext: () -> Unit,
    onTitleTapped: () -> Unit,
) {
    val listState = rememberLazyListState()
    var localItems by remember { mutableStateOf(items) }
    var dragging by remember { mutableStateOf(false) }
    var lastCount by remember { mutableIntStateOf(items.size) }

    // Follow saved data, except mid-drag when the local order is ahead of it.
    LaunchedEffect(items) {
        if (!dragging) localItems = items
    }

    // After adding an item, bring it into view.
    LaunchedEffect(localItems.size) {
        if (localItems.size > lastCount && localItems.isNotEmpty()) {
            listState.animateScrollToItem(localItems.lastIndex)
        }
        lastCount = localItems.size
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = localItems.indexOfFirst { it.id == from.key }
        val toIndex = localItems.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            localItems = localItems.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(category.id) {
                var totalX = 0f
                var totalY = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalX = 0f; totalY = 0f },
                    onHorizontalDrag = { change, amount ->
                        totalX += amount
                        totalY += change.positionChange().y
                        change.consume()
                    },
                    onDragEnd = {
                        // Ignore mostly-vertical drags, which belong to the list's own scrolling.
                        if (abs(totalX) > 80f && abs(totalX) > abs(totalY)) {
                            if (totalX > 0) onSwipeToPrevious() else onSwipeToNext()
                        }
                    },
                )
            }
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp)) {
            // Tapping the title adds straight into this category, as it does on the widget.
            Text(
                text = category.name,
                style = BitsText.Title,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onTitleTapped() }
                    .padding(vertical = 2.dp),
            )
            subtitleFor(category.id)?.let {
                Text(it, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
            }
        }

        if (localItems.isEmpty()) {
            Text(
                text = "Nothing here yet. Type below and tap Add.",
                style = BitsText.Body.copy(color = BitsColors.Muted),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 6.dp, end = 2.dp, bottom = 8.dp),
            ) {
                itemsIndexed(localItems, key = { _, item -> item.id }) { index, item ->
                    ReorderableItem(reorderState, key = item.id) { isDragging ->
                        ItemRow(
                            item = item,
                            isDragging = isDragging,
                            isFirst = index == 0,
                            canShiftBack = state.canShiftItem(item.id, forward = false),
                            canShiftForward = state.canShiftItem(item.id, forward = true),
                            onShift = { forward -> repository.edit { s -> s.shiftItemCategory(item.id, forward) } },
                            handleModifier = Modifier.draggableHandle(
                                onDragStarted = { dragging = true },
                                onDragStopped = {
                                    dragging = false
                                    val orderedIds = localItems.map { entry -> entry.id }
                                    repository.edit { s -> s.reorderItems(orderedIds) }
                                },
                            ),
                            onToggle = { repository.edit { s -> s.toggleItem(item.id) } },
                            onEdit = { text -> repository.edit { s -> s.editItem(item.id, text) } },
                            onDelete = { repository.deleteItemWithUndo(item.id) },
                        )
                    }
                }
            }
        }

        var draft by rememberSaveable { mutableStateOf("") }
        InputPill(
            value = draft,
            onValueChange = { draft = it },
            placeholder = "Add to ${category.name}",
            onSubmit = {
                val text = draft.trim()
                if (text.isNotEmpty()) {
                    repository.edit { s -> s.addItem(category.id, text) }
                    draft = ""
                }
            },
            modifier = Modifier.tutorialTarget(TutorialTarget.CAPTURE),
        )
    }
}

@Composable
private fun ItemRow(
    item: Item,
    isDragging: Boolean,
    isFirst: Boolean,
    handleModifier: Modifier,
    canShiftBack: Boolean,
    canShiftForward: Boolean,
    onShift: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFirst) Modifier.tutorialTarget(TutorialTarget.ITEM) else Modifier)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDragging) BitsColors.DragHighlight else Color.Transparent),
        verticalAlignment = Alignment.Top,
    ) {
        BitsCheckbox(
            checked = item.done,
            onToggle = onToggle,
            label = if (item.done) "Reopen ${item.text}" else "Complete ${item.text}",
        )

        if (editing) {
            EditField(
                initial = item.text,
                canShiftBack = canShiftBack,
                canShiftForward = canShiftForward,
                onShift = onShift,
                onSave = { text ->
                    editing = false
                    val trimmed = text.trim()
                    if (trimmed.isEmpty()) onDelete() else if (trimmed != item.text) onEdit(trimmed)
                },
                onDelete = {
                    editing = false
                    onDelete()
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = item.text,
                style = if (item.done) BitsText.BodyDone else BitsText.Body,
                modifier = Modifier
                    .weight(1f)
                    .clickable { editing = true }
                    .padding(top = 9.dp, bottom = 9.dp, end = 4.dp),
            )
        }

        Box(
            modifier = handleModifier
                .then(if (isFirst) Modifier.tutorialTarget(TutorialTarget.HANDLE) else Modifier)
                .size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_drag),
                contentDescription = "Reorder ${item.text}",
                tint = BitsColors.Muted.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Tap an item to edit it. Clearing the text deletes it, like erasing a line on paper. */
@Composable
private fun EditField(
    initial: String,
    canShiftBack: Boolean,
    canShiftForward: Boolean,
    onShift: (Boolean) -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val save: () -> Unit = {
        if (!finished) {
            finished = true
            onSave(value.text)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier.padding(top = 9.dp, bottom = 2.dp)) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            textStyle = BitsText.Body,
            cursorBrush = SolidColor(BitsColors.Amber),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focus ->
                    if (focus.isFocused) hadFocus = true else if (hadFocus) save()
                },
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(BitsColors.Amber)
        )
        // Arrows on the left move the task between categories; actions stay on the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShiftArrow("\u2039", enabled = canShiftBack) {
                finished = true
                save()
                onShift(false)
            }
            ShiftArrow("\u203A", enabled = canShiftForward) {
                finished = true
                save()
                onShift(true)
            }
            Spacer(Modifier.weight(1f))
            TextAction("Save", BitsColors.Ink, save)
            // Two taps to delete, so a slip never loses a task.
            ArmedDelete {
                finished = true
                onDelete()
            }
        }
    }
}

@Composable
private fun ManageCategories(state: BitsState, repository: BitsRepository, onHiddenCategory: () -> Unit) {
    val categories = state.sortedCategories
    val listState = rememberLazyListState()
    var local by remember { mutableStateOf(categories) }
    var dragging by remember { mutableStateOf(false) }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(categories) {
        if (!dragging) local = categories
    }

    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = local.indexOfFirst { it.id == from.key }
        val toIndex = local.indexOfFirst { it.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            local = local.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Categories",
            style = BitsText.Title,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp),
        )
        Text(
            text = "Tap a name to dim it and hide it from your widget. Drag to reorder. Today and Tomorrow are permanent.",
            style = BitsText.Small,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        ) {
            items(local, key = { it.id }) { category ->
                ReorderableItem(reorderState, key = category.id) { isDragging ->
                    val count = state.items.count { it.categoryId == category.id }
                    val shown = state.isShownOnWidget(category.id)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDragging) BitsColors.DragHighlight else Color.Transparent)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (renamingId == category.id) {
                                RenameField(
                                    initial = category.name,
                                    onCommit = { name ->
                                        renamingId = null
                                        if (name.isNotBlank() && name != category.name) {
                                            repository.edit { s -> s.renameCategory(category.id, name) }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                                )
                            } else {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(
                                            onClickLabel = if (shown) "Hide from widget" else "Show on widget",
                                        ) {
                                            repository.edit { s -> s.setShownOnWidget(category.id, !shown) }
                                            if (shown) onHiddenCategory()
                                        }
                                        .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = category.name,
                                        style = if (shown) BitsText.Body
                                        else BitsText.Body.copy(color = BitsColors.Muted.copy(alpha = 0.5f)),
                                    )
                                }
                                if (isSystemCategory(category.id)) {
                                    // Invisible spacer that keeps rows lined up with the ones that have Rename/Delete.
                                    Row(Modifier.alpha(0f).clearAndSetSemantics {}) {
                                        ActionLabel("Rename")
                                        ActionLabel("Delete")
                                    }
                                } else {
                                    TextAction("Rename") {
                                        confirmDeleteId = null
                                        renamingId = category.id
                                    }
                                    TextAction("Delete", BitsColors.Danger) {
                                        confirmDeleteId = category.id
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .draggableHandle(
                                        onDragStarted = { dragging = true },
                                        onDragStopped = {
                                            dragging = false
                                            val orderedIds = local.map { entry -> entry.id }
                                            repository.edit { s -> s.reorderCategories(orderedIds) }
                                        },
                                    )
                                    .size(width = 36.dp, height = 44.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_drag),
                                    contentDescription = "Reorder ${category.name}",
                                    tint = BitsColors.Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        if (confirmDeleteId == category.id) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1FE8907F))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                val noun = if (count == 1) "item" else "items"
                                Text(
                                    text = "Delete \u201C${category.name}\u201D and its $count $noun? This can't be undone.",
                                    style = BitsText.Small.copy(color = BitsColors.Ink),
                                )
                                Row {
                                    TextAction("Delete category", BitsColors.Danger) {
                                        confirmDeleteId = null
                                        repository.edit { s -> s.deleteCategory(category.id) }
                                    }
                                    TextAction("Keep it") { confirmDeleteId = null }
                                }
                            }
                        }
                    }
                }
            }
        }

        var newName by rememberSaveable { mutableStateOf("") }
        InputPill(
            value = newName,
            onValueChange = { newName = it },
            placeholder = "New category",
            onSubmit = {
                val name = newName.trim()
                if (name.isNotEmpty()) {
                    repository.edit { s -> s.addCategory(name) }
                    newName = ""
                }
            },
        )
    }
}

/** One of the move-between-categories arrows. Dimmed and inert at the ends of the list. */
@Composable
private fun ShiftArrow(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = glyph,
        style = BitsText.Subtitle.copy(
            color = if (enabled) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.3f),
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** A small dialog for adding straight into a category, matching the widget's card. */
@Composable
private fun QuickAddSheet(categoryName: String, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(BitsColors.PanelBase)
                .padding(20.dp)
        ) {
            Text("Add to $categoryName", style = BitsText.Small.copy(color = BitsColors.Amber))
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                textStyle = BitsText.Body,
                cursorBrush = SolidColor(BitsColors.Amber),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val t = value.trim()
                    if (t.isNotEmpty()) onAdd(t) else onDismiss()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .focusRequester(focusRequester)
                    .padding(top = 12.dp),
            )
            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(1.dp).background(BitsColors.Amber))
            Row(Modifier.padding(top = 4.dp)) {
                Spacer(Modifier.weight(1f))
                TextAction("Cancel", BitsColors.Muted, onDismiss)
                TextAction("Add", BitsColors.Ink) {
                    val t = value.trim()
                    if (t.isNotEmpty()) onAdd(t) else onDismiss()
                }
            }
        }
    }
}

/** Same size as TextAction, not clickable. */
@Composable
private fun ActionLabel(text: String) {
    Text(
        text = text,
        style = BitsText.Small,
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
    )
}

@Composable
private fun RenameField(initial: String, onCommit: (String) -> Unit, modifier: Modifier = Modifier) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val commit: () -> Unit = {
        if (!finished) {
            finished = true
            onCommit(value.text.trim())
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        textStyle = BitsText.Body,
        cursorBrush = SolidColor(BitsColors.Amber),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focus ->
                if (focus.isFocused) hadFocus = true else if (hadFocus) commit()
            }
            .padding(vertical = 10.dp),
    )
}

private fun highlighted(text: String, query: String): AnnotatedString {
    val lower = text.lowercase()
    val index = if (query.isEmpty() || lower.length != text.length) -1 else lower.indexOf(query)
    return buildAnnotatedString {
        if (index < 0) {
            append(text)
        } else {
            append(text.substring(0, index))
            withStyle(SpanStyle(background = BitsColors.Amber.copy(alpha = 0.3f))) {
                append(text.substring(index, index + query.length))
            }
            append(text.substring(index + query.length))
        }
    }
}

@Composable
private fun SearchResults(
    state: BitsState,
    query: String,
    onToggle: (String) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    val q = query.trim().lowercase()
    val categories = state.sortedCategories
    val categoryHits = categories.filter { it.name.lowercase().contains(q) }
    val groups = categories.mapNotNull { category ->
        val hits = state.itemsIn(category.id).filter { it.text.lowercase().contains(q) }
        if (hits.isEmpty()) null else category to hits
    }
    val itemCount = groups.sumOf { it.second.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
    ) {
        if (categoryHits.isEmpty() && groups.isEmpty()) {
            item {
                Text(
                    text = "Nothing matches \u201C${query.trim()}\u201D. Try a shorter word or check the spelling.",
                    style = BitsText.Body.copy(color = BitsColors.Muted),
                    modifier = Modifier.padding(8.dp),
                )
            }
            return@LazyColumn
        }

        item {
            val summary = buildString {
                append(itemCount)
                append(if (itemCount == 1) " item" else " items")
                if (categoryHits.isNotEmpty()) {
                    append(" and ")
                    append(categoryHits.size)
                    append(if (categoryHits.size == 1) " category" else " categories")
                }
            }
            Text(summary, style = BitsText.Small, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }

        if (categoryHits.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categoryHits.forEach { category ->
                        Text(
                            text = highlighted(category.name, q),
                            style = BitsText.Body,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BitsColors.PanelBase)
                                .clickable { onOpenCategory(category.id) }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }

        groups.forEach { (category, hits) ->
            item(key = "group_${category.id}") {
                Text(
                    text = category.name,
                    style = BitsText.Subtitle,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenCategory(category.id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
            items(hits, key = { it.id }) { hit ->
                Row(verticalAlignment = Alignment.Top) {
                    BitsCheckbox(
                        checked = hit.done,
                        onToggle = { onToggle(hit.id) },
                        label = if (hit.done) "Reopen ${hit.text}" else "Complete ${hit.text}",
                    )
                    Text(
                        text = highlighted(hit.text, q),
                        style = if (hit.done) BitsText.BodyDone else BitsText.Body,
                        modifier = Modifier.weight(1f).padding(top = 9.dp, bottom = 9.dp, end = 8.dp),
                    )
                }
            }
        }
    }
}
