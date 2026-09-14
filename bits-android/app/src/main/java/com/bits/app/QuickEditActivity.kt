package com.bits.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bits.app.data.BitsState
import com.bits.app.data.WidgetThemes
import androidx.compose.ui.graphics.Color
import com.bits.app.data.BitsRepository
import com.bits.app.data.addItem
import com.bits.app.data.deleteItem
import com.bits.app.data.editItem
import com.bits.app.data.toggleItem
import com.bits.app.ui.ArmedDelete
import com.bits.app.ui.BitsCheckbox
import com.bits.app.ui.TextAction
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import com.bits.app.ui.theme.BitsTheme

/**
 * A small floating card for editing one item, or adding one to a category,
 * straight from a widget tap.
 *
 * Android widgets can't host a live keyboard or text field themselves — a tap on a widget
 * can only launch a PendingIntent, never draw an editable view inside the widget's own pixels.
 * This activity is the usual workaround: a transparent-themed Activity that pops up over
 * whatever's on screen, feels instant, and never shows the full app.
 */
class QuickEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra(Launch.EXTRA_ITEM_ID)
        val categoryId = intent.getStringExtra(Launch.EXTRA_CATEGORY)
        val appWidgetId = intent.getIntExtra(Launch.EXTRA_WIDGET_ID, -1)
        val repository = BitsRepository.get(this)

        setContent {
            BitsTheme {
                if (categoryId != null) {
                    QuickAddCard(categoryId, appWidgetId, repository) { finish() }
                } else {
                    QuickEditCard(itemId, appWidgetId, repository) { finish() }
                }
            }
        }
    }
}

/** The colours this card should wear, taken from whatever theme the widget is using. */
private data class CardSkin(
    val surface: Color,
    val ink: Color,
    val accent: Color,
    val muted: Color,
)

/**
 * The skin comes from the settings of the widget that launched this card, so two widgets
 * with different themes never show each other's colours. It reads from live state, so a
 * theme change applies the moment it is made.
 */
@Composable
private fun skinFor(state: BitsState?, appWidgetId: Int): CardSkin {
    val theme = when {
        state == null -> WidgetThemes.Classic
        appWidgetId >= 0 -> state.themeFor(state.settingsFor(appWidgetId))
        else -> state.activeTheme
    }
    val ink = Color(theme.ink)
    return CardSkin(
        // Lifted a little off the widget's own tint so the card reads as a layer above it.
        surface = Color(theme.backgroundTint),
        ink = ink,
        accent = Color(theme.accent),
        muted = ink.copy(alpha = 0.55f),
    )
}

@Composable
private fun CardShell(skin: CardSkin, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(BitsColors.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .windowInsetsPadding(WindowInsets.ime),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(skin.surface)
                // Swallows taps so they don't dismiss the card.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun EditorField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    skin: CardSkin,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    // Keep the caret in view as a long bit keeps growing.
    LaunchedEffect(value.text) { scroll.animateScrollTo(scroll.maxValue) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = BitsText.Body.copy(color = skin.ink),
        cursorBrush = SolidColor(skin.accent),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.focusRequester(focusRequester),
    )
}

@Composable
private fun QuickAddCard(categoryId: String, appWidgetId: Int, repository: BitsRepository, onDone: () -> Unit) {
    val state by repository.state.collectAsState()
    val skin = skinFor(state, appWidgetId)
    LaunchedEffect(Unit) { repository.load() }

    val category = state?.categories?.firstOrNull { it.id == categoryId }
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var saved by remember { mutableStateOf(false) }

    val save: () -> Unit = {
        val text = value.text.trim()
        if (!saved && text.isNotEmpty()) {
            saved = true
            repository.edit { it.addItem(categoryId, text) }
        }
        onDone()
    }

    // Backing out, or tapping away, keeps whatever was typed rather than throwing it away.
    val latestSave by rememberUpdatedState(save)

    CardShell(skin = skin, onDismiss = { latestSave() }) {
        Text(
            text = "Add to ${category?.name ?: "list"}",
            style = BitsText.Small.copy(color = skin.accent),
        )
        val addScroll = rememberScrollState()
        EditorField(
            value = value,
            onValueChange = { value = it },
            onSubmit = save,
            skin = skin,
            scroll = addScroll,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(addScroll)
                .padding(top = 10.dp),
        )
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(1.dp).background(skin.accent))
        Row(Modifier.padding(top = 4.dp)) {
            Spacer(Modifier.weight(1f))
            TextAction("Cancel", skin.muted) {
                saved = true
                onDone()
            }
            TextAction("Add", skin.ink) { save() }
        }
    }
}

@Composable
private fun QuickEditCard(itemId: String?, appWidgetId: Int, repository: BitsRepository, onDone: () -> Unit) {
    val state by repository.state.collectAsState()
    val skin = skinFor(state, appWidgetId)
    LaunchedEffect(Unit) { repository.load() }

    val current = state
    val item = itemId?.let { id -> current?.items?.firstOrNull { it.id == id } }

    if (current == null) {
        CardShell(skin = skin, onDismiss = onDone) {
            Text("Loading\u2026", style = BitsText.Body.copy(color = skin.muted))
        }
        return
    }

    // The item can vanish while the card is open, e.g. after a delete. Just close
    // quietly rather than showing a dead-end message.
    if (item == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }

    var value by remember(item.id) { mutableStateOf(TextFieldValue(item.text, TextRange(item.text.length))) }
    var settled by remember(item.id) { mutableStateOf(false) }
    val save: () -> Unit = {
        if (!settled) {
            settled = true
            val text = value.text.trim()
            if (text.isEmpty()) repository.deleteItemWithUndo(item.id)
            else if (text != item.text) repository.edit { it.editItem(item.id, text) }
        }
        onDone()
    }
    val latestSave by rememberUpdatedState(save)

    CardShell(skin = skin, onDismiss = { latestSave() }) {
        Row(verticalAlignment = Alignment.Top) {
            BitsCheckbox(
                checked = item.done,
                onToggle = { repository.edit { it.toggleItem(item.id) } },
                label = if (item.done) "Reopen ${item.text}" else "Complete ${item.text}",
            )
            // Capped and scrollable, so a very long note can't push the buttons
            // off the bottom of the screen.
            val editScroll = rememberScrollState()
            EditorField(
                value = value,
                onValueChange = { value = it },
                onSubmit = save,
                skin = skin,
                scroll = editScroll,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 220.dp)
                    .verticalScroll(editScroll)
                    .padding(top = 9.dp),
            )
        }
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(1.dp).background(skin.accent))
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextAction("Save", skin.ink) { save() }
            ArmedDelete {
                settled = true
                repository.deleteItemWithUndo(item.id)
                onDone()
            }
        }
    }
}
