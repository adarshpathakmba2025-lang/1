package com.bits.app.data

/**
 * A named color set for the widget. Colors are stored as plain 0xAARRGGBB ints here
 * so this file has no Android/Compose dependency; the UI layer converts them to Color.
 *
 * "Classic" is the app's default look and always free; the rest are Pro.
 * Each theme only touches the widget's accent and background tint, never the app's own screens.
 */
data class WidgetTheme(
    val id: String,
    val displayName: String,
    val accent: Long,
    val backgroundTint: Long,
    val doneColor: Long,
    val free: Boolean,
)

object WidgetThemes {
    val Classic = WidgetTheme(
        id = "classic",
        displayName = "Classic",
        accent = 0xFFF2B544,
        backgroundTint = 0xFF0C131B,
        doneColor = 0xFF667281,
        free = true,
    )
    val Midnight = WidgetTheme(
        id = "midnight",
        displayName = "Midnight",
        accent = 0xFF8FB3FF,
        backgroundTint = 0xFF0B0F1F,
        doneColor = 0xFF5B6485,
        free = false,
    )
    val Forest = WidgetTheme(
        id = "forest",
        displayName = "Forest",
        accent = 0xFF7FD68A,
        backgroundTint = 0xFF0A1512,
        doneColor = 0xFF5C7A64,
        free = false,
    )
    val Ember = WidgetTheme(
        id = "ember",
        displayName = "Ember",
        accent = 0xFFFF8A5B,
        backgroundTint = 0xFF1A0E0A,
        doneColor = 0xFF8A6255,
        free = false,
    )
    val Ocean = WidgetTheme(
        id = "ocean",
        displayName = "Ocean",
        accent = 0xFF5BD3D3,
        backgroundTint = 0xFF071A1D,
        doneColor = 0xFF547D80,
        free = false,
    )
    val Royal = WidgetTheme(
        id = "royal",
        displayName = "Royal",
        accent = 0xFFC79BFF,
        backgroundTint = 0xFF150E24,
        doneColor = 0xFF7A6C93,
        free = false,
    )

    val all = listOf(Classic, Midnight, Forest, Ember, Ocean, Royal)

    fun find(id: String): WidgetTheme = all.firstOrNull { it.id == id } ?: Classic
}
