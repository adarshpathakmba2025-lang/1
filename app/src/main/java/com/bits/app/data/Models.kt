package com.bits.app.data

const val TODAY_ID = "cat_today"
const val TOMORROW_ID = "cat_tomorrow"
const val GROCERY_ID = "cat_grocery"

fun isSystemCategory(id: String): Boolean = id == TODAY_ID || id == TOMORROW_ID

/** The plans Pro can be granted under. */
object ProPlan {
    const val NONE = ""
    const val LIFETIME = "lifetime"
    const val MONTHLY = "monthly"
}

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
    /**
     * Draws this widget's category headings in the pixel face. Pro, or unlocked through
     * the easter egg. Per widget like the other looks, so two widgets can differ.
     * Defaulted, so every existing construction site keeps working untouched.
     */
    val pixelHeadings: Boolean = false,
) {
    companion object {
        val Default = WidgetSettings(opacity = 0.72f, showClock = true, hiddenCategoryIds = emptySet())
    }
}

data class Preferences(
    val autoClearCompleted: Boolean,
    val tutorialSeen: Boolean,
    /**
     * Whether this account has bought Pro, as told to us by Play Billing.
     *
     * Note this is ownership, not access: while [Monetization.ENABLED] is false nobody
     * owns anything and everybody has everything. Read [BitsState.proUnlocked] to decide
     * whether a perk is available - never this flag on its own.
     */
    val isPro: Boolean,
    /**
     * Which plan granted Pro, so a monthly subscriber can be offered a way to cancel
     * while a lifetime buyer isn't shown a cancel option that would mean nothing for them.
     * Empty when not Pro. Set by Play Billing once wired up; for now, by the developer
     * switch, so the flow can be tested before real billing exists.
     */
    val proPlan: String,
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
    val bonusThemeIds: Set<String>,
    val bonusGameIds: Set<String>,
    val bonusClockIds: Set<String>,
    /**
     * How many reward sets this device has been granted, and how many have been taken.
     * Everyone starts with one; the thirty-day thank-you grants a second. A claim is only
     * ever allowed while taken < allowance, which caps the total no matter what.
     */
    val easterEggAllowance: Int,
    val easterEggClaims: Int,
    /** The pixel-heading look, if it came from the easter egg rather than Pro. */
    val bonusPixelHeadings: Boolean,
    /** Set once the user has been walked through placing the widget on their home screen. */
    val onboardingDone: Boolean,
    /** When the app first ran, for the thirty-day thank-you. Zero until first recorded. */
    val installedAt: Long,
    /** Set once the thirty-day thank-you has been given, so it happens exactly once. */
    val anniversaryGiven: Boolean,
    val highScores: Map<String, Int>,
    /** The daily Word Guess puzzle: which day it was, the guesses made, and the streak. */
    val wordleDay: Long,
    val wordleGuesses: List<String>,
    val wordleStreak: Int,
    /** Every submission this day, including words rejected as not in the list. */
    val wordleAttempts: Int,
    /** Fewest flips ever used to clear a Memory round. 0 means nothing recorded yet. */
    val memoryBestTries: Int,
    /** Spendable hint points, earned one per solved daily puzzle. */
    val hintPoints: Int,
    /** Positions bought with hint points for today's puzzle. */
    val wordleRevealed: Set<Int>,
    /**
     * True for anyone who installed Bits while it was free.
     *
     * Written from the very first release even though nothing charges yet, because this
     * cannot be reconstructed afterwards: once Pro costs money, there is no way to tell
     * who had been using the app since before it did. See
     * [Monetization.GRANDFATHER_EARLY_USERS], which decides whether they keep their
     * perks. Defaulted, so older saved files load untouched.
     */
    val foundingUser: Boolean = false,
) {
    companion object {
        val Default = Preferences(
            autoClearCompleted = false,
            tutorialSeen = false,
            isPro = false,
            proPlan = "",
            widgetThemeId = WidgetThemes.Classic.id,
            clockStyleId = ClockStyle.MINIMAL,
            addToBottom = false,
            hideHintSeen = false,
            bonusThemeIds = emptySet(),
            bonusGameIds = emptySet(),
            bonusClockIds = emptySet(),
            easterEggAllowance = 1,
            easterEggClaims = 0,
            bonusPixelHeadings = false,
            onboardingDone = false,
            installedAt = 0L,
            anniversaryGiven = false,
            highScores = emptyMap(),
            wordleDay = 0L,
            wordleGuesses = emptyList(),
            wordleStreak = 0,
            wordleAttempts = 0,
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

    /**
     * Whether Pro perks are available to this person right now.
     *
     * Every gate below reads this and none reads [Preferences.isPro] directly, so there
     * is exactly one place that decides, and switching Pro on later is a change to
     * [Monetization] rather than a hunt through the app.
     *
     * Three ways to have everything:
     *  - Bits isn't charging for anything yet, so nothing is locked for anyone.
     *  - They bought it.
     *  - They were here before Bits started charging, and get to keep what they had.
     */
    val proUnlocked: Boolean
        get() = !Monetization.ENABLED ||
            preferences.isPro ||
            (Monetization.GRANDFATHER_EARLY_USERS && preferences.foundingUser)

    /** Settings for one placed widget: its own board if it has one, otherwise the shared config. */
    fun settingsFor(appWidgetId: Int): WidgetSettings =
        if (proUnlocked) boards[appWidgetId] ?: widget else widget

    fun hasOwnBoard(appWidgetId: Int): Boolean = proUnlocked && boards.containsKey(appWidgetId)

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
        return theme.free || proUnlocked || themeId in preferences.bonusThemeIds
    }

    fun canUseClockStyle(styleId: String): Boolean =
        ClockStyles.find(styleId).free || proUnlocked || styleId in preferences.bonusClockIds

    /** The pixel-heading look: Pro, or claimed through the easter egg. */
    val canUsePixelHeadings: Boolean
        get() = proUnlocked || preferences.bonusPixelHeadings

    /** Games are identified by the keys in the UI's GameId list. */
    fun canPlayGame(gameId: String, free: Boolean): Boolean =
        free || proUnlocked || gameId in preferences.bonusGameIds

    /** The theme actually drawn, falling back to Classic if a Pro theme is no longer available. */
    val activeTheme: WidgetTheme
        get() = if (canUseTheme(preferences.widgetThemeId)) WidgetThemes.find(preferences.widgetThemeId)
        else WidgetThemes.Classic

    val activeClockStyle: ClockStyle
        get() = if (canUseClockStyle(preferences.clockStyleId)) ClockStyles.find(preferences.clockStyleId)
        else ClockStyles.all.first()

    fun highScore(gameId: String): Int = preferences.highScores[gameId] ?: 0

    /** True while a reward set is still waiting to be taken. */
    val easterEggAvailable: Boolean
        get() = preferences.easterEggClaims < preferences.easterEggAllowance
}
