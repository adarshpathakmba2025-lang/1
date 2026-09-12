package com.bits.app.ui

import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.animateColorAsState   
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.delay

/** How long a primed Delete stays live before going back to sleep. */
const val DELETE_ARM_MILLIS = 3000L

/**
 * A Delete that needs two taps. It sits dim and inert; the first tap arms it and turns
 * it bright red for three seconds; a second tap inside that window actually deletes.
 * Miss the window and it quietly disarms, so a stray tap can never remove anything.
 */
@Composable
fun ArmedDelete(
    label: String = "Delete",
    armedLabel: String = "Tap again",
    onConfirmed: () -> Unit,
) {
    var armed by remember { mutableStateOf(false) }

    LaunchedEffect(armed) {
        if (armed) {
            delay(DELETE_ARM_MILLIS)
            armed = false
        }
    }

    val textColor by animateColorAsState(
        targetValue = if (armed) BitsColors.Danger else BitsColors.Muted.copy(alpha = 0.45f),
        animationSpec = tween(160),
        label = "armColor",
    )
    val background by animateColorAsState(
        targetValue = if (armed) BitsColors.Danger.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(160),
        label = "armBg",
    )

    Text(
        text = if (armed) armedLabel else label,
        style = BitsText.Small.copy(color = textColor),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable {
                if (armed) {
                    armed = false
                    onConfirmed()
                } else {
                    armed = true
                }
            }
            .padding(horizontal = 9.dp, vertical = 10.dp),
    )
}
