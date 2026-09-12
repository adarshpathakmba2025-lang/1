package com.bits.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.remember
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlin.math.roundToInt

/** The checkbox with a comfortable 40dp touch target. */
@Composable
fun BitsCheckbox(checked: Boolean, onToggle: () -> Unit, label: String) {
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 40.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        // Sits against the first line of text, so multi-line items stay tidy.
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.padding(top = 9.dp)) {
            CheckVisual(checked = checked, size = 19.dp)
        }
    }
}

/**
 * A squared-off box rather than a rounded one: slightly wider than tall, with a small
 * corner radius, which reads a little more deliberate than a rounded tick-box.
 */
@Composable
fun CheckVisual(checked: Boolean, size: Dp) {
    val shape = RoundedCornerShape(size * 0.12f)
    val base = Modifier.size(width = size * 1.08f, height = size).clip(shape)
    Box(
        modifier = if (checked) base.background(BitsColors.Done) else base.border(1.5.dp, BitsColors.Muted, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = BitsColors.Bg,
                modifier = Modifier.size(size * 0.7f),
            )
        }
    }
}

/** Compact switch that fits in a category row. */
@Composable
fun MiniSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    val trackShape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 44.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 20.dp)
                .clip(trackShape)
                .background(if (checked) BitsColors.Amber else Color.Transparent)
                .border(1.5.dp, if (checked) BitsColors.Amber else BitsColors.Muted, trackShape)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (checked) BitsColors.Bg else BitsColors.Muted)
            )
        }
    }
}

@Composable
fun BitsSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = BitsColors.Bg,
            checkedTrackColor = BitsColors.Amber,
            checkedBorderColor = BitsColors.Amber,
            uncheckedThumbColor = BitsColors.Muted,
            uncheckedTrackColor = Color.Transparent,
            uncheckedBorderColor = BitsColors.Muted,
        ),
    )
}

@Composable
fun TextAction(text: String, color: Color = BitsColors.Muted, onClick: () -> Unit) {
    Text(
        text = text,
        style = BitsText.Small.copy(color = color),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 10.dp),
    )
}

@Composable
fun FilledAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = text,
        style = BitsText.BodyBold.copy(color = BitsColors.Amber),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BitsColors.Amber.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun OpacitySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = BitsText.Body, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", style = BitsText.Body.copy(color = BitsColors.Muted))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = BitsColors.Amber,
                activeTrackColor = BitsColors.Amber,
                inactiveTrackColor = BitsColors.Track,
            ),
        )
    }
}

/**
 * Single-line input. Press Enter, tap the plus, or tap Add.
 * The keyboard stays open so the next entry can be typed straight away.
 */
@Composable
fun InputPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasText = value.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x66000000)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    // Empty field: just bring the keyboard up. Field already has text: same as tapping Add.
                    if (hasText) onSubmit() else {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add", tint = BitsColors.Amber, modifier = Modifier.size(22.dp))
        }
        Box(Modifier.weight(1f).padding(vertical = 13.dp)) {
            if (value.isEmpty()) {
                Text(placeholder, style = BitsText.Body.copy(color = BitsColors.Muted))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = BitsText.Body,
                cursorBrush = SolidColor(BitsColors.Amber),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        Text(
            text = "Add",
            style = BitsText.BodyBold.copy(color = if (hasText) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (hasText) BitsColors.Amber.copy(alpha = 0.16f) else Color.Transparent)
                .clickable(onClick = onSubmit)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** The faint dot grid behind everything. */
@Composable
fun DotGrid(modifier: Modifier, spacing: Dp = 22.dp, color: Color = Color(0x0FEAE6DA)) {
    Canvas(modifier) {
        val step = spacing.toPx()
        val radius = 1.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color = color, radius = radius, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}
