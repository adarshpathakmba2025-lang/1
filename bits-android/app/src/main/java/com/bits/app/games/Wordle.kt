package com.bits.app.games

enum class LetterMark { CORRECT, PRESENT, ABSENT }

object Wordle {
    const val LENGTH = 5
    const val MAX_GUESSES = 6

    /** A small built-in list keeps the game fully offline. */
    val answers = listOf(
        "APPLE", "BRAVE", "CRANE", "DRIFT", "EAGER", "FLAME", "GRAPE", "HOUSE",
        "IVORY", "JOLLY", "KNEEL", "LEMON", "MANGO", "NOBLE", "OCEAN", "PIANO",
        "QUIET", "RIVER", "STONE", "TIGER", "UNITY", "VIVID", "WHALE", "YACHT",
        "ZEBRA", "BLOOM", "CHARM", "DANCE", "EARTH", "FROST", "GLASS", "HONEY",
        "LIGHT", "MUSIC", "NIGHT", "PEARL", "QUILT", "ROBIN", "SUGAR", "TRUST",
    )

    private val valid = answers.toSet()

    fun randomAnswer(): String = answers.random()

    fun isAcceptable(guess: String): Boolean =
        guess.length == LENGTH && guess.uppercase() in valid

    /**
     * Standard Wordle marking. A letter is only marked PRESENT if the answer still has
     * an unmatched copy of it, so duplicate letters behave the way players expect.
     */
    fun mark(guess: String, answer: String): List<LetterMark> {
        val g = guess.uppercase()
        val a = answer.uppercase()
        val result = MutableList(LENGTH) { LetterMark.ABSENT }
        val remaining = mutableMapOf<Char, Int>()

        for (i in 0 until LENGTH) {
            if (g[i] == a[i]) result[i] = LetterMark.CORRECT
            else remaining[a[i]] = (remaining[a[i]] ?: 0) + 1
        }
        for (i in 0 until LENGTH) {
            if (result[i] == LetterMark.CORRECT) continue
            val count = remaining[g[i]] ?: 0
            if (count > 0) {
                result[i] = LetterMark.PRESENT
                remaining[g[i]] = count - 1
            }
        }
        return result
    }
}
