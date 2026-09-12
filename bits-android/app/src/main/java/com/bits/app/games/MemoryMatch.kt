package com.bits.app.games

import kotlin.random.Random

data class MemoryCard(val id: Int, val symbol: Int, val faceUp: Boolean, val matched: Boolean)

object MemoryMatch {
    const val PAIRS = 8

    fun newGame(random: Random = Random.Default): List<MemoryCard> {
        val symbols = (0 until PAIRS).flatMap { listOf(it, it) }.shuffled(random)
        return symbols.mapIndexed { index, symbol ->
            MemoryCard(id = index, symbol = symbol, faceUp = false, matched = false)
        }
    }

    fun faceUpUnmatched(cards: List<MemoryCard>): List<MemoryCard> =
        cards.filter { it.faceUp && !it.matched }

    /** Flipping is refused while two cards are already being compared. */
    fun flip(cards: List<MemoryCard>, id: Int): List<MemoryCard> {
        val card = cards.firstOrNull { it.id == id } ?: return cards
        if (card.faceUp || card.matched) return cards
        if (faceUpUnmatched(cards).size >= 2) return cards
        return cards.map { if (it.id == id) it.copy(faceUp = true) else it }
    }

    /** Call after two cards are face up: keeps them if they match, flips them back if not. */
    fun resolve(cards: List<MemoryCard>): List<MemoryCard> {
        val open = faceUpUnmatched(cards)
        if (open.size < 2) return cards
        val (a, b) = open
        return if (a.symbol == b.symbol) {
            cards.map { if (it.id == a.id || it.id == b.id) it.copy(matched = true) else it }
        } else {
            cards.map { if (it.id == a.id || it.id == b.id) it.copy(faceUp = false) else it }
        }
    }

    fun isComplete(cards: List<MemoryCard>): Boolean = cards.all { it.matched }
}
