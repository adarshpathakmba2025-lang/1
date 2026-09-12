package com.bits.app.games

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/** A 4x4 board of tile values, 0 meaning empty. Row-major: board[row][col]. */
typealias Board = List<List<Int>>

data class MoveResult(val board: Board, val moved: Boolean, val gained: Int)

/** The 2048 rules, kept free of any UI or Android dependency so they can be tested directly. */
object Game2048 {
    const val SIZE = 4

    fun emptyBoard(): Board = List(SIZE) { List(SIZE) { 0 } }

    fun newGame(random: Random = Random.Default): Board =
        spawnTile(spawnTile(emptyBoard(), random), random)

    /** Adds one new tile (90% a 2, 10% a 4) into a random empty cell. No-op if the board is full. */
    fun spawnTile(board: Board, random: Random = Random.Default): Board {
        val empties = cells().filter { (r, c) -> board[r][c] == 0 }
        if (empties.isEmpty()) return board
        val (r, c) = empties[random.nextInt(empties.size)]
        val value = if (random.nextInt(10) == 0) 4 else 2
        return board.mapIndexed { row, line ->
            if (row != r) line else line.mapIndexed { col, v -> if (col == c) value else v }
        }
    }

    /**
     * Slides and merges every row/column in the given direction.
     * Each tile merges into at most one other tile per move, and a merged tile
     * cannot merge again in the same move.
     */
    fun move(board: Board, direction: Direction): MoveResult {
        val rotated = toCanonical(board, direction)
        var gained = 0
        val result = rotated.map { line ->
            val (merged, points) = mergeLine(line)
            gained += points
            merged
        }
        val finalBoard = fromCanonical(result, direction)
        return MoveResult(finalBoard, finalBoard != board, gained)
    }

    fun isGameOver(board: Board): Boolean {
        if (cells().any { (r, c) -> board[r][c] == 0 }) return false
        return Direction.entries.none { move(board, it).moved }
    }

    fun hasWon(board: Board): Boolean = cells().any { (r, c) -> board[r][c] >= 2048 }

    private fun cells(): List<Pair<Int, Int>> =
        (0 until SIZE).flatMap { r -> (0 until SIZE).map { c -> r to c } }

    /** Slides non-zero values to the front of the line, merges equal neighbours once each, pads with zeros. */
    private fun mergeLine(line: List<Int>): Pair<List<Int>, Int> {
        val values = line.filter { it != 0 }.toMutableList()
        val merged = mutableListOf<Int>()
        var points = 0
        var i = 0
        while (i < values.size) {
            val current = values[i]
            if (i + 1 < values.size && values[i + 1] == current) {
                val sum = current * 2
                merged.add(sum)
                points += sum
                i += 2
            } else {
                merged.add(current)
                i += 1
            }
        }
        while (merged.size < SIZE) merged.add(0)
        return merged to points
    }

    /** Rotates the board so that "move left" logic can handle every direction uniformly. */
    private fun toCanonical(board: Board, direction: Direction): Board = when (direction) {
        Direction.LEFT -> board
        Direction.RIGHT -> board.map { it.reversed() }
        Direction.UP -> transpose(board)
        Direction.DOWN -> transpose(board).map { it.reversed() }
    }

    private fun fromCanonical(board: Board, direction: Direction): Board = when (direction) {
        Direction.LEFT -> board
        Direction.RIGHT -> board.map { it.reversed() }
        Direction.UP -> transpose(board)
        Direction.DOWN -> transpose(board.map { it.reversed() })
    }

    private fun transpose(board: Board): Board =
        (0 until SIZE).map { col -> (0 until SIZE).map { row -> board[row][col] } }
}
