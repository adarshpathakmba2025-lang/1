package com.bits.app.games

import kotlin.random.Random

enum class LetterMark { CORRECT, PRESENT, ABSENT }

/**
 * One day's puzzle. [revealed] positions are given away for free and are never typed
 * by the player, so a guess is always assembled from the hints plus the typed letters.
 */
data class WordPuzzle(val dayIndex: Long, val answer: String, val revealed: Set<Int>)

object Wordle {
    const val LENGTH = 5
    const val MAX_GUESSES = 6

    /** One free letter keeps it a notch above easy without giving the word away. */
    private const val HINTS = 1

    val answers = listOf(
        "APPLE", "BRAVE", "CRANE", "DRIFT", "EAGER", "FLAME", "GRAPE", "HOUSE",
        "IVORY", "JOLLY", "KNEEL", "LEMON", "MANGO", "NOBLE", "OCEAN", "PIANO",
        "QUIET", "RIVER", "STONE", "TIGER", "UNITY", "VIVID", "WHALE", "YACHT",
        "ZEBRA", "BLOOM", "CHARM", "DANCE", "EARTH", "FROST", "GLASS", "HONEY",
        "LIGHT", "MUSIC", "NIGHT", "PEARL", "QUILT", "ROBIN", "SUGAR", "TRUST",
        "AMBER", "BEACH", "CLOUD", "DREAM", "EMBER", "FABLE", "GHOST", "HEART",
        "INDEX", "JOKER", "KARMA", "LUNAR", "MAPLE", "NURSE", "ORBIT", "PLUMB",
        "QUEST", "RUSTY", "SHINE", "TULIP", "URBAN", "VAPOR", "WAGON", "XENON",
        "YIELD", "ZONAL", "ADOBE", "BADGE", "CABIN", "DELTA", "EQUIP", "FLINT",
        "GRIND", "HAZEL", "INLET", "JUICE", "KIOSK", "LATCH", "MERIT", "NOTCH",
        "OLIVE", "PRISM", "QUARK", "RIDGE", "SPICE", "THUMB", "ULTRA", "VOWEL",
        "WHISK", "YOUNG", "ZESTY", "ANGEL", "BRICK", "CHESS", "DOUGH", "ELBOW",
        "FUDGE", "GRASS", "HOTEL", "IMAGE", "JEWEL", "KNACK", "LODGE", "MOUNT",
        "NYLON", "ONION", "PATCH", "RAVEN", "SOLAR", "TOWEL", "VAULT", "WOVEN",
        "ALBUM", "BLAZE", "COMET", "DIZZY", "EXTRA", "FROWN", "GAUGE", "HUMID",
        "IRONY", "JUMBO", "LEAPT", "MIRTH", "NOVEL", "PLANK", "QUOTA", "ROAST",
        "SWIRL", "TREND", "USHER", "VIGOR", "WRIST", "YEAST", "ARROW", "BERRY",
        "CIDER", "DWELL", "ELECT", "FIBER", "GLOBE", "HATCH", "INPUT", "BLINK",
        "CRISP", "DODGE", "EAGLE", "FAULT", "GRAVY", "HINGE", "IGLOO", "JOINT",
        "KNIFE", "LYRIC", "MEDAL", "NUDGE", "OPERA", "PIVOT", "QUILL", "RHYME",
        "SCARF", "TORCH", "UNWED", "VENUE", "WALTZ", "YOUTH", "ZILCH", "ABIDE",
        "BOUGH", "CHIME", "DRAWN", "ETHIC", "FLOAT", "GUILD", "HUMOR", "IDEAL",
        "JAUNT", "KRAFT", "LOYAL", "MOTIF", "NINJA", "OZONE", "PROBE", "QUAKE",
        "RELIC", "SHEEP", "TEMPO", "UNCLE", "VISTA", "WHEAT", "YODEL", "ZONES",
        "BISON", "CANDY", "DEPTH", "EMPTY", "FERRY", "GRAIN", "HEDGE", "ISSUE",
        "JELLY", "KNOTS", "LEDGE", "MARSH", "NOISE", "OUGHT", "PUNCH", "QUERY",
        "ROUND", "SHELF", "TRAIL", "UNITE", "VOICE", "WEAVE", "YEARN", "ZEBEC",
    )

    private val valid = answers.toSet()

    /** Days since the epoch, the same for everyone in a given local day. */
    fun todayIndex(epochDay: Long): Long = epochDay

    /**
     * The puzzle for a given day. Fully deterministic, so the same day always gives the
     * same word and the same hint positions, but both move around from day to day.
     */
    fun puzzleFor(dayIndex: Long): WordPuzzle {
        // Mixing the day index keeps consecutive days from picking neighbouring words.
        val seed = dayIndex * 0x9E3779B97F4A7C15uL.toLong()
        val random = Random(seed)
        val answer = answers[((dayIndex * 7919L).mod(answers.size.toLong())).toInt()]
        val revealed = (0 until LENGTH).shuffled(random).take(HINTS).toSet()
        return WordPuzzle(dayIndex, answer, revealed)
    }

    fun isAcceptable(guess: String): Boolean =
        guess.length == LENGTH && guess.uppercase() in valid

    /** Positions the player actually types into, left to right. */
    fun editableIndices(puzzle: WordPuzzle): List<Int> =
        (0 until LENGTH).filterNot { it in puzzle.revealed }

    /**
     * Builds the full guess from the letters the player typed plus the free letters.
     * Typed input only ever covers the editable slots, so a hint can't be overwritten.
     */
    fun assembleGuess(typed: String, puzzle: WordPuzzle): String {
        val slots = editableIndices(puzzle)
        val chars = CharArray(LENGTH)
        for (i in 0 until LENGTH) {
            chars[i] = if (i in puzzle.revealed) puzzle.answer[i] else ' '
        }
        typed.forEachIndexed { index, ch ->
            if (index < slots.size) chars[slots[index]] = ch.uppercaseChar()
        }
        return chars.concatToString()
    }

    fun isComplete(typed: String, puzzle: WordPuzzle): Boolean =
        typed.length >= editableIndices(puzzle).size

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
