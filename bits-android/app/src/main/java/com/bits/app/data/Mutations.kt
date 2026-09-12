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
    )

fun BitsState.reorderCategories(orderedIds: List<String>): BitsState {
    val positions = orderedIds.withIndex().associate { (index, id) -> id to index }
    return copy(categories = categories.map { c -> positions[c.id]?.let { c.copy(order = it) } ?: c })
}

fun BitsState.setShownOnWidget(categoryId: String, shown: Boolean): BitsState {
    val hidden = if (shown) widget.hiddenCategoryIds - categoryId else widget.hiddenCategoryIds + categoryId
    return copy(widget = widget.copy(hiddenCategoryIds = hidden))
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

/** Claims the easter-egg theme. Only ever works once per device. */
fun BitsState.claimBonusTheme(themeId: String): BitsState {
    if (preferences.bonusThemeId.isNotEmpty()) return this
    val theme = WidgetThemes.find(themeId)
    if (theme.free) return this
    return copy(preferences = preferences.copy(bonusThemeId = themeId, widgetThemeId = themeId))
}

fun BitsState.withEasterEggUsed(): BitsState =
    copy(preferences = preferences.copy(easterEggUsed = true))
