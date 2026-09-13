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
    /** Empty means "use whatever the app-wide theme is". Only Pro boards set this. */
    val themeIdOverride: String = "",
    val clockStyleOverride: String = "",
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
    /**
     * The single reward set unlocked by the tap easter egg: exactly one theme, one game
     * and one clock style, chosen once. Empty strings mean nothing claimed yet.
     */
    val bonusThemeId: String,
    val bonusGameId: String,
    val bonusClockId: String,
    /** Set once the easter egg has been claimed on this device, so it can never repeat. */
    val easterEggUsed: Boolean,
    /** Set once the user has been walked through placing the widget on their home screen. */
    val onboardingDone: Boolean,
    val highScores: Map<String, Int>,
    /** The daily Word Guess puzzle: which day it was, the guesses made, and the streak. */
    val wordleDay: Long,
    val wordleGuesses: List<String>,
    val wordleStreak: Int,
    /** Fewest flips ever used to clear a Memory round. 0 means nothing recorded yet. */
    val memoryBestTries: Int,
    /** Spendable hint points, earned one per solved daily puzzle. */
    val hintPoints: Int,
    /** Positions bought with hint points for today's puzzle. */
    val wordleRevealed: Set<Int>,
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
            bonusGameId = "",
            bonusClockId = "",
            easterEggUsed = false,
            onboardingDone = false,
            highScores = emptyMap(),
            wordleDay = 0L,
            wordleGuesses = emptyList(),
            wordleStreak = 0,
            memoryBestTries = 0,
            hintPoints = 0,
            wordleRevealed = emptySet(),
        )
    }
}

data class BitsState(
    val categories: List<Category>,
    val items: List<Item>,
    val lastRollover: String,
    /** The shared settings every widget uses unless it has its own board. */
    val widget: WidgetSettings,
    /**
     * Per-widget settings, keyed by Android's appWidgetId. Only Pro users create these.
     * A widget with no entry here simply falls back to [widget], so free users see
     * every placed widget stay identical, exactly as before.
     */
    val boards: Map<Int, WidgetSettings>,
    val preferences: Preferences,
) {
    val sortedCategories: List<Category>
        get() = categories.sortedBy { it.order }

    /** Categories that appear on the shared widget, in the user's order. */
    val widgetCategories: List<Category>
        get() = categoriesFor(widget)

    fun categoriesFor(settings: WidgetSettings): List<Category> =
        sortedCategories.filter { it.id !in settings.hiddenCategoryIds }

    /** Settings for one placed widget: its own board if it has one, otherwise the shared config. */
    fun settingsFor(appWidgetId: Int): WidgetSettings =
        if (preferences.isPro) boards[appWidgetId] ?: widget else widget

    fun hasOwnBoard(appWidgetId: Int): Boolean = preferences.isPro && boards.containsKey(appWidgetId)

    /** The theme a given widget draws with, honouring a board override when it's allowed. */
    fun themeFor(settings: WidgetSettings): WidgetTheme {
        val id = settings.themeIdOverride.ifEmpty { preferences.widgetThemeId }
        return if (canUseTheme(id)) WidgetThemes.find(id) else WidgetThemes.Classic
    }

    fun clockStyleFor(settings: WidgetSettings): ClockStyle {
        val id = settings.clockStyleOverride.ifEmpty { preferences.clockStyleId }
        return if (canUseClockStyle(id)) ClockStyles.find(id) else ClockStyles.all.first()
    }

    fun itemsIn(categoryId: String): List<Item> =
        items.filter { it.categoryId == categoryId }.sortedBy { it.position }

    fun isShownOnWidget(categoryId: String): Boolean = categoryId !in widget.hiddenCategoryIds

    /** Free, bought with Pro, or claimed through the easter egg. */
    fun canUseTheme(themeId: String): Boolean {
        val theme = WidgetThemes.find(themeId)
        return theme.free || preferences.isPro || preferences.bonusThemeId == themeId
    }

    fun canUseClockStyle(styleId: String): Boolean =
        ClockStyles.find(styleId).free || preferences.isPro || preferences.bonusClockId == styleId

    /** Games are identified by the keys in the UI's GameId list. */
    fun canPlayGame(gameId: String, free: Boolean): Boolean =
        free || preferences.isPro || preferences.bonusGameId == gameId

    /** The theme actually drawn, falling back to Classic if a Pro theme is no longer available. */
    val activeTheme: WidgetTheme
        get() = if (canUseTheme(preferences.widgetThemeId)) WidgetThemes.find(preferences.widgetThemeId)
        else WidgetThemes.Classic

    val activeClockStyle: ClockStyle
        get() = if (canUseClockStyle(preferences.clockStyleId)) ClockStyles.find(preferences.clockStyleId)
        else ClockStyles.all.first()

    fun highScore(gameId: String): Int = preferences.highScores[gameId] ?: 0

    /** True once the whole easter-egg reward has been taken. */
    val easterEggClaimed: Boolean get() = preferences.easterEggUsed
}
