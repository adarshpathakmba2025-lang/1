package com.bits.app.data

/** What a brand-new user sees. */
object Seed {
    fun create(): BitsState {
        val now = System.currentTimeMillis()
        val categories = listOf(
            Category(TODAY_ID, "Today", 0),
            Category(TOMORROW_ID, "Tomorrow", 1),
            Category(GROCERY_ID, "Grocery list", 2),
        )
        val content = listOf(
            TODAY_ID to listOf("Rate us on the Play Store! :D"),
            TOMORROW_ID to listOf("Recommend us to your friends too! ^_^"),
            GROCERY_ID to listOf("Tomatoes", "Tofu"),
        )
        val items = content.flatMap { (categoryId, texts) ->
            texts.mapIndexed { index, text -> Item(newId(), text, false, categoryId, now, index) }
        }
        return BitsState(
            categories = categories,
            items = items,
            lastRollover = today(),
            // Grocery list starts hidden, so new users see how the widget toggle works.
            widget = WidgetSettings.Default.copy(hiddenCategoryIds = setOf(GROCERY_ID)),
            boards = emptyMap(),
            preferences = Preferences.Default,
        )
    }
}
