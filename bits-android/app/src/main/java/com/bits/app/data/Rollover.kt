package com.bits.app.data

import java.time.LocalDate

fun today(): String = LocalDate.now().toString()

/**
 * The automatic day transition. Runs at most once per calendar day.
 *
 * 1. If "Clear finished items at midnight" is on, checked-off items are removed from every category.
 * 2. Unfinished Tomorrow items move to the end of Today, keeping their order.
 *
 * This is checked every time data is read, so it's correct even if the
 * midnight alarm fires late or not at all.
 */
object Rollover {
    fun apply(state: BitsState, todayKey: String): BitsState {
        if (state.lastRollover == todayKey) return state

        val remaining = if (state.preferences.autoClearCompleted) state.items.filterNot { it.done } else state.items

        val todayMax = remaining.filter { it.categoryId == TODAY_ID }.maxOfOrNull { it.position } ?: -1
        val moving = remaining
            .filter { it.categoryId == TOMORROW_ID && !it.done }
            .sortedBy { it.position }
        val newPositions = moving.mapIndexed { index, item -> item.id to (todayMax + 1 + index) }.toMap()
        val items = remaining.map { item ->
            val position = newPositions[item.id]
            if (position != null) item.copy(categoryId = TODAY_ID, position = position) else item
        }
        return state.copy(items = items, lastRollover = todayKey)
    }
}
