package com.bits.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.bits.app.R

object BitsColors {
    val Bg = Color(0xFF16202B)
    val PanelBase = Color(0xFF1F2C3B)
    val Panel = Color(0xC71F2C3B)
    val Ink = Color(0xFFEAE6DA)
    val Muted = Color(0xFF8A95A3)
    val Done = Color(0xFF667281)
    val Amber = Color(0xFFF2B544)
    val Danger = Color(0xFFE8907F)
    val Track = Color(0x33EAE6DA)
    val DragHighlight = Color(0x14EAE6DA)
    val WidgetInk = Color(0xFF0C131B)
    val HomeWall = Color(0xFF22364A)
    val Scrim = Color(0xD90A1017)
}

object BitsFonts {
    val Display = FontFamily(
        Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
        Font(R.font.chakra_petch_bold, FontWeight.Bold),
    )
    val Body = FontFamily(
        Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
        Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
    )
    /** Used only in the games section, for its arcade feel. */
    val Pixel = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
}

object BitsText {
    val Brand = TextStyle(fontFamily = BitsFonts.Display, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = BitsColors.Ink)
    val Title = TextStyle(fontFamily = BitsFonts.Display, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 34.sp, color = BitsColors.Ink)
    val Subtitle = TextStyle(fontFamily = BitsFonts.Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, color = BitsColors.Ink)
    val Body = TextStyle(fontFamily = BitsFonts.Body, fontSize = 16.sp, lineHeight = 22.sp, color = BitsColors.Ink)
    val BodyBold = Body.copy(fontWeight = FontWeight.Bold)
    val BodyDone = Body.copy(color = BitsColors.Done, textDecoration = TextDecoration.LineThrough)
    val Small = TextStyle(fontFamily = BitsFonts.Body, fontSize = 14.sp, lineHeight = 19.sp, color = BitsColors.Muted)

    // The widget preview uses the system font at the same sizes and weights as the real widget.
    val WidgetClock = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 50.sp, color = BitsColors.Ink)
    val WidgetDate = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp, color = BitsColors.Ink.copy(alpha = 0.75f))
    val WidgetHeading = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, color = BitsColors.Amber)
    val WidgetItem = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 19.sp, color = BitsColors.Ink)
    val WidgetItemDone = WidgetItem.copy(color = BitsColors.Done, textDecoration = TextDecoration.LineThrough)

    val WidgetClockCompact = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 30.sp, color = BitsColors.Ink)
    val WidgetClockMono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 38.sp, lineHeight = 44.sp, color = BitsColors.Ink)
    val WidgetDateMono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp, color = BitsColors.Ink.copy(alpha = 0.75f))
    val WidgetClockBold = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 56.sp, color = BitsColors.Ink)
    val WidgetClockSeconds = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 42.sp, color = BitsColors.Ink)

    // Games section. Press Start 2P is wide and tall, so sizes run small and lines run loose.
    val PixelTitle = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 17.sp, lineHeight = 26.sp, color = BitsColors.Ink)
    val PixelHeading = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 12.sp, lineHeight = 20.sp, color = BitsColors.Ink)
    /** Big serif-free letters for the word grid, matching the reference layout. */
    /** Emoji faces in Memory Match, sized to be readable without squinting. */
    val MemoryGlyph = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, lineHeight = 40.sp, color = BitsColors.Ink)
    val WordCell = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 34.sp, color = BitsColors.Ink)
    /** Small enough that DEL and GO fit their keys without shrinking the letter keys. */
    val KeyCap = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 9.sp, lineHeight = 14.sp, color = BitsColors.Ink)
    /** Category chips on the home page, in the arcade face. */
    /** The reset glyph beside a best score: deliberately chunky. */
    val PixelReset = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 20.sp, lineHeight = 24.sp, color = BitsColors.Amber)
    val ChipLabel = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 9.sp, lineHeight = 14.sp, color = BitsColors.Ink)
    val PixelCaption = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 7.sp, lineHeight = 13.sp, color = BitsColors.Muted)
    val PixelBody = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 9.sp, lineHeight = 17.sp, color = BitsColors.Muted)
    val PixelScore = TextStyle(fontFamily = BitsFonts.Pixel, fontSize = 11.sp, lineHeight = 18.sp, color = BitsColors.Amber)
}

@Composable
fun BitsTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = BitsColors.Amber,
        onPrimary = BitsColors.Bg,
        secondary = BitsColors.Amber,
        background = BitsColors.Bg,
        onBackground = BitsColors.Ink,
        surface = BitsColors.PanelBase,
        onSurface = BitsColors.Ink,
        error = BitsColors.Danger,
    )
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            LocalTextStyle provides BitsText.Body,
            LocalContentColor provides BitsColors.Ink,
            content = content,
        )
    }
}
