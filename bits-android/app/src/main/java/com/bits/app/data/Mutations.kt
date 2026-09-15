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

/**
 * Moves an item one category left or right in the user's own category order.
 *
 * Returns the state untouched whenever the move isn't possible: unknown item, unknown
 * category, or already at the first/last category. That makes the UI's job simply to dim
 * the arrow, while the rules here stay the single source of truth.
 */
fun BitsState.shiftItemCategory(itemId: String, forward: Boolean): BitsState {
    val item = items.firstOrNull { it.id == itemId } ?: return this
    val ordered = sortedCategories
    val index = ordered.indexOfFirst { it.id == item.categoryId }
    if (index < 0) return this
    val targetIndex = if (forward) index + 1 else index - 1
    val target = ordered.getOrNull(targetIndex) ?: return this

    // It lands on top of the destination unless the user prefers new items at the bottom.
    return if (preferences.addToBottom) {
        val max = items.filter { it.categoryId == target.id }.maxOfOrNull { it.position } ?: -1
        copy(items = items.map { if (it.id == itemId) it.copy(categoryId = target.id, position = max + 1) else it })
    } else {
        val shifted = items.map {
            when {
                it.id == itemId -> it.copy(categoryId = target.id, position = 0)
                it.categoryId == target.id -> it.copy(position = it.position + 1)
                else -> it
            }
        }
        copy(items = shifted)
    }
}

/** Whether a shift in that direction would actually do anything. */
fun BitsState.canShiftItem(itemId: String, forward: Boolean): Boolean {
    val item = items.firstOrNull { it.id == itemId } ?: return false
    val ordered = sortedCategories
    val index = ordered.indexOfFirst { it.id == item.categoryId }
    if (index < 0) return false
    return if (forward) index < ordered.lastIndex else index > 0
}

/** The category an item would land in, for labelling the arrows. */
fun BitsState.shiftTarget(itemId: String, forward: Boolean): Category? {
    val item = items.firstOrNull { it.id == itemId } ?: return null
    val ordered = sortedCategories
    val index = ordered.indexOfFirst { it.id == item.categoryId }
    if (index < 0) return null
    return ordered.getOrNull(if (forward) index + 1 else index - 1)
}

/**
 * The widget's whole visible list, flattened: every item from every shown category, in
 * the order they appear on screen. Moving through this sequence is what "reorder from the
 * widget" means, and it naturally carries an item across a category boundary.
 */
fun BitsState.widgetSequence(settings: WidgetSettings): List<Item> =
    categoriesFor(settings).flatMap { itemsIn(it.id) }

/**
 * Moves an item one place up or down the widget's flattened list. Crossing a boundary
 * moves it into the neighbouring category, landing at that category's near edge.
 *
 * Returns the state untouched at either end of the list, or for an unknown item, so the
 * UI only has to dim the arrow.
 */
fun BitsState.moveInWidgetOrder(itemId: String, settings: WidgetSettings, up: Boolean): BitsState {
    val sequence = widgetSequence(settings)
    val index = sequence.indexOfFirst { it.id == itemId }
    if (index < 0) return this
    val neighbourIndex = if (up) index - 1 else index + 1
    val neighbour = sequence.getOrNull(neighbourIndex) ?: return this
    val item = sequence[index]

    // Same category: a straight swap of positions.
    if (neighbour.categoryId == item.categoryId) {
        return copy(items = items.map {
            when (it.id) {
                item.id -> it.copy(position = neighbour.position)
                neighbour.id -> it.copy(position = item.position)
                else -> it
            }
        })
    }

    // Different category: land at the edge nearest where it came from.
    val targetId = neighbour.categoryId
    val targetItems = itemsIn(targetId)
    return if (up) {
        // Moving up means joining the end of the category above.
        val max = targetItems.maxOfOrNull { it.position } ?: -1
        copy(items = items.map { if (it.id == item.id) it.copy(categoryId = targetId, position = max + 1) else it })
    } else {
        // Moving down means joining the top of the category below, pushing it along.
        copy(items = items.map {
            when {
                it.id == item.id -> it.copy(categoryId = targetId, position = 0)
                it.categoryId == targetId -> it.copy(position = it.position + 1)
                else -> it
            }
        })
    }
}

/**
 * One row of the widget's flattened list: either a category heading or a bit.
 * Headings are fixed anchors; bits move between them.
 */
sealed interface WidgetRow {
    data class Header(val categoryId: String) : WidgetRow
    data class Entry(val itemId: String) : WidgetRow
}

/** The widget's list as draggable rows, headings included. */
fun BitsState.widgetRows(settings: WidgetSettings): List<WidgetRow> =
    categoriesFor(settings).flatMap { category ->
        listOf(WidgetRow.Header(category.id)) + itemsIn(category.id).map { WidgetRow.Entry(it.id) }
    }

/**
 * Applies a dragged arrangement. Each bit takes the category of the heading above it, and
 * its position from the order within that run, so dragging past a heading genuinely moves
 * it into that category.
 *
 * Anything not represented in [rows] is left exactly as it was, which keeps hidden
 * categories and other widgets untouched.
 */
fun BitsState.applyWidgetRows(rows: List<WidgetRow>): BitsState {
    // A bit ahead of every heading has no category to belong to. The UI already refuses
    // that drop; refusing it here too means no arrangement can ever strand a bit.
    val firstHeader = rows.indexOfFirst { it is WidgetRow.Header }
    if (firstHeader < 0) return this
    if (rows.take(firstHeader).any { it is WidgetRow.Entry }) return this

    val assignment = mutableMapOf<String, Pair<String, Int>>()
    var currentCategory: String? = null
    var index = 0
    for (row in rows) {
        when (row) {
            is WidgetRow.Header -> {
                currentCategory = row.categoryId
                index = 0
            }
            is WidgetRow.Entry -> {
                val category = currentCategory ?: continue
                assignment[row.itemId] = category to index
                index += 1
            }
        }
    }
    if (assignment.isEmpty()) return this
    return copy(items = items.map { item ->
        val target = assignment[item.id]
        if (target == null) item else item.copy(categoryId = target.first, position = target.second)
    })
}

fun BitsState.canMoveInWidgetOrder(itemId: String, settings: WidgetSettings, up: Boolean): Boolean {
    val sequence = widgetSequence(settings)
    val index = sequence.indexOfFirst { it.id == itemId }
    if (index < 0) return false
    return if (up) index > 0 else index < sequence.lastIndex
}

fun BitsState.reorderItems(orderedIds: List<String>): BitsState {
    val positions = orderedIds.withIndex().associate { (index, id) -> id to index }
    return copy(items = items.map { item -> positions[item.id]?.let { item.copy(position = it) } ?: item })
}

fun BitsState.addCategory(name: String): BitsState {
    val order = (categories.maxOfOrNull { it.order } ?: -1) + 1
    return copy(categories = categories + Category(newId(), name, order))
}

/**
 * Renames a category. Today and Tomorrow are fixed, and a blank name is refused rather
 * than leaving an unnamed heading on the widget and in the category strip.
 */
fun BitsState.renameCategory(id: String, name: String): BitsState {
    if (isSystemCategory(id)) return this
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return this
    return copy(categories = categories.map { if (it.id == id) it.copy(name = trimmed) else it })
}

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

/**
 * Sets Pro on or off. Turning it off also clears the plan, since there is then nothing
 * to cancel. Turning it on requires saying which plan, so the two always stay consistent.
 */
fun BitsState.withPro(pro: Boolean, plan: String = ProPlan.NONE): BitsState = copy(
    preferences = preferences.copy(
        isPro = pro,
        proPlan = if (pro) plan else ProPlan.NONE,
    )
)

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

/** Keeps the lowest clear ever. Zero means no record yet, so the first clear always sticks. */
fun BitsState.withMemoryTries(tries: Int): BitsState {
    if (tries <= 0) return this
    val best = preferences.memoryBestTries
    return if (best in 1 until tries) this
    else copy(preferences = preferences.copy(memoryBestTries = tries))
}

fun BitsState.resetMemoryBest(): BitsState =
    copy(preferences = preferences.copy(memoryBestTries = 0))

/** Starts a fresh day's Word Guess, clearing yesterday's board. */
fun BitsState.startWordleDay(dayIndex: Long, brokeStreak: Boolean): BitsState =
    copy(
        preferences = preferences.copy(
            wordleDay = dayIndex,
            wordleGuesses = emptyList(),
            wordleRevealed = emptySet(),
            wordleAttempts = 0,
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
            wordleAttempts = preferences.wordleAttempts + 1,
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

/** Spends points on the cheap hint, which fills in one more box. */
fun BitsState.spendOnHint(index: Int, cost: Int): BitsState {
    if (preferences.hintPoints < cost) return this
    if (index in preferences.wordleRevealed) return this
    return copy(
        preferences = preferences.copy(
            hintPoints = preferences.hintPoints - cost,
            wordleRevealed = preferences.wordleRevealed + index,
        )
    )
}

/** Counts a rejected word as an attempt, without recording it as a guess. */
fun BitsState.withRejectedAttempt(): BitsState =
    copy(preferences = preferences.copy(wordleAttempts = preferences.wordleAttempts + 1))

/**
 * Claims one whole easter-egg reward set: a theme, a game and a clock style together.
 *
 * Refuses unless a set is genuinely owed (claims < allowance), and refuses any pick that
 * is free or already owned, so a set can never be spent on something worthless. Being a
 * single atomic edit, there is no window to take a theme now and a game later.
 */
fun BitsState.claimEasterEgg(themeId: String, gameId: String, clockId: String): BitsState {
    if (!easterEggAvailable) return this

    val theme = WidgetThemes.all.firstOrNull { it.id == themeId } ?: return this
    if (theme.free || theme.id in preferences.bonusThemeIds) return this
    val clock = ClockStyles.all.firstOrNull { it.id == clockId } ?: return this
    if (clock.free || clock.id in preferences.bonusClockIds) return this
    if (gameId.isBlank() || gameId in preferences.bonusGameIds) return this

    return copy(
        preferences = preferences.copy(
            bonusThemeIds = preferences.bonusThemeIds + theme.id,
            bonusGameIds = preferences.bonusGameIds + gameId,
            bonusClockIds = preferences.bonusClockIds + clock.id,
            // The pixel-heading look rides along with any claimed set, as a small extra.
            bonusPixelHeadings = true,
            easterEggClaims = preferences.easterEggClaims + 1,
            widgetThemeId = theme.id,
        )
    )
}

/** Records first launch, once. Later calls are ignored so the clock can't be restarted. */
fun BitsState.withInstallRecorded(now: Long): BitsState =
    if (preferences.installedAt != 0L) this
    else copy(preferences = preferences.copy(installedAt = now))

/**
 * The thirty-day note. It grants nothing on its own: the reward set is still the single
 * one every user starts with. All this does is point the user at the hidden easter egg
 * they might otherwise never find.
 *
 * Skipped entirely if they already discovered it, since the note would then be pointless.
 * Happens at most once, and only once thirty days have truly passed since first launch.
 */
fun BitsState.grantAnniversary(now: Long, message: String): BitsState {
    if (preferences.anniversaryGiven) return this
    val installed = preferences.installedAt
    if (installed == 0L) return this
    if (now - installed < ANNIVERSARY_MILLIS) return this
    // Nothing left to point them at.
    if (!easterEggAvailable) return copy(preferences = preferences.copy(anniversaryGiven = true))

    val withMessage = addItem(TODAY_ID, message)
    return withMessage.copy(
        preferences = withMessage.preferences.copy(anniversaryGiven = true),
    )
}

/** Thirty days in milliseconds. */
const val ANNIVERSARY_MILLIS = 30L * 24 * 60 * 60 * 1000

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
        proPlan = device.preferences.proPlan,
        bonusThemeIds = device.preferences.bonusThemeIds,
        bonusGameIds = device.preferences.bonusGameIds,
        bonusClockIds = device.preferences.bonusClockIds,
        easterEggAllowance = device.preferences.easterEggAllowance,
        easterEggClaims = device.preferences.easterEggClaims,
        bonusPixelHeadings = device.preferences.bonusPixelHeadings,
        // The install clock and the thank-you also belong to the device, not the file.
        installedAt = device.preferences.installedAt,
        anniversaryGiven = device.preferences.anniversaryGiven,
    )
)
