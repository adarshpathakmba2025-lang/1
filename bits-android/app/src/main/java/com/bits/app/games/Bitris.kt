package com.bits.app.games

import kotlin.random.Random

/**
 * Bitris: blocks fall, full rows clear.
 *
 * The falling-block rules are not anyone's to own, but the familiar arcade version's
 * look is, so nothing here borrows it. The piece set deliberately mixes three-block and
 * four-block shapes rather than using the usual seven four-block pieces, the well is
 * eighteen rows rather than twenty, the colours are Bits' own, and there is no piece to
 * hold in reserve. What is kept is the part that is only rules: gravity, rotation, and
 * clearing a filled row.
 */

/** One shape, with its four rotations worked out once at startup. */
class BitrisShape(val name: String, base: List<Point>, val colour: Int) {
    val rotations: List<List<Point>> = buildRotations(base)

    /** Width and height of a given rotation, used to keep spawns and kicks in bounds. */
    fun width(rotation: Int): Int = rotations[rotation and 3].maxOf { it.x } + 1

    private companion object {
        fun normalise(cells: List<Point>): List<Point> {
            val minX = cells.minOf { it.x }
            val minY = cells.minOf { it.y }
            return cells
                .map { Point(it.x - minX, it.y - minY) }
                .sortedWith(compareBy({ it.y }, { it.x }))
        }

        /** A quarter turn clockwise about the shape's own bounding box. */
        fun turn(cells: List<Point>): List<Point> {
            val maxY = cells.maxOf { it.y }
            return normalise(cells.map { Point(maxY - it.y, it.x) })
        }

        fun buildRotations(base: List<Point>): List<List<Point>> {
            var current = normalise(base)
            val all = mutableListOf(current)
            repeat(3) {
                current = turn(current)
                all += current
            }
            return all
        }
    }
}

/** The active piece: which shape, how it is turned, and where its box sits. */
data class BitrisActive(val shape: Int, val rotation: Int, val x: Int, val y: Int)

data class BitrisState(
    /** [Bitris.ROWS] rows of [Bitris.COLS] cells. Zero is empty; anything else is a colour. */
    val grid: List<List<Int>>,
    val active: BitrisActive?,
    val nextShape: Int,
    val score: Int,
    val lines: Int,
    val dead: Boolean,
) {
    val level: Int get() = lines / 10 + 1
}

object Bitris {

    const val COLS = 10
    const val ROWS = 18

    /** Bits' palette, indexed by the colour stored in the grid (1-based). */
    val PALETTE: List<Long> = listOf(
        0xFFF2B544, // amber
        0xFF7FD68A, // green
        0xFF5BD3D3, // teal
        0xFF9DBBFF, // periwinkle
        0xFFCFA6FF, // lilac
        0xFFE8907F, // coral
        0xFFC9B688, // sand
    )

    /**
     * Seven pieces, none of them the familiar arcade set taken whole: three of them are
     * three-block shapes, and the four-block ones leave out the mirrored pair, so the
     * well fills differently from the game this borrows its rules from.
     */
    val SHAPES: List<BitrisShape> = listOf(
        BitrisShape("bar", listOf(Point(0, 0), Point(1, 0), Point(2, 0), Point(3, 0)), 1),
        BitrisShape("box", listOf(Point(0, 0), Point(1, 0), Point(0, 1), Point(1, 1)), 2),
        BitrisShape("tee", listOf(Point(0, 0), Point(1, 0), Point(2, 0), Point(1, 1)), 3),
        BitrisShape("ell", listOf(Point(0, 0), Point(0, 1), Point(1, 1), Point(2, 1)), 4),
        BitrisShape("zig", listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(2, 1)), 5),
        BitrisShape("corner", listOf(Point(0, 0), Point(0, 1), Point(1, 1)), 6),
        BitrisShape("trio", listOf(Point(0, 0), Point(1, 0), Point(2, 0)), 7),
    )

    /** Points for clearing one to four rows at once, before the level multiplier. */
    private val CLEAR_SCORE = intArrayOf(0, 100, 300, 500, 800)

    /** Sideways nudges tried when a rotation would otherwise not fit. */
    private val KICKS = intArrayOf(0, -1, 1, -2, 2)

    private fun emptyGrid(): List<List<Int>> = List(ROWS) { List(COLS) { 0 } }

    fun newGame(random: Random = Random.Default): BitrisState {
        val first = random.nextInt(SHAPES.size)
        val state = BitrisState(
            grid = emptyGrid(),
            active = null,
            nextShape = first,
            score = 0,
            lines = 0,
            dead = false,
        )
        return spawn(state, random)
    }

    /** Where the cells of [active] actually sit on the board. */
    fun cellsOf(active: BitrisActive): List<Point> =
        SHAPES[active.shape].rotations[active.rotation and 3].map {
            Point(it.x + active.x, it.y + active.y)
        }

    /** The cells of the piece waiting next, in its own little box, for the preview. */
    fun previewCells(shapeIndex: Int): List<Point> = SHAPES[shapeIndex].rotations[0]

    /** How long a row sits before gravity pulls the piece down one, by level. */
    fun dropIntervalFor(level: Int): Long = (620L - (level - 1) * 48L).coerceAtLeast(110L)

    private fun fits(grid: List<List<Int>>, cells: List<Point>): Boolean = cells.all { cell ->
        cell.x in 0 until COLS &&
            cell.y < ROWS &&
            // Above the top of the well is allowed, so a tall piece can rotate on entry.
            (cell.y < 0 || grid[cell.y][cell.x] == 0)
    }

    private fun spawn(state: BitrisState, random: Random): BitrisState {
        val shape = state.nextShape
        val rotation = 0
        val x = (COLS - SHAPES[shape].width(rotation)) / 2
        val active = BitrisActive(shape = shape, rotation = rotation, x = x, y = 0)
        val next = random.nextInt(SHAPES.size)

        // No room for the new piece means the well is full.
        if (!fits(state.grid, cellsOf(active))) {
            return state.copy(active = null, nextShape = next, dead = true)
        }
        return state.copy(active = active, nextShape = next)
    }

    fun move(state: BitrisState, dx: Int): BitrisState {
        val active = state.active ?: return state
        if (state.dead) return state
        val moved = active.copy(x = active.x + dx)
        return if (fits(state.grid, cellsOf(moved))) state.copy(active = moved) else state
    }

    /**
     * Turns the piece a quarter clockwise, nudging it sideways if the turn would
     * otherwise be blocked. Without the nudge a piece resting against a wall could not
     * be turned at all, which reads as the controls being broken.
     */
    fun rotate(state: BitrisState): BitrisState {
        val active = state.active ?: return state
        if (state.dead) return state
        val turned = active.copy(rotation = (active.rotation + 1) and 3)
        KICKS.forEach { kick ->
            val candidate = turned.copy(x = turned.x + kick)
            if (fits(state.grid, cellsOf(candidate))) return state.copy(active = candidate)
        }
        return state
    }

    /**
     * One tick of gravity. If the piece cannot fall it is locked into the grid, any full
     * rows are cleared, and the next piece enters.
     */
    fun step(state: BitrisState, random: Random = Random.Default): BitrisState {
        if (state.dead) return state
        val active = state.active ?: return spawn(state, random)

        val dropped = active.copy(y = active.y + 1)
        if (fits(state.grid, cellsOf(dropped))) return state.copy(active = dropped)
        return lock(state, active, random)
    }

    /** Drops the piece as far as it will go and locks it, scoring the distance fallen. */
    fun hardDrop(state: BitrisState, random: Random = Random.Default): BitrisState {
        val active = state.active ?: return state
        if (state.dead) return state

        var resting = active
        var fallen = 0
        while (true) {
            val next = resting.copy(y = resting.y + 1)
            if (!fits(state.grid, cellsOf(next))) break
            resting = next
            fallen++
        }
        val landed = lock(state, resting, random)
        return if (landed.dead) landed else landed.copy(score = landed.score + fallen * 2)
    }

    /** Nudges the piece down one row by hand, worth a single point. */
    fun softDrop(state: BitrisState, random: Random = Random.Default): BitrisState {
        val active = state.active ?: return state
        if (state.dead) return state
        val dropped = active.copy(y = active.y + 1)
        return if (fits(state.grid, cellsOf(dropped))) {
            state.copy(active = dropped, score = state.score + 1)
        } else {
            lock(state, active, random)
        }
    }

    /**
     * How far the piece would fall if left alone, so the screen can show its landing
     * spot. Purely a hint; it never affects the grid.
     */
    fun ghostDrop(state: BitrisState): BitrisActive? {
        val active = state.active ?: return null
        var resting = active
        while (true) {
            val next = resting.copy(y = resting.y + 1)
            if (!fits(state.grid, cellsOf(next))) return resting
            resting = next
        }
    }

    private fun lock(state: BitrisState, active: BitrisActive, random: Random): BitrisState {
        val colour = SHAPES[active.shape].colour
        val grid = state.grid.map { it.toMutableList() }
        cellsOf(active).forEach { cell ->
            // A cell resting above the top of the well simply has nowhere to be written.
            if (cell.y in 0 until ROWS && cell.x in 0 until COLS) grid[cell.y][cell.x] = colour
        }

        val kept = grid.filterNot { row -> row.all { it != 0 } }
        val cleared = ROWS - kept.size
        val settled = List(cleared) { List(COLS) { 0 } } + kept.map { it.toList() }

        val gained = CLEAR_SCORE[cleared.coerceIn(0, 4)] * state.level
        val afterClear = state.copy(
            grid = settled,
            active = null,
            score = state.score + gained,
            lines = state.lines + cleared,
        )
        return spawn(afterClear, random)
    }
}
