package com.bits.app.games

import kotlin.random.Random

/**
 * A modest chess opponent: look a few moves ahead, assume the other side plays its best
 * reply, and pick what leaves the position looking healthiest.
 *
 * It is not trying to be strong. It is trying to be a reasonable game for someone
 * playing on their phone, and above all to be quick and to never play an illegal move -
 * every move it considers comes from the same generator the rules use, so it cannot.
 */
object ChessAi {

    /** Rough worth of each piece, in pawns times a hundred. */
    private val VALUE = intArrayOf(0, 100, 320, 330, 500, 900, 0)

    /**
     * A small bonus for sitting nearer the middle, which is enough to stop it shuffling
     * pieces along the edges in the opening. Indexed by distance from the centre.
     */
    private val CENTRE_BONUS = intArrayOf(12, 8, 3, 0)

    /** Ply depth. Three is quick enough to feel responsive and still spots simple traps. */
    const val DEPTH = 3

    private const val MATE = 100_000

    /**
     * The move the computer wants to play, or null if it has none, in which case the
     * game is already over.
     *
     * [random] only breaks ties between equally good moves, so the opening is not
     * identical every single game.
     */
    fun chooseMove(state: ChessState, depth: Int = DEPTH, random: Random = Random.Default): ChessMove? {
        val moves = order(state, Chess.legalMoves(state))
        if (moves.isEmpty()) return null

        var best = Int.MIN_VALUE
        val bestMoves = ArrayList<ChessMove>(4)
        moves.forEach { move ->
            val score = -search(Chess.apply(state, move), depth - 1, -MATE * 2, MATE * 2)
            if (score > best) {
                best = score
                bestMoves.clear()
                bestMoves += move
            } else if (score == best) {
                bestMoves += move
            }
        }
        return bestMoves[random.nextInt(bestMoves.size)]
    }

    /**
     * Negamax with alpha-beta pruning: the score is always from the point of view of
     * whoever is to move, and a line is abandoned as soon as it is clear the opponent
     * would never allow it.
     */
    private fun search(state: ChessState, depth: Int, alpha: Int, beta: Int): Int {
        val moves = Chess.legalMoves(state)

        if (moves.isEmpty()) {
            // Mate is worth less the further off it is, so it prefers the quick one.
            return if (Chess.isInCheck(state, state.whiteToMove)) -(MATE + depth) else 0
        }
        // A drawn position is worth nothing to either side, whatever is on the board.
        if (state.halfmoveClock >= 100 || Chess.isDeadPosition(state.board)) return 0
        if (depth <= 0) return evaluate(state)

        var lower = alpha
        order(state, moves).forEach { move ->
            val score = -search(Chess.apply(state, move), depth - 1, -beta, -lower)
            if (score >= beta) return beta
            if (score > lower) lower = score
        }
        return lower
    }

    /**
     * Captures first, biggest prize first. Pruning works far better when the promising
     * moves are tried early, which is most of why this is fast enough to use.
     */
    private fun order(state: ChessState, moves: List<ChessMove>): List<ChessMove> =
        moves.sortedByDescending { move ->
            val victim = state.board[move.to]
            val gain = if (victim == NO_PIECE) 0 else VALUE[pieceType(victim)]
            val promo = if (move.promotion == NO_PIECE) 0 else VALUE[move.promotion]
            gain + promo
        }

    /** Material plus a nudge towards the centre, from the moving side's point of view. */
    private fun evaluate(state: ChessState): Int {
        var score = 0
        state.board.forEachIndexed { square, piece ->
            if (piece == NO_PIECE) return@forEachIndexed
            val type = pieceType(piece)
            var worth = VALUE[type]
            if (type != KING) {
                val row = square / 8
                val col = square % 8
                val fromCentre = maxOf(
                    minOf(row, 7 - row).let { 3 - it },
                    minOf(col, 7 - col).let { 3 - it },
                )
                worth += CENTRE_BONUS[fromCentre.coerceIn(0, 3)]
            }
            score += if (isWhitePiece(piece)) worth else -worth
        }
        return if (state.whiteToMove) score else -score
    }
}
