package com.bits.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON, used both for the app's own storage and for backup files. */
internal object StateJson {

    fun encode(state: BitsState, exportedAt: Long? = null): String {
        val categories = JSONArray()
        state.categories.forEach { c ->
            categories.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("order", c.order)
            )
        }

        val items = JSONArray()
        state.items.forEach { i ->
            items.put(
                JSONObject()
                    .put("id", i.id)
                    .put("text", i.text)
                    .put("done", i.done)
                    .put("categoryId", i.categoryId)
                    .put("createdAt", i.createdAt)
                    .put("position", i.position)
            )
        }

        val root = JSONObject()
            .put("app", "bits")
            .put("version", 4)
            .put("lastRollover", state.lastRollover)
            .put("categories", categories)
            .put("items", items)
            .put(
                "widget",
                JSONObject()
                    .put("opacity", state.widget.opacity.toDouble())
                    .put("showClock", state.widget.showClock)
                    .put("hiddenCategoryIds", JSONArray(state.widget.hiddenCategoryIds.toList()))
            )
            .put(
                "preferences",
                JSONObject()
                    .put("autoClearCompleted", state.preferences.autoClearCompleted)
                    .put("tutorialSeen", state.preferences.tutorialSeen)
                    .put("isPro", state.preferences.isPro)
                    .put("widgetThemeId", state.preferences.widgetThemeId)
                    .put("clockStyleId", state.preferences.clockStyleId)
                    .put("addToBottom", state.preferences.addToBottom)
                    .put("hideHintSeen", state.preferences.hideHintSeen)
                    .put("bonusThemeId", state.preferences.bonusThemeId)
                    .put("easterEggUsed", state.preferences.easterEggUsed)
                    .put("highScores", JSONObject(state.preferences.highScores.mapValues { it.value as Any }))
            )
        if (exportedAt != null) root.put("exportedAt", exportedAt)
        return root.toString()
    }

    /** Throws if the text isn't a Bits file. */
    fun decode(text: String): BitsState {
        val root = JSONObject(text)

        val categoriesJson = root.getJSONArray("categories")
        val parsedCategories = (0 until categoriesJson.length()).map { index ->
            val c = categoriesJson.getJSONObject(index)
            Category(c.getString("id"), c.getString("name"), c.optInt("order", index))
        }
        val categories = withSystemCategories(parsedCategories)
        val categoryIds = categories.map { it.id }.toSet()

        val itemsJson = root.getJSONArray("items")
        val items = (0 until itemsJson.length())
            .map { index ->
                val i = itemsJson.getJSONObject(index)
                Item(
                    id = i.getString("id"),
                    text = i.getString("text"),
                    done = i.optBoolean("done", false),
                    categoryId = i.getString("categoryId"),
                    createdAt = i.optLong("createdAt", 0L),
                    position = i.optInt("position", index),
                )
            }
            .filter { it.categoryId in categoryIds }

        return BitsState(
            categories = categories,
            items = items,
            lastRollover = root.optString("lastRollover", today()),
            widget = decodeWidget(root, categories),
            preferences = decodePreferences(root),
        )
    }

    private fun decodeWidget(root: JSONObject, categories: List<Category>): WidgetSettings {
        val widgetJson = root.optJSONObject("widget")
        if (widgetJson != null) {
            val hidden = widgetJson.optJSONArray("hiddenCategoryIds") ?: JSONArray()
            return WidgetSettings(
                opacity = widgetJson.optDouble("opacity", WidgetSettings.Default.opacity.toDouble()).toFloat(),
                showClock = widgetJson.optBoolean("showClock", true),
                hiddenCategoryIds = (0 until hidden.length()).map { hidden.getString(it) }.toSet(),
            )
        }

        // Version 0.1 stored settings per placed widget. Carry over the first one.
        val legacy = root.optJSONObject("widgets") ?: return WidgetSettings.Default
        val keys = legacy.keys()
        if (!keys.hasNext()) return WidgetSettings.Default
        val first = legacy.optJSONObject(keys.next()) ?: return WidgetSettings.Default
        val shownJson = first.optJSONArray("categoryIds") ?: JSONArray()
        val shown = (0 until shownJson.length()).map { shownJson.getString(it) }.toSet()
        return WidgetSettings(
            opacity = first.optDouble("opacity", WidgetSettings.Default.opacity.toDouble()).toFloat(),
            showClock = first.optBoolean("showClock", true),
            hiddenCategoryIds = categories.map { it.id }.filterNot { it in shown }.toSet(),
        )
    }

    private fun decodePreferences(root: JSONObject): Preferences {
        val json = root.optJSONObject("preferences") ?: return Preferences.Default
        val scoresJson = json.optJSONObject("highScores") ?: JSONObject()
        val highScores = mutableMapOf<String, Int>()
        val scoreKeys = scoresJson.keys()
        while (scoreKeys.hasNext()) {
            val key = scoreKeys.next()
            highScores[key] = scoresJson.optInt(key, 0)
        }
        return Preferences(
            autoClearCompleted = json.optBoolean("autoClearCompleted", false),
            tutorialSeen = json.optBoolean("tutorialSeen", false),
            isPro = json.optBoolean("isPro", false),
            widgetThemeId = json.optString("widgetThemeId", WidgetThemes.Classic.id),
            clockStyleId = json.optString("clockStyleId", ClockStyle.MINIMAL),
            addToBottom = json.optBoolean("addToBottom", false),
            hideHintSeen = json.optBoolean("hideHintSeen", false),
            bonusThemeId = json.optString("bonusThemeId", ""),
            easterEggUsed = json.optBoolean("easterEggUsed", false),
            highScores = highScores,
        )
    }

    /** Today and Tomorrow must always exist, even in a hand-edited or partial backup. */
    private fun withSystemCategories(categories: List<Category>): List<Category> {
        var result = categories
        if (result.none { it.id == TODAY_ID }) {
            result = listOf(Category(TODAY_ID, "Today", (result.minOfOrNull { it.order } ?: 0) - 2)) + result
        }
        if (result.none { it.id == TOMORROW_ID }) {
            result = result + Category(TOMORROW_ID, "Tomorrow", (result.maxOfOrNull { it.order } ?: 0) + 1)
        }
        return result
    }
}
