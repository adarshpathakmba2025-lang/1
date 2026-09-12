package com.bits.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bits.app.data.BitsRepository
import com.bits.app.data.deleteItem
import com.bits.app.data.editItem
import com.bits.app.data.toggleItem
import com.bits.app.ui.BitsCheckbox
import com.bits.app.ui.TextAction
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import com.bits.app.ui.theme.BitsTheme

/**
 * A small floating card for editing one item straight from a widget tap.
 *
 * Android widgets can't host a live keyboard or text field themselves — a tap on a widget
 * can only launch a PendingIntent, never draw an editable view inside the widget's own pixels.
 * This activity is the workaround every "edit from widget" app uses: a transparent-themed
 * Activity that pops up over whatever's on screen, feels instant, and never shows the full app.
 */
class QuickEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getStringExtra(Launch.EXTRA_ITEM_ID)
        val repository = BitsRepository.get(this)

        setContent {
            BitsTheme {
                QuickEditScreen(
                    itemId = itemId,
                    repository = repository,
                    onDone = { finish() },
                )
            }
        }
    }
}

@Composable
private fun QuickEditScreen(itemId: String?, repository: BitsRepository, onDone: () -> Unit) {
    val state by repository.state.collectAsState()
    val item = itemId?.let { id -> state?.items?.firstOrNull { it.id == id } }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { repository.load() }

    Box(
        Modifier
            .fillMaxSize()
            .background(BitsColors.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .windowInsetsPadding(WindowInsets.ime),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (state == null) return@Box

        if (item == null) {
            Column(
                Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BitsColors.PanelBase)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(20.dp)
            ) {
                Text("This item isn't there anymore.", style = BitsText.Body)
                Row { TextAction("Close", BitsColors.Amber, onDone) }
            }
            return@Box
        }

        var value by remember(item.id) { mutableStateOf(TextFieldValue(item.text, TextRange(item.text.length))) }
        val focusRequester = remember { FocusRequester() }
        var done by remember(item.id) { mutableStateOf(item.done) }

        LaunchedEffect(item.id) {
            focusRequester.requestFocus()
            keyboard?.show()
        }

        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BitsColors.PanelBase)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                BitsCheckbox(
                    checked = done,
                    onToggle = {
                        done = !done
                        repository.edit { it.toggleItem(item.id) }
                    },
                    label = if (done) "Reopen ${item.text}" else "Complete ${item.text}",
                )
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = BitsText.Body,
                    cursorBrush = SolidColor(BitsColors.Amber),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .padding(top = 9.dp),
                )
            }
            Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(1.dp).background(BitsColors.Amber))
            Row {
                TextAction("Delete", BitsColors.Danger) {
                    repository.edit { it.deleteItem(item.id) }
                    onDone()
                }
                TextAction("Cancel", BitsColors.Muted, onDone)
                TextAction("Save", BitsColors.Ink) {
                    val text = value.text.trim()
                    if (text.isEmpty()) {
                        repository.edit { it.deleteItem(item.id) }
                    } else if (text != item.text) {
                        repository.edit { it.editItem(item.id, text) }
                    }
                    onDone()
                }
            }
        }
    }
}
