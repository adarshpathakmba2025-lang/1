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

    /**
     * There is no try limit: the player keeps going until the word falls. The count of
     * guesses is reported at the end instead of being used to cut them off.
     */
    const val ROWS_SHOWN_MIN = 6

    /**
     * How many letters come free. Mostly one, sometimes two, which keeps the difficulty
     * hovering between comfortably-easy and medium rather than settling into a routine.
     */
    private fun hintCountFor(random: Random): Int = if (random.nextInt(4) == 0) 2 else 1

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
        "ACORN", "BLUSH", "CHALK", "DINER", "EERIE", "FLUTE", "GIANT", "HASTE",
        "INFER", "JIFFY", "KAYAK", "LATTE", "MOUSE", "NEEDY", "OPTIC", "PLUSH",
        "QUIRK", "RANCH", "SCOUT", "TIDAL", "UPPER", "VIRUS", "WIDEN", "ANKLE",
        "BROOM", "CRUMB", "DRAPE", "ELITE", "FEAST", "GLEAM", "HOVER", "INNER",
        "JOLTS", "KNEAD", "LUMEN", "MIDST", "NICHE", "ODDLY", "PROUD", "QUELL",
        "RIFLE", "SHEEN", "TOKEN", "USAGE", "VALVE", "WHARF", "YUCCA", "AMPLE",
        "BLIMP", "CROWN", "DEBUT", "ENJOY", "FRANK", "GUSTO", "HYMNS", "IDIOM",
        "JUMPY", "KUDOS", "LOFTY", "MOTOR", "NOMAD", "ORGAN", "PIXEL", "QUOTE",
        "ROVER", "SPARK", "TRUCE", "UNDER", "VOUCH", "WINCE", "ZAPPY", "BLADE",
        "CHART", "DWARF", "EQUAL", "FLOOR", "GRACE", "HUNCH", "IVIED", "JOUST",
        "KRILL", "LUNCH", "MERCY", "OASIS", "PRIDE", "QUAIL", "ROBOT", "SIEGE",
        "THORN", "UNZIP", "VINYL", "WOULD", "YUMMY",
        // A second batch, so the daily word takes far longer to come round again
        // and far more everyday words are accepted as guesses.
        "SMILE", "STORM", "PLANT", "BRUSH", "CANAL", "DEALT", "EIGHT", "FLOCK",
        "GROWL", "HABIT", "IDLER", "KETCH", "LABEL", "MINOR", "NEVER", "OFTEN",
        "PORCH", "RAPID", "SHADE", "TRACE", "UNDUE", "VAGUE", "WEIRD", "ZEALS",
        "ABBEY", "BLAND", "CHIEF", "DITCH", "ENACT", "FETCH", "GLINT", "HEAVY",
        "INEPT", "JUDGE", "KNOLL", "LEMUR", "MOULD", "OMEGA", "PLAZA", "QUART",
        "RHINO", "SCALE", "TRAWL", "UNFIT", "VIXEN", "WRATH", "YOKEL", "ADEPT",
        "BOAST", "CREPT", "DRILL", "ELDER", "FLARE", "GRIEF", "HOARD", "INTRO",
        "JETTY", "KNOWN", "LIMIT", "MOIST", "NOBLY", "PLUCK", "QUASI", "RURAL",
        "SHARP", "TWINE", "UPSET", "VOTER", "WOKEN", "ARENA", "BRINY", "CLASP",
        "DEITY", "EPOXY", "FJORD", "GUILT", "HYENA", "ICING", "JERKY", "KHAKI",
        "LOUSY", "MEDIC", "OPINE", "PRANK", "QUOTH", "REALM", "SHRUB", "TIMID",
        "UNTIE", "VERGE", "WIDTH", "YOGIC", "ANVIL", "BEGIN", "CRAVE", "DOSED",
        "EMCEE", "FILET", "GRUNT", "HOIST", "IRATE", "JOKED", "KAPPA", "LUCID",
        "MOPED", "NEIGH", "OVERT", "PLAIT", "QUEEN", "RETRO", "SWORE", "TEPID",
        "UNIFY", "VOILE", "WOOZY", "ZILLS", "AWARE", "BLUNT", "DERBY", "ENVOY",
        "FORGE", "GIDDY", "HOARY", "INCUR", "JADED", "KRAAL", "LIVID", "MUSKY",
        "NYMPH", "OUNCE", "PESTO", "QUIRE", "RIVET", "SUAVE", "TRUMP", "UNCAP",
        "VAPID", "WRUNG", "YOURS", "AISLE", "BATCH", "CHUTE", "DINGY", "ENSUE",
        "FLASK", "GRIMY", "HUSKY", "INBOX", "JOULE", "KNURL", "LOAMY", "MOTTO",
        "NOOSE", "OPALS", "PRONG", "QUACK", "RUMOR", "SNACK", "TABOO", "UNDID",
        "VERSE", "WHINE", "AMUSE", "BRAWN", "CLOVE", "DRYER", "EJECT", "FRAIL",
        "GONER", "HELIX", "INLAY", "JOKES", "KELPS", "LURCH", "MURAL", "NICER",
        "ODORS", "PUDGY", "RIPEN", "SMIRK", "TRIKE", "UNLIT", "VOCAL", "WEDGE",
        "YAWNS", "ABLED", "BRIEF", "CROAK", "DRAIN", "EXALT", "FLUID", "GRIST",
        "HASTY", "IMBUE", "JUROR", "KNAVE", "LATER", "MOODY", "NINTH", "OCTET",
        "PLIED", "QUAFF", "RUDDY", "SHOAL", "TAFFY", "UNMET", "VIRAL", "WHIRL",
        "ZEBUS", "ACRID", "BOSOM", "CHAOS", "DOILY", "EVOKE", "FLESH", "GRAZE",
        "HEIST", "INTER", "JIVED", "KNELT", "LUPIN", "MANIA", "NASAL", "OFFAL",
        "PARKA", "QUIPS", "RINSE", "SCOWL", "TRIAD", "UNWON", "VISOR", "WOODY",
    )

    private val valid = answers.toSet()

    /** Days since the epoch, the same for everyone in a given local day. */
    fun todayIndex(epochDay: Long): Long = epochDay

    /**
     * The puzzle for a given day. Fully deterministic, so the same day always gives the
     * same word and the same hint positions, but both move around from day to day.
     */
    fun puzzleFor(dayIndex: Long): WordPuzzle {
        // Mixing the day index keeps consecutive days from picking neighbouring words,
        // and gives hint positions that jump around instead of marching along the row.
        val seed = dayIndex * 0x9E3779B97F4A7C15uL.toLong() xor (dayIndex shl 21)
        val random = Random(seed)
        val answer = answers[((dayIndex * 7919L).mod(answers.size.toLong())).toInt()]
        val revealed = (0 until LENGTH).shuffled(random).take(hintCountFor(random)).toSet()
        return WordPuzzle(dayIndex, answer, revealed)
    }

    /** Cost in hint points to reveal one whole letter of the player's choosing. */
    const val REVEAL_COST = 5

    /** Cost to be told one letter that appears somewhere in the word. */
    const val PEEK_COST = 1

/**
     * The next position the cheap hint should fill in: the leftmost box the player hasn't
     * been given and hasn't already pinned down with a correct guess. Because each hint
     * consumes a position, repeated hints never repeat themselves.
     *
     * Returns null once every box is known.
     */
    fun nextHintIndex(puzzle: WordPuzzle, guesses: List<String>, purchased: Set<Int>): Int? {
        val solvedPositions = buildSet {
            addAll(puzzle.revealed)
            addAll(purchased)
            guesses.forEach { guess ->
                mark(guess, puzzle.answer).forEachIndexed { i, m ->
                    if (m == LetterMark.CORRECT) add(i)
                }
            }
        }
        return (0 until LENGTH).firstOrNull { it !in solvedPositions }
    }

    /** Positions still worth buying outright: not free, not already bought. */
    fun revealableIndices(puzzle: WordPuzzle, purchased: Set<Int>): List<Int> =
        (0 until LENGTH).filterNot { it in puzzle.revealed || it in purchased }

    fun isAcceptable(guess: String): Boolean =
        guess.length == LENGTH && guess.uppercase() in valid

    /**
     * Positions the player actually types into, left to right. Free hints and any
     * letters bought with hint points are excluded, so neither can be typed over.
     */
    fun editableIndices(puzzle: WordPuzzle, purchased: Set<Int> = emptySet()): List<Int> =
        (0 until LENGTH).filterNot { it in puzzle.revealed || it in purchased }

    /** Every position whose letter is already shown, free or bought. */
    fun shownIndices(puzzle: WordPuzzle, purchased: Set<Int> = emptySet()): Set<Int> =
        puzzle.revealed + purchased

    /**
     * Builds the full guess from the letters the player typed plus the free letters.
     * Typed input only ever covers the editable slots, so a hint can't be overwritten.
     */
    fun assembleGuess(typed: String, puzzle: WordPuzzle, purchased: Set<Int> = emptySet()): String {
        val slots = editableIndices(puzzle, purchased)
        val shown = shownIndices(puzzle, purchased)
        val chars = CharArray(LENGTH)
        for (i in 0 until LENGTH) {
            chars[i] = if (i in shown) puzzle.answer[i] else ' '
        }
        typed.forEachIndexed { index, ch ->
            if (index < slots.size) chars[slots[index]] = ch.uppercaseChar()
        }
        return chars.concatToString()
    }

    fun isComplete(typed: String, puzzle: WordPuzzle, purchased: Set<Int> = emptySet()): Boolean =
        typed.length >= editableIndices(puzzle, purchased).size

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
