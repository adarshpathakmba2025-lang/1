package com.bits.app.data

const val TODAY_ID = "cat_today"
const val TOMORROW_ID = "cat_tomorrow"
const val GROCERY_ID = "cat_grocery"

fun isSystemCategory(id: String): Boolean = id == TODAY_ID || id == TOMORROW_ID

/** A flat bucket. No nesting, ever. */
data class Category(
    val id: String,
    val name: String,
    val order: Int,
)

/** Everything is an item. Meaning comes from the user, not from fields. */
data class Item(
    val id: String,
    val text: String,
    val done: Boolean,
    val categoryId: String,
    val createdAt: Long,
    val position: Int,
)

/** One set of settings shared by the home screen widget. */
data class WidgetSettings(
    val opacity: Float,
    val showClock: Boolean,
    /** Categories switched off in Edit. New categories are shown by default. */
    val hiddenCategoryIds: Set<String>,
) {
    companion object {
        val Default = WidgetSettings(opacity = 0.72f, showClock = true, hiddenCategoryIds = emptySet())
    }
}

data class Preferences(
    val autoClearCompleted: Boolean,
    val tutorialSeen: Boolean,
    /**
     * Whether Pro perks (extra games, widget themes, clock styles) are unlocked.
     * This will be set by a verified Play Billing purchase once billing is wired up.
     * Until then it can only be flipped by the "Simulate Pro" developer switch in Settings.
     */
    val isPro: Boolean,
    val widgetThemeId: String,
    val clockStyleId: String,
    /** New items go to the top of their category unless this is on. */
    val addToBottom: Boolean,
    /** Shown once, the first time a category is hidden from the widget. */
    val hideHintSeen: Boolean,
    /** The one theme unlocked by the tap easter egg. Empty until it's claimed. */
    val bonusThemeId: String,
    /** Set once the easter egg has been triggered on this device, so it can't repeat. */
    val easterEggUsed: Boolean,
    val highScores: Map<String, Int>,
) {
    companion object {
        val Default = Preferences(
            autoClearCompleted = false,
            tutorialSeen = false,
            isPro = false,
            widgetThemeId = WidgetThemes.Classic.id,
            clockStyleId = ClockStyle.MINIMAL,
            addToBottom = false,
            hideHintSeen = false,
            bonusThemeId = "",
            easterEggUsed = false,
            highScores = emptyMap(),
        )
    }
}

data class BitsState(
    val categories: List<Category>,
    val items: List<Item>,
    val lastRollover: String,
    val widget: WidgetSettings,
    val preferences: Preferences,
) {
    val sortedCategories: List<Category>
        get() = categories.sortedBy { it.order }

    /** Categories that appear on the widget, in the user's order. */
    val widgetCategories: List<Category>
        get() = sortedCategories.filter { it.id !in widget.hiddenCategoryIds }

    fun itemsIn(categoryId: String): List<Item> =
        items.filter { it.categoryId == categoryId }.sortedBy { it.position }

    fun isShownOnWidget(categoryId: String): Boolean = categoryId !in widget.hiddenCategoryIds

    /** Free, bought with Pro, or claimed through the easter egg. */
    fun canUseTheme(themeId: String): Boolean {
        val theme = WidgetThemes.find(themeId)
        return theme.free || preferences.isPro || preferences.bonusThemeId == themeId
    }

    fun canUseClockStyle(styleId: String): Boolean =
        ClockStyles.find(styleId).free || preferences.isPro

    /** The theme actually drawn, falling back to Classic if a Pro theme is no longer available. */
    val activeTheme: WidgetTheme
        get() = if (canUseTheme(preferences.widgetThemeId)) WidgetThemes.find(preferences.widgetThemeId)
        else WidgetThemes.Classic

    val activeClockStyle: ClockStyle
        get() = if (canUseClockStyle(preferences.clockStyleId)) ClockStyles.find(preferences.clockStyleId)
        else ClockStyles.all.first()

    fun highScore(gameId: String): Int = preferences.highScores[gameId] ?: 0
}
