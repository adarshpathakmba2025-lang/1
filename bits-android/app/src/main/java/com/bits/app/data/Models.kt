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
) {
    companion object {
        val Default = Preferences(autoClearCompleted = false, tutorialSeen = false)
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
}
