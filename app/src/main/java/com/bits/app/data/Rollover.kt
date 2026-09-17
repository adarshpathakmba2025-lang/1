package com.bits.app.data

import java.time.LocalDate

fun today(): String = LocalDate.now().toString()

/**
 * The automatic day transition. Runs at most once per calendar day.
 *
 * 1. If "Clear finished items at midnight" is on, checked-off items are removed everywhere.
 * 2. Unfinished Tomorrow items move to the TOP of Today, keeping their order relative to
 *    each other, so what you planned last night is the first thing you see.
 *
 * This is checked every time data is read, so it's correct even if the
 * midnight alarm fires late or not at all.
 */
object Rollover {
    fun apply(state: BitsState, todayKey: String): BitsState {
        if (state.lastRollover == todayKey) return state

        val remaining = if (state.preferences.autoClearCompleted) state.items.filterNot { it.done } else state.items

        val moving = remaining
            .filter { it.categoryId == TOMORROW_ID && !it.done }
            .sortedBy { it.position }
        val arrivalPositions = moving.withIndex().associate { (index, item) -> item.id to index }
        val shift = moving.size

        val items = remaining.map { item ->
            val arrival = arrivalPositions[item.id]
            when {
                // Arrivals take the first slots, in the order they had in Tomorrow.
                arrival != null -> item.copy(categoryId = TODAY_ID, position = arrival)
                // Everything already in Today slides down to make room.
                item.categoryId == TODAY_ID -> item.copy(position = item.position + shift)
                else -> item
            }
        }
        return state.copy(items = items, lastRollover = todayKey)
    }
}
