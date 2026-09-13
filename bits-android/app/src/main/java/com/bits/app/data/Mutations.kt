package com.bits.app.data

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Adds a new item at the top of its category, or the bottom if that preference is on. */
fun BitsState.addItem(categoryId: String, text: String): BitsState {
    val inCategory = items.filter { it.categoryId == categoryId }
    val newItem = Item(
        id = newId(),
        text = text,
        done = false,
        categoryId = categoryId,
        createdAt = System.currentTimeMillis(),
        position = 0,
    )
    return if (preferences.addToBottom) {
        val max = inCategory.maxOfOrNull { it.position } ?: -1
        copy(items = items + newItem.copy(position = max + 1))
    } else {
        // Everything already there shifts down by one so the new item sits first.
        val shifted = items.map { if (it.categoryId == categoryId) it.copy(position = it.position + 1) else it }
        copy(items = shifted + newItem)
    }
}

fun BitsState.toggleItem(id: String): BitsState =
    copy(items = items.map { if (it.id == id) it.copy(done = !it.done) else it })

fun BitsState.editItem(id: String, text: String): BitsState =
    copy(items = items.map { if (it.id == id) it.copy(text = text) else it })

fun BitsState.deleteItem(id: String): BitsState =
    copy(items = items.filterNot { it.id == id })

/**
 * Puts a deleted item back exactly where it was, nudging anything that has since taken
 * its slot. Safe to call twice: if the item is already present, nothing changes.
 */
fun BitsState.restoreItem(item: Item): BitsState {
    if (items.any { it.id == item.id }) return this
    if (categories.none { it.id == item.categoryId }) return this
    val shifted = items.map {
        if (it.categoryId == item.categoryId && it.position >= item.position) it.copy(position = it.position + 1) else it
    }
    return copy(items = shifted + item)
}

fun BitsState.reorderItems(orderedIds: List<String>): BitsState {
    val positions = orderedIds.withIndex().associate { (index, id) -> id to index }
    return copy(items = items.map { item -> positions[item.id]?.let { item.copy(position = it) } ?: item })
}

fun BitsState.addCategory(name: String): BitsState {
    val order = (categories.maxOfOrNull { it.order } ?: -1) + 1
    return copy(categories = categories + Category(newId(), name, order))
}

fun BitsState.renameCategory(id: String, name: String): BitsState =
    if (isSystemCategory(id)) this
    else copy(categories = categories.map { if (it.id == id) it.copy(name = name) else it })

fun BitsState.deleteCategory(id: String): BitsState =
    if (isSystemCategory(id)) this
    else copy(
        categories = categories.filterNot { it.id == id },
        items = items.filterNot { it.categoryId == id },
        widget = widget.copy(hiddenCategoryIds = widget.hiddenCategoryIds - id),
        boards = boards.mapValues { (_, s) -> s.copy(hiddenCategoryIds = s.hiddenCategoryIds - id) },
    )

fun BitsState.reorderCategories(orderedIds: List<String>): BitsState {
    val positions = orderedIds.withIndex().associate { (index, id) -> id to index }
    return copy(categories = categories.map { c -> positions[c.id]?.let { c.copy(order = it) } ?: c })
}

fun BitsState.setShownOnWidget(categoryId: String, shown: Boolean): BitsState {
    val hidden = if (shown) widget.hiddenCategoryIds - categoryId else widget.hiddenCategoryIds + categoryId
    return copy(widget = widget.copy(hiddenCategoryIds = hidden))
}

/** Per-board editing. Creating a board copies the shared settings so nothing jumps visually. */
fun BitsState.editBoard(appWidgetId: Int, transform: (WidgetSettings) -> WidgetSettings): BitsState {
    if (!preferences.isPro) return this
    val existing = boards[appWidgetId] ?: widget
    return copy(boards = boards + (appWidgetId to transform(existing)))
}

fun BitsState.setShownOnBoard(appWidgetId: Int, categoryId: String, shown: Boolean): BitsState =
    editBoard(appWidgetId) { settings ->
        val hidden = if (shown) settings.hiddenCategoryIds - categoryId else settings.hiddenCategoryIds + categoryId
        settings.copy(hiddenCategoryIds = hidden)
    }

/** Drops a board so the widget goes back to following the shared settings. */
fun BitsState.resetBoard(appWidgetId: Int): BitsState = copy(boards = boards - appWidgetId)

/** Removes boards for widgets that are no longer on the home screen. */
fun BitsState.pruneBoards(livingIds: Set<Int>): BitsState {
    val kept = boards.filterKeys { it in livingIds }
    return if (kept.size == boards.size) this else copy(boards = kept)
}

fun BitsState.withWidgetOpacity(value: Float): BitsState =
    copy(widget = widget.copy(opacity = value))

fun BitsState.withClock(show: Boolean): BitsState =
    copy(widget = widget.copy(showClock = show))

fun BitsState.withAutoClear(enabled: Boolean): BitsState =
    copy(preferences = preferences.copy(autoClearCompleted = enabled))

fun BitsState.withTutorialSeen(seen: Boolean): BitsState =
    copy(preferences = preferences.copy(tutorialSeen = seen))

fun BitsState.withPro(pro: Boolean): BitsState =
    copy(preferences = preferences.copy(isPro = pro))

/** Only takes effect if the theme is free or the user is already Pro; otherwise the state is unchanged. */
fun BitsState.withWidgetTheme(themeId: String): BitsState =
    if (canUseTheme(themeId)) copy(preferences = preferences.copy(widgetThemeId = themeId)) else this

fun BitsState.withClockStyle(styleId: String): BitsState =
    if (canUseClockStyle(styleId)) copy(preferences = preferences.copy(clockStyleId = styleId)) else this

fun BitsState.withAddToBottom(enabled: Boolean): BitsState =
    copy(preferences = preferences.copy(addToBottom = enabled))

fun BitsState.withHideHintSeen(): BitsState =
    copy(preferences = preferences.copy(hideHintSeen = true))

fun BitsState.withHighScore(gameId: String, score: Int): BitsState =
    if (score <= highScore(gameId)) this
    else copy(preferences = preferences.copy(highScores = preferences.highScores + (gameId to score)))

/**
 * Claims the entire easter-egg reward in one shot: one theme, one game, one clock style.
 *
 * Deliberately all-or-nothing and one-way. It refuses outright if the egg has already
 * been claimed, if any bonus slot is already filled, or if any chosen item is a free one
 * (which would waste the pick). Because it is a single atomic edit, there is no window
 * in which a caller could claim a theme, then come back later for a second game.
 */
fun BitsState.claimEasterEgg(themeId: String, gameId: String, clockId: String): BitsState {
    if (preferences.easterEggUsed) return this
    if (preferences.bonusThemeId.isNotEmpty()) return this
    if (preferences.bonusGameId.isNotEmpty()) return this
    if (preferences.bonusClockId.isNotEmpty()) return this

    // Every pick must be a real locked item, and must actually exist.
    val theme = WidgetThemes.all.firstOrNull { it.id == themeId } ?: return this
    if (theme.free) return this
    val clock = ClockStyles.all.firstOrNull { it.id == clockId } ?: return this
    if (clock.free) return this
    if (gameId.isBlank()) return this

    return copy(
        preferences = preferences.copy(
            bonusThemeId = theme.id,
            bonusGameId = gameId,
            bonusClockId = clock.id,
            easterEggUsed = true,
            widgetThemeId = theme.id,
        )
    )
}

/** Starts a fresh day's Word Guess, clearing yesterday's board. */
fun BitsState.startWordleDay(dayIndex: Long, brokeStreak: Boolean): BitsState =
    copy(
        preferences = preferences.copy(
            wordleDay = dayIndex,
            wordleGuesses = emptyList(),
            wordleRevealed = emptySet(),
            wordleStreak = if (brokeStreak) 0 else preferences.wordleStreak,
        )
    )

/**
 * Records a guess. Every completed row earns one hint point, so play itself funds the
 * hints rather than only a win doing so.
 */
fun BitsState.withWordleGuess(dayIndex: Long, guess: String, solved: Boolean): BitsState =
    copy(
        preferences = preferences.copy(
            wordleDay = dayIndex,
            wordleGuesses = preferences.wordleGuesses + guess,
            wordleStreak = if (solved) preferences.wordleStreak + 1 else preferences.wordleStreak,
            hintPoints = preferences.hintPoints + 1,
        )
    )

/** Buys one letter outright. Refuses unless the points are actually there. */
fun BitsState.spendOnReveal(index: Int, cost: Int): BitsState {
    if (preferences.hintPoints < cost) return this
    if (index in preferences.wordleRevealed) return this
    return copy(
        preferences = preferences.copy(
            hintPoints = preferences.hintPoints - cost,
            wordleRevealed = preferences.wordleRevealed + index,
        )
    )
}

/** Spends points on the cheap "one letter is in there somewhere" hint. */
fun BitsState.spendPoints(cost: Int): BitsState =
    if (preferences.hintPoints < cost) this
    else copy(preferences = preferences.copy(hintPoints = preferences.hintPoints - cost))

fun BitsState.withOnboardingDone(): BitsState =
    copy(preferences = preferences.copy(onboardingDone = true))

/**
 * Entitlements belong to this device, not to a backup file. Restoring brings back
 * lists, categories and widget settings, but never Pro or easter-egg unlocks, so a
 * hand-edited backup can't be used to grant them.
 */
fun BitsState.withEntitlementsFrom(device: BitsState): BitsState = copy(
    preferences = preferences.copy(
        isPro = device.preferences.isPro,
        bonusThemeId = device.preferences.bonusThemeId,
        bonusGameId = device.preferences.bonusGameId,
        bonusClockId = device.preferences.bonusClockId,
        easterEggUsed = device.preferences.easterEggUsed,
    )
)
