package com.bits.app.data

/**
 * A named color set for the widget. Colors are plain 0xAARRGGBB longs so this file
 * stays free of any Android/Compose dependency; the UI layer converts them to Color.
 *
 * Each theme is a small palette rather than one accent on near-black: a tinted background,
 * an accent for headings, a separate ink for item text, and a muted tone for finished items.
 * "Classic" is the default and always free.
 */
data class WidgetTheme(
    val id: String,
    val displayName: String,
    val blurb: String,
    val accent: Long,
    val backgroundTint: Long,
    val ink: Long,
    val doneColor: Long,
    val free: Boolean,
)

object WidgetThemes {
    val Classic = WidgetTheme(
        id = "classic",
        displayName = "Amber Classic",
        blurb = "Warm amber on slate",
        accent = 0xFFF2B544,
        backgroundTint = 0xFF0C131B,
        ink = 0xFFEAE6DA,
        doneColor = 0xFF667281,
        free = true,
    )
    val Midnight = WidgetTheme(
        id = "midnight",
        displayName = "Blue Hour",
        blurb = "Cool periwinkle on deep navy",
        accent = 0xFF9DBBFF,
        backgroundTint = 0xFF0B1022,
        ink = 0xFFE4E9F7,
        doneColor = 0xFF5F6A8C,
        free = false,
    )
    val Forest = WidgetTheme(
        id = "forest",
        displayName = "Moss & Cream",
        blurb = "Soft green on forest floor",
        accent = 0xFF9BD9A0,
        backgroundTint = 0xFF0C1712,
        ink = 0xFFE8EFE2,
        doneColor = 0xFF5F7D66,
        free = false,
    )
    val Ember = WidgetTheme(
        id = "ember",
        displayName = "Ember Glow",
        blurb = "Warm coral on cocoa",
        accent = 0xFFFF9E6B,
        backgroundTint = 0xFF1B100C,
        ink = 0xFFF5E7DE,
        doneColor = 0xFF8E6A5B,
        free = false,
    )
    val Ocean = WidgetTheme(
        id = "ocean",
        displayName = "Tide Pool",
        blurb = "Aqua on deep teal",
        accent = 0xFF6FDCD2,
        backgroundTint = 0xFF07191C,
        ink = 0xFFE0F1F0,
        doneColor = 0xFF57807F,
        free = false,
    )
    val Royal = WidgetTheme(
        id = "royal",
        displayName = "Orchid Night",
        blurb = "Lilac on aubergine",
        accent = 0xFFCFA6FF,
        backgroundTint = 0xFF160F26,
        ink = 0xFFEDE6F7,
        doneColor = 0xFF7E6E99,
        free = false,
    )
    val Sand = WidgetTheme(
        id = "sand",
        displayName = "Paper & Ink",
        blurb = "Muted olive on warm stone",
        accent = 0xFFC9B688,
        backgroundTint = 0xFF181712,
        ink = 0xFFEFEADD,
        doneColor = 0xFF7D7663,
        free = false,
    )
    val Rose = WidgetTheme(
        id = "rose",
        displayName = "Dusty Rose",
        blurb = "Blush on plum",
        accent = 0xFFF3A6B8,
        backgroundTint = 0xFF1A1015,
        ink = 0xFFF6E6EA,
        doneColor = 0xFF8E6874,
        free = false,
    )

    val all = listOf(Classic, Midnight, Forest, Ember, Ocean, Royal, Sand, Rose)

    fun find(id: String): WidgetTheme = all.firstOrNull { it.id == id } ?: Classic
}

/** How the clock at the top of the widget is drawn. */
data class ClockStyle(
    val id: String,
    val displayName: String,
    val blurb: String,
    val free: Boolean,
) {
    companion object {
        const val MINIMAL = "minimal"
        const val STACKED = "stacked"
        const val COMPACT = "compact"
        const val MONO = "mono"
        const val BOLD = "bold"
        const val DOTTED = "dotted"
    }
}

object ClockStyles {
    val all = listOf(
        ClockStyle(ClockStyle.MINIMAL, "Minimal", "Big time, date underneath", free = true),
        ClockStyle(ClockStyle.COMPACT, "Compact", "One line, saves room for your lists", free = true),
        ClockStyle(ClockStyle.STACKED, "Stacked", "Weekday above, time below", free = false),
        ClockStyle(ClockStyle.MONO, "Monospace", "Even digits, terminal feel", free = false),
        ClockStyle(ClockStyle.BOLD, "Statement", "Oversized and left-aligned", free = false),
        ClockStyle(ClockStyle.DOTTED, "Seconds", "Minimal, with ticking seconds", free = false),
    )

    fun find(id: String): ClockStyle = all.firstOrNull { it.id == id } ?: all.first()
}
