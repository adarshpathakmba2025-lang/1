package com.bits.app.games

/**
 * Full chess rules: every piece, castling, en passant, promotion, check, checkmate,
 * stalemate, the fifty-move rule, threefold repetition and dead positions.
 *
 * Nothing here is cut down. The board is a plain list of 64 squares and every state is
 * immutable, which keeps it easy to reason about and easy to test - the move generator
 * is checked against the published move counts for known positions, which is the
 * standard way to prove a chess engine's rules are right rather than merely plausible.
 *
 * Square 0 is a8 and square 63 is h1, so the list reads in the same order the board is
 * drawn, top-left to bottom-right, with no flipping in the UI.
 */

const val NO_PIECE = 0
const val PAWN = 1
const val KNIGHT = 2
const val BISHOP = 3
const val ROOK = 4
const val QUEEN = 5
const val KING = 6

/** Colour lives in bit 3, so a black piece is its type plus eight. */
const val BLACK_FLAG = 8

fun pieceType(piece: Int): Int = piece and 7
fun isWhitePiece(piece: Int): Boolean = piece != NO_PIECE && piece < BLACK_FLAG
fun isBlackPiece(piece: Int): Boolean = piece >= BLACK_FLAG
fun pieceOf(type: Int, white: Boolean): Int = if (white) type else type or BLACK_FLAG

/** Castling rights, one bit each. */
const val WHITE_KING_SIDE = 1
const val WHITE_QUEEN_SIDE = 2
const val BLACK_KING_SIDE = 4
const val BLACK_QUEEN_SIDE = 8

data class ChessMove(
    val from: Int,
    val to: Int,
    /** The piece type a pawn becomes, or [NO_PIECE] for every other move. */
    val promotion: Int = NO_PIECE,
    val castle: Boolean = false,
    val enPassant: Boolean = false,
)

enum class ChessOutcome { ONGOING, WHITE_MATES, BLACK_MATES, STALEMATE, FIFTY_MOVE, REPETITION, DEAD_POSITION }

data class ChessState(
    val board: List<Int>,
    val whiteToMove: Boolean,
    val castling: Int,
    /** The square a pawn could be captured on this move, or -1. */
    val enPassant: Int,
    /** Plies since the last capture or pawn move, for the fifty-move rule. */
    val halfmoveClock: Int,
    val fullmove: Int,
    /** Position keys seen so far, including the current one, for repetition. */
    val seen: List<Long>,
    /** The move just played, so the board can highlight it. */
    val lastMove: ChessMove? = null,
)

object Chess {

    private fun rowOf(square: Int) = square / 8
    private fun colOf(square: Int) = square % 8
    private fun squareAt(row: Int, col: Int) = row * 8 + col
    private fun onBoard(row: Int, col: Int) = row in 0..7 && col in 0..7

    private val KNIGHT_STEPS = arrayOf(
        -2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1,
    )
    private val KING_STEPS = arrayOf(
        -1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1,
    )
    private val ROOK_RAYS = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val BISHOP_RAYS = arrayOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

    fun newGame(): ChessState {
        val board = MutableList(64) { NO_PIECE }
        val backRank = listOf(ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK)
        backRank.forEachIndexed { col, type ->
            board[squareAt(0, col)] = pieceOf(type, white = false)
            board[squareAt(7, col)] = pieceOf(type, white = true)
        }
        for (col in 0..7) {
            board[squareAt(1, col)] = pieceOf(PAWN, white = false)
            board[squareAt(6, col)] = pieceOf(PAWN, white = true)
        }
        val state = ChessState(
            board = board.toList(),
            whiteToMove = true,
            castling = WHITE_KING_SIDE or WHITE_QUEEN_SIDE or BLACK_KING_SIDE or BLACK_QUEEN_SIDE,
            enPassant = -1,
            halfmoveClock = 0,
            fullmove = 1,
            seen = emptyList(),
        )
        return state.copy(seen = listOf(keyOf(state)))
    }

    /**
     * A compact fingerprint of everything that makes two positions the same for the
     * repetition rule: the pieces, whose turn it is, castling rights and the en passant
     * square. Deliberately not the move clocks, which are allowed to differ.
     */
    fun keyOf(state: ChessState): Long {
        var key = 1125899906842597L
        state.board.forEach { key = key * 1099511628211L + (it + 1) }
        key = key * 31L + (if (state.whiteToMove) 1L else 2L)
        key = key * 31L + state.castling
        key = key * 31L + (state.enPassant + 2)
        return key
    }

    fun findKing(board: List<Int>, white: Boolean): Int =
        board.indexOfFirst { pieceType(it) == KING && isWhitePiece(it) == white && it != NO_PIECE }

    /** Whether [square] is attacked by a piece of the given colour. */
    fun isAttacked(board: List<Int>, square: Int, byWhite: Boolean): Boolean {
        val row = rowOf(square)
        val col = colOf(square)

        // Pawns. A white pawn sits one rank below the squares it attacks, because white
        // moves up the board towards row 0.
        val pawnRow = if (byWhite) row + 1 else row - 1
        for (dc in intArrayOf(-1, 1)) {
            if (onBoard(pawnRow, col + dc)) {
                val piece = board[squareAt(pawnRow, col + dc)]
                if (pieceType(piece) == PAWN && isWhitePiece(piece) == byWhite && piece != NO_PIECE) return true
            }
        }

        KNIGHT_STEPS.forEach { (dr, dc) ->
            if (onBoard(row + dr, col + dc)) {
                val piece = board[squareAt(row + dr, col + dc)]
                if (pieceType(piece) == KNIGHT && isWhitePiece(piece) == byWhite && piece != NO_PIECE) return true
            }
        }

        KING_STEPS.forEach { (dr, dc) ->
            if (onBoard(row + dr, col + dc)) {
                val piece = board[squareAt(row + dr, col + dc)]
                if (pieceType(piece) == KING && isWhitePiece(piece) == byWhite && piece != NO_PIECE) return true
            }
        }

        // Sliding pieces: walk each ray until something blocks it.
        fun rayHits(rays: Array<Pair<Int, Int>>, wanted: Int): Boolean {
            rays.forEach { (dr, dc) ->
                var r = row + dr
                var c = col + dc
                while (onBoard(r, c)) {
                    val piece = board[squareAt(r, c)]
                    if (piece != NO_PIECE) {
                        if (isWhitePiece(piece) == byWhite) {
                            val type = pieceType(piece)
                            if (type == wanted || type == QUEEN) return true
                        }
                        break
                    }
                    r += dr
                    c += dc
                }
            }
            return false
        }
        if (rayHits(ROOK_RAYS, ROOK)) return true
        if (rayHits(BISHOP_RAYS, BISHOP)) return true
        return false
    }

    fun isInCheck(state: ChessState, white: Boolean): Boolean {
        val king = findKing(state.board, white)
        if (king < 0) return false
        return isAttacked(state.board, king, byWhite = !white)
    }

    /** Every move the side to move may legally play. */
    fun legalMoves(state: ChessState): List<ChessMove> =
        pseudoMoves(state).filter { move ->
            val next = applyRaw(state, move)
            // A move is only legal if it doesn't leave your own king attacked.
            !isAttacked(next.board, findKing(next.board, state.whiteToMove), byWhite = !state.whiteToMove)
        }

    /** The legal moves that start on one square. Used to light up a tapped piece. */
    fun movesFrom(state: ChessState, square: Int): List<ChessMove> =
        legalMoves(state).filter { it.from == square }

    private fun pseudoMoves(state: ChessState): List<ChessMove> {
        val white = state.whiteToMove
        val board = state.board
        val moves = ArrayList<ChessMove>(48)

        board.forEachIndexed { square, piece ->
            if (piece == NO_PIECE || isWhitePiece(piece) != white) return@forEachIndexed
            val row = rowOf(square)
            val col = colOf(square)

            fun tryStep(r: Int, c: Int) {
                if (!onBoard(r, c)) return
                val target = board[squareAt(r, c)]
                if (target != NO_PIECE && isWhitePiece(target) == white) return
                moves += ChessMove(square, squareAt(r, c))
            }

            fun tryRay(rays: Array<Pair<Int, Int>>) {
                rays.forEach { (dr, dc) ->
                    var r = row + dr
                    var c = col + dc
                    while (onBoard(r, c)) {
                        val target = board[squareAt(r, c)]
                        if (target == NO_PIECE) {
                            moves += ChessMove(square, squareAt(r, c))
                        } else {
                            if (isWhitePiece(target) != white) moves += ChessMove(square, squareAt(r, c))
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }

            when (pieceType(piece)) {
                PAWN -> {
                    // White marches towards row 0, black towards row 7.
                    val forward = if (white) -1 else 1
                    val startRow = if (white) 6 else 1
                    val lastRow = if (white) 0 else 7

                    val oneUp = row + forward
                    if (onBoard(oneUp, col) && board[squareAt(oneUp, col)] == NO_PIECE) {
                        addPawnMove(moves, square, squareAt(oneUp, col), oneUp == lastRow)
                        val twoUp = row + forward * 2
                        if (row == startRow && board[squareAt(twoUp, col)] == NO_PIECE) {
                            moves += ChessMove(square, squareAt(twoUp, col))
                        }
                    }
                    for (dc in intArrayOf(-1, 1)) {
                        val r = oneUp
                        val c = col + dc
                        if (!onBoard(r, c)) continue
                        val target = board[squareAt(r, c)]
                        if (target != NO_PIECE && isWhitePiece(target) != white) {
                            addPawnMove(moves, square, squareAt(r, c), r == lastRow)
                        } else if (target == NO_PIECE && squareAt(r, c) == state.enPassant) {
                            moves += ChessMove(square, squareAt(r, c), enPassant = true)
                        }
                    }
                }

                KNIGHT -> KNIGHT_STEPS.forEach { (dr, dc) -> tryStep(row + dr, col + dc) }
                BISHOP -> tryRay(BISHOP_RAYS)
                ROOK -> tryRay(ROOK_RAYS)
                QUEEN -> { tryRay(ROOK_RAYS); tryRay(BISHOP_RAYS) }

                KING -> {
                    KING_STEPS.forEach { (dr, dc) -> tryStep(row + dr, col + dc) }
                    addCastles(state, moves, square, white)
                }
            }
        }
        return moves
    }

    private fun addPawnMove(moves: MutableList<ChessMove>, from: Int, to: Int, promoting: Boolean) {
        if (promoting) {
            // All four choices, so the player picks rather than always getting a queen.
            intArrayOf(QUEEN, ROOK, BISHOP, KNIGHT).forEach { moves += ChessMove(from, to, promotion = it) }
        } else {
            moves += ChessMove(from, to)
        }
    }

    private fun addCastles(state: ChessState, moves: MutableList<ChessMove>, kingSquare: Int, white: Boolean) {
        val homeRow = if (white) 7 else 0
        if (kingSquare != squareAt(homeRow, 4)) return
        val board = state.board
        val enemyIsWhite = !white

        // Castling out of check is never allowed, and the king may not pass through an
        // attacked square either. The square it lands on is covered by the usual
        // legality filter, but these two are not, so they are checked here.
        if (isAttacked(board, kingSquare, enemyIsWhite)) return

        val kingSide = if (white) WHITE_KING_SIDE else BLACK_KING_SIDE
        if (state.castling and kingSide != 0 &&
            board[squareAt(homeRow, 5)] == NO_PIECE &&
            board[squareAt(homeRow, 6)] == NO_PIECE &&
            pieceType(board[squareAt(homeRow, 7)]) == ROOK &&
            isWhitePiece(board[squareAt(homeRow, 7)]) == white &&
            !isAttacked(board, squareAt(homeRow, 5), enemyIsWhite)
        ) {
            moves += ChessMove(kingSquare, squareAt(homeRow, 6), castle = true)
        }

        val queenSide = if (white) WHITE_QUEEN_SIDE else BLACK_QUEEN_SIDE
        if (state.castling and queenSide != 0 &&
            board[squareAt(homeRow, 1)] == NO_PIECE &&
            board[squareAt(homeRow, 2)] == NO_PIECE &&
            board[squareAt(homeRow, 3)] == NO_PIECE &&
            pieceType(board[squareAt(homeRow, 0)]) == ROOK &&
            isWhitePiece(board[squareAt(homeRow, 0)]) == white &&
            !isAttacked(board, squareAt(homeRow, 3), enemyIsWhite)
        ) {
            moves += ChessMove(kingSquare, squareAt(homeRow, 2), castle = true)
        }
    }

    /** Plays a move without recording it for repetition. Used inside the generator. */
    private fun applyRaw(state: ChessState, move: ChessMove): ChessState {
        val board = state.board.toMutableList()
        val piece = board[move.from]
        val type = pieceType(piece)
        val white = state.whiteToMove
        val captured = board[move.to]

        board[move.from] = NO_PIECE
        board[move.to] = if (move.promotion != NO_PIECE) pieceOf(move.promotion, white) else piece

        if (move.enPassant) {
            // The captured pawn is beside the arrival square, not on it.
            val victimRow = rowOf(move.from)
            board[squareAt(victimRow, colOf(move.to))] = NO_PIECE
        }

        if (move.castle) {
            val homeRow = rowOf(move.from)
            if (colOf(move.to) == 6) {
                board[squareAt(homeRow, 5)] = board[squareAt(homeRow, 7)]
                board[squareAt(homeRow, 7)] = NO_PIECE
            } else {
                board[squareAt(homeRow, 3)] = board[squareAt(homeRow, 0)]
                board[squareAt(homeRow, 0)] = NO_PIECE
            }
        }

        // Moving a king or a rook gives up the matching rights; so does capturing a
        // rook on its home square, which is easy to miss.
        var castling = state.castling
        if (type == KING) {
            castling = castling and if (white) (BLACK_KING_SIDE or BLACK_QUEEN_SIDE)
            else (WHITE_KING_SIDE or WHITE_QUEEN_SIDE)
        }
        castling = castling and cornerMask(move.from)
        castling = castling and cornerMask(move.to)

        // Only a two-square pawn push leaves a square open to en passant.
        val enPassant = if (type == PAWN && kotlin.math.abs(rowOf(move.to) - rowOf(move.from)) == 2) {
            squareAt((rowOf(move.from) + rowOf(move.to)) / 2, colOf(move.from))
        } else {
            -1
        }

        val reset = type == PAWN || captured != NO_PIECE || move.enPassant
        return state.copy(
            board = board.toList(),
            whiteToMove = !white,
            castling = castling,
            enPassant = enPassant,
            halfmoveClock = if (reset) 0 else state.halfmoveClock + 1,
            fullmove = if (white) state.fullmove else state.fullmove + 1,
            lastMove = move,
        )
    }

    /** Clears whichever castling right lives on this corner, if any. */
    private fun cornerMask(square: Int): Int = when (square) {
        squareAt(7, 0) -> WHITE_QUEEN_SIDE.inv()
        squareAt(7, 7) -> WHITE_KING_SIDE.inv()
        squareAt(0, 0) -> BLACK_QUEEN_SIDE.inv()
        squareAt(0, 7) -> BLACK_KING_SIDE.inv()
        else -> -1
    }

    /** Plays a move and records the new position for the repetition rule. */
    fun apply(state: ChessState, move: ChessMove): ChessState {
        val next = applyRaw(state, move)
        // A capture or pawn move makes every earlier position unreachable, so the
        // history can be dropped at that point rather than growing for the whole game.
        val history = if (next.halfmoveClock == 0) emptyList() else next.seen
        return next.copy(seen = history + keyOf(next))
    }

    /**
     * Whether either side could still deliver mate. Covers the cases that come up in a
     * real game: bare kings, a lone minor piece, and king and bishop against king and
     * bishop on the same colour squares.
     */
    fun isDeadPosition(board: List<Int>): Boolean {
        val pieces = board.filter { it != NO_PIECE }
        if (pieces.any { pieceType(it) in intArrayOf(PAWN, ROOK, QUEEN) }) return false

        val whiteMinors = board.withIndex().filter { (_, p) -> isWhitePiece(p) && pieceType(p) in intArrayOf(KNIGHT, BISHOP) }
        val blackMinors = board.withIndex().filter { (_, p) -> isBlackPiece(p) && pieceType(p) in intArrayOf(KNIGHT, BISHOP) }

        if (whiteMinors.isEmpty() && blackMinors.isEmpty()) return true
        if (whiteMinors.size + blackMinors.size == 1) return true
        if (whiteMinors.size == 1 && blackMinors.size == 1 &&
            pieceType(whiteMinors[0].value) == BISHOP && pieceType(blackMinors[0].value) == BISHOP
        ) {
            // Bishops that can never meet cannot force mate between them.
            fun shade(square: Int) = (square / 8 + square % 8) % 2
            return shade(whiteMinors[0].index) == shade(blackMinors[0].index)
        }
        return false
    }

    fun outcome(state: ChessState): ChessOutcome {
        if (legalMoves(state).isEmpty()) {
            return when {
                !isInCheck(state, state.whiteToMove) -> ChessOutcome.STALEMATE
                state.whiteToMove -> ChessOutcome.BLACK_MATES
                else -> ChessOutcome.WHITE_MATES
            }
        }
        if (state.halfmoveClock >= 100) return ChessOutcome.FIFTY_MOVE
        if (isDeadPosition(state.board)) return ChessOutcome.DEAD_POSITION
        val current = state.seen.lastOrNull()
        if (current != null && state.seen.count { it == current } >= 3) return ChessOutcome.REPETITION
        return ChessOutcome.ONGOING
    }

    /** Counts the leaves of the move tree. Used only to verify the rules. */
    fun perft(state: ChessState, depth: Int): Long {
        if (depth == 0) return 1L
        val moves = legalMoves(state)
        if (depth == 1) return moves.size.toLong()
        var total = 0L
        moves.forEach { total += perft(applyRaw(state, it), depth - 1) }
        return total
    }

    /** Reads a position from FEN. Only used by the tests. */
    fun fromFen(fen: String): ChessState {
        val parts = fen.trim().split(" ")
        val board = MutableList(64) { NO_PIECE }
        var square = 0
        parts[0].forEach { ch ->
            when {
                ch == '/' -> Unit
                ch.isDigit() -> square += ch - '0'
                else -> {
                    val white = ch.isUpperCase()
                    val type = when (ch.lowercaseChar()) {
                        'p' -> PAWN; 'n' -> KNIGHT; 'b' -> BISHOP
                        'r' -> ROOK; 'q' -> QUEEN; 'k' -> KING
                        else -> NO_PIECE
                    }
                    board[square] = pieceOf(type, white)
                    square++
                }
            }
        }
        var castling = 0
        parts.getOrElse(2) { "-" }.forEach {
            when (it) {
                'K' -> castling = castling or WHITE_KING_SIDE
                'Q' -> castling = castling or WHITE_QUEEN_SIDE
                'k' -> castling = castling or BLACK_KING_SIDE
                'q' -> castling = castling or BLACK_QUEEN_SIDE
            }
        }
        val epText = parts.getOrElse(3) { "-" }
        val enPassant = if (epText == "-" || epText.length < 2) -1 else {
            squareAt(8 - (epText[1] - '0'), epText[0] - 'a')
        }
        val state = ChessState(
            board = board.toList(),
            whiteToMove = parts.getOrElse(1) { "w" } == "w",
            castling = castling,
            enPassant = enPassant,
            halfmoveClock = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0,
            fullmove = parts.getOrElse(5) { "1" }.toIntOrNull() ?: 1,
            seen = emptyList(),
        )
        return state.copy(seen = listOf(keyOf(state)))
    }

    /** Square names, for the move list. */
    fun squareName(square: Int): String = "${'a' + colOf(square)}${8 - rowOf(square)}"
}
