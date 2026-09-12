package com.bits.app.games

import kotlin.random.Random

/** What a level's cards show. Each keeps the pixel look of the games section. */
enum class MemoryDeck(val label: String) {
    COLORS("Colours"),
    NUMBERS("Numbers"),
    LETTERS("Letters"),
    FRUIT("Fruit"),
    SHAPES("Shapes"),
}

data class MemoryCard(val id: Int, val symbol: Int, val faceUp: Boolean, val matched: Boolean)

data class MemoryLevel(val number: Int, val deck: MemoryDeck, val pairs: Int, val cards: List<MemoryCard>)

object MemoryMatch {
    /** Levels cycle through the decks and widen the board as they go. */
    fun deckFor(level: Int): MemoryDeck = MemoryDeck.entries[(level - 1).coerceAtLeast(0) % MemoryDeck.entries.size]

    fun pairsFor(level: Int): Int = when {
        level <= 1 -> 6
        level <= 3 -> 8
        else -> 10
    }

    fun newLevel(level: Int, random: Random = Random.Default): MemoryLevel {
        val pairs = pairsFor(level)
        val symbols = (0 until pairs).flatMap { listOf(it, it) }.shuffled(random)
        val cards = symbols.mapIndexed { index, symbol ->
            MemoryCard(id = index, symbol = symbol, faceUp = false, matched = false)
        }
        return MemoryLevel(number = level, deck = deckFor(level), pairs = pairs, cards = cards)
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
