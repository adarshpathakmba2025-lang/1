package com.bits.app.games

/** Board cells: 0 empty, 1 player (X), 2 computer (O). Indices 0..8, row-major. */
object TicTacToe {
    const val EMPTY = 0
    const val X = 1
    const val O = 2

    private val lines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6),
    )

    fun empty(): List<Int> = List(9) { EMPTY }

    fun winner(board: List<Int>): Int {
        for (line in lines) {
            val first = board[line[0]]
            if (first != EMPTY && line.all { board[it] == first }) return first
        }
        return EMPTY
    }

    fun winningLine(board: List<Int>): List<Int>? =
        lines.firstOrNull { line ->
            val first = board[line[0]]
            first != EMPTY && line.all { board[it] == first }
        }

    fun isFull(board: List<Int>): Boolean = board.none { it == EMPTY }

    fun isOver(board: List<Int>): Boolean = winner(board) != EMPTY || isFull(board)

    fun play(board: List<Int>, index: Int, player: Int): List<Int> =
        if (board[index] != EMPTY || isOver(board)) board
        else board.toMutableList().also { it[index] = player }

    /**
     * Perfect play via minimax. The computer never loses, so the best a player
     * can manage is a draw — which is the point of the classic game.
     *
     * Scores are weighted by depth so a win available right now beats the same win
     * three moves later. Without that, every winning line scores alike and the
     * computer can wander into a block when it could simply have won.
     */
    fun bestMove(board: List<Int>, player: Int): Int {
        var bestScore = Int.MIN_VALUE
        var move = -1
        for (i in board.indices) {
            if (board[i] != EMPTY) continue
            val score = minimax(play(board, i, player), player, maximizing = false, depth = 1)
            if (score > bestScore) {
                bestScore = score
                move = i
            }
        }
        return move
    }

    private fun minimax(board: List<Int>, player: Int, maximizing: Boolean, depth: Int): Int {
        val w = winner(board)
        if (w == player) return 10 - depth
        if (w != EMPTY) return depth - 10
        if (isFull(board)) return 0

        val opponent = if (player == X) O else X
        val turn = if (maximizing) player else opponent
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE
        for (i in board.indices) {
            if (board[i] != EMPTY) continue
            val score = minimax(play(board, i, turn), player, !maximizing, depth + 1)
            best = if (maximizing) maxOf(best, score) else minOf(best, score)
        }
        return best
    }
}
