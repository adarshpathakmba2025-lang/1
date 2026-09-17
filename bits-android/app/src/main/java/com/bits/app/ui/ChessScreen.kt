package com.bits.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.bits.app.games.BISHOP
import com.bits.app.games.Chess
import com.bits.app.games.ChessAi
import com.bits.app.games.ChessMove
import com.bits.app.games.ChessOutcome
import com.bits.app.games.ChessState
import com.bits.app.games.KING
import com.bits.app.games.KNIGHT
import com.bits.app.games.NO_PIECE
import com.bits.app.games.PAWN
import com.bits.app.games.QUEEN
import com.bits.app.games.ROOK
import com.bits.app.games.isWhitePiece
import com.bits.app.games.pieceType
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Chess, drawn as pixel art.
 *
 * The pieces are hand-drawn on a ten-by-ten grid rather than set in a font, which is the
 * only way to get shapes that are unmistakably a knight or a bishop at the size a phone
 * board gives each square, while still being built from visible square pixels. Each
 * sprite is outlined in the opposite shade so both colours read on both shades of square.
 */

private enum class ChessMode { PASS_AND_PLAY, VS_COMPUTER }

/** The four pieces a pawn may become. Shared by the real picker and its footer twin. */
private val PROMOTION_CHOICES = listOf(
    QUEEN to "Queen", ROOK to "Rook", BISHOP to "Bishop", KNIGHT to "Knight",
)

/** '#' is the piece, anything else is see-through. Ten rows of ten. */
private val PAWN_SPRITE = listOf(
    "..........",
    "...####...",
    "..######..",
    "..######..",
    "...####...",
    "...####...",
    "..######..",
    ".########.",
    "##########",
    "..........",
)

private val ROOK_SPRITE = listOf(
    "..........",
    ".##.##.##.",
    ".########.",
    "..######..",
    "..######..",
    "..######..",
    "..######..",
    ".########.",
    "##########",
    "..........",
)

private val KNIGHT_SPRITE = listOf(
    "..........",
    "....##....",
    "...####...",
    "..######..",
    ".#.#####..",
    "...#####..",
    "...#####..",
    "..######..",
    ".########.",
    "..........",
)

private val BISHOP_SPRITE = listOf(
    "..........",
    "....##....",
    "...####...",
    "..######..",
    "..##..##..",
    "..######..",
    "...####...",
    "..######..",
    ".########.",
    "..........",
)

private val QUEEN_SPRITE = listOf(
    "..........",
    ".#.#..#.#.",
    ".########.",
    "..######..",
    "..######..",
    "...####...",
    "..######..",
    ".########.",
    "##########",
    "..........",
)

private val KING_SPRITE = listOf(
    "..........",
    "....##....",
    "..######..",
    "....##....",
    "..######..",
    "..######..",
    "...####...",
    "..######..",
    ".########.",
    "..........",
)

private fun spriteFor(type: Int): List<String>? = when (type) {
    PAWN -> PAWN_SPRITE
    ROOK -> ROOK_SPRITE
    KNIGHT -> KNIGHT_SPRITE
    BISHOP -> BISHOP_SPRITE
    QUEEN -> QUEEN_SPRITE
    KING -> KING_SPRITE
    else -> null
}

private val LIGHT_SQUARE = Color(0xFF4A5C70)
private val DARK_SQUARE = Color(0xFF2B3948)
private val WHITE_FILL = Color(0xFFF2EFE5)
private val WHITE_EDGE = Color(0xFF16202B)
private val BLACK_FILL = Color(0xFF12191F)
private val BLACK_EDGE = Color(0xFF9AA6B4)

@Composable
fun ChessScreen(wins: Int, onWin: (Int) -> Unit, onBack: () -> Unit) {
    var mode by remember { mutableStateOf<ChessMode?>(null) }
    var state by remember { mutableStateOf(Chess.newGame()) }
    var selected by remember { mutableStateOf<Int?>(null) }
    /** A pawn has reached the last rank and is waiting to be told what to become. */
    var promoting by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var thinking by remember { mutableStateOf(false) }

    // Both of these walk the whole board, so they are worked out once per position
    // rather than on every recomposition - and the check flag is needed by all
    // sixty-four squares while drawing, which would otherwise mean sixty-four scans
    // of the board per frame.
    val outcome = remember(state) { Chess.outcome(state) }
    val inCheck = remember(state) { Chess.isInCheck(state, state.whiteToMove) }
    val finished = outcome != ChessOutcome.ONGOING
    // The human always has white, so the computer answers on black.
    val humansTurn = mode == ChessMode.PASS_AND_PLAY || state.whiteToMove

    // The moves available from whatever is selected, worked out once per selection.
    val options: List<ChessMove> = remember(state, selected) {
        val from = selected
        if (from == null) emptyList() else Chess.movesFrom(state, from)
    }

    fun reset() {
        state = Chess.newGame()
        selected = null
        promoting = null
    }

    fun play(move: ChessMove) {
        state = Chess.apply(state, move)
        selected = null
        promoting = null
    }

    // Winning against the computer is what counts, so a pass-and-play result isn't
    // recorded - there is no single player to credit it to.
    LaunchedEffect(outcome, mode) {
        if (mode == ChessMode.VS_COMPUTER && outcome == ChessOutcome.WHITE_MATES) onWin(wins + 1)
    }

    // The computer thinks on a background thread; searching on the main one would freeze
    // the board for as long as it took.
    LaunchedEffect(state, mode) {
        if (mode != ChessMode.VS_COMPUTER || state.whiteToMove || finished) return@LaunchedEffect
        thinking = true
        // A beat before replying, so a move doesn't appear in the same instant as yours.
        delay(220)
        val reply = withContext(Dispatchers.Default) { ChessAi.chooseMove(state) }
        thinking = false
        if (reply != null) state = Chess.apply(state, reply)
    }

    GameFrame(
        title = "Chess",
        score = state.fullmove,
        best = wins,
        onBack = onBack,
        leftLabel = "MOVE",
        rightLabel = "WINS",
        footer = {
            // Only the two states below - no piece picked up, or one picked up - change
            // on every single tap during ordinary play, so only those two are held to a
            // fixed height. Promotion and the end-of-game banner are rare or terminal;
            // letting the board shift a little for either of those is no annoyance, and
            // trying to reserve space for the promotion grid too is what caused the
            // previous bug: a hand-measured stand-in for its buttons came out a few dp
            // short of the real ones, and the real grid got squeezed to match, pushing
            // its bottom row past the edge of the screen.
            //
            // The fix here reuses one composable for both the invisible reservation and
            // the real content, so their sizes cannot drift apart the way hand-matching
            // two separate layouts did: the ghost simply calls it with the longest
            // strings play produces, which is guaranteed to be at least as tall as
            // whatever the real call ends up showing.
            Column(Modifier.fillMaxWidth()) {
                when {
                    mode == null -> Unit

                    promoting != null -> {
                        val (fromSquare, toSquare) = promoting!!
                        PromotionPicker { type ->
                            val move = Chess.movesFrom(state, fromSquare)
                                .firstOrNull { it.to == toSquare && it.promotion == type }
                            if (move != null) play(move)
                        }
                    }

                    finished -> GameOverBanner(outcomeText(outcome)) { reset() }

                    else -> {
                        val turn = if (state.whiteToMove) "WHITE" else "BLACK"
                        val check = if (inCheck) " \u00B7 CHECK" else ""
                        val turnLine = if (thinking) "THINKING" else "$turn TO MOVE$check"
                        val held = selected?.let { state.board[it] }
                        val heldLine = if (held != null && held != NO_PIECE) {
                            "${if (isWhitePiece(held)) "WHITE" else "BLACK"} ${pieceName(pieceType(held))}"
                        } else {
                            null
                        }
                        val hintLine = when {
                            heldLine == null -> "TAP A PIECE TO SEE ITS MOVES"
                            options.isEmpty() -> "NO LEGAL MOVES"
                            else -> "TAP A MARKED SQUARE"
                        }

                        Box {
                            // A hidden longest-case call establishes the height; the
                            // real call underneath is always the same size or smaller,
                            // so it can never be squeezed and never needs to overflow.
                            Box(Modifier.alpha(0f)) {
                                TurnStatus(
                                    turnLine = "BLACK TO MOVE \u00B7 CHECK",
                                    heldLine = "WHITE KNIGHT",
                                    hintLine = "TAP A MARKED SQUARE",
                                    checkColor = false,
                                )
                            }
                            Box(Modifier.matchParentSize()) {
                                TurnStatus(
                                    turnLine = turnLine,
                                    heldLine = heldLine,
                                    hintLine = hintLine,
                                    checkColor = check.isNotEmpty(),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        if (mode == null) {
            // Asked once, up front, because it decides who is sitting on the other side.
            PixelPanel(Modifier.fillMaxWidth()) {
                Text("HOW ARE YOU PLAYING?", style = BitsText.PixelHeading.copy(color = Arcade.Glow))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Full rules, both ways: castling, en passant, promotion, stalemate and the draws.",
                    style = BitsText.PixelBody,
                )
                Spacer(Modifier.height(14.dp))
                PixelButton("Vs computer") { mode = ChessMode.VS_COMPUTER; reset() }
                Spacer(Modifier.height(10.dp))
                PixelButton("Pass and play") { mode = ChessMode.PASS_AND_PLAY; reset() }
            }
            return@GameFrame
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Arcade.Border)
                .padding(3.dp)
                .pointerInput(state, selected, humansTurn, finished, promoting) {
                    detectTapGestures { offset ->
                        if (finished || !humansTurn || promoting != null) return@detectTapGestures
                        val cell = size.width / 8f
                        val col = (offset.x / cell).toInt().coerceIn(0, 7)
                        val row = (offset.y / cell).toInt().coerceIn(0, 7)
                        val square = row * 8 + col

                        val matching = options.filter { it.to == square }
                        when {
                            // A destination with several moves on it can only be a
                            // promotion, so ask which piece before playing it.
                            matching.size > 1 -> promoting = selected!! to square
                            matching.size == 1 -> play(matching.first())
                            // Otherwise treat the tap as picking a piece up, or putting
                            // it down again.
                            else -> {
                                val piece = state.board[square]
                                selected = if (
                                    piece != NO_PIECE && isWhitePiece(piece) == state.whiteToMove && square != selected
                                ) square else null
                            }
                        }
                    }
                }
        ) {
            val cell = size.width / 8f
            val pixel = cell / 10f
            val targets = options.map { it.to }.toSet()

            for (square in 0 until 64) {
                val row = square / 8
                val col = square % 8
                val left = col * cell
                val top = row * cell

                // The board itself.
                drawRect(
                    color = if ((row + col) % 2 == 0) LIGHT_SQUARE else DARK_SQUARE,
                    topLeft = Offset(left, top),
                    size = Size(cell, cell),
                )

                // Where the last move came from and went to, so it is easy to follow
                // the computer's reply.
                state.lastMove?.let { last ->
                    if (square == last.from || square == last.to) {
                        drawRect(Color(0x33F2B544), Offset(left, top), Size(cell, cell))
                    }
                }

                // A king in check gets a warning wash.
                val piece = state.board[square]
                if (inCheck && pieceType(piece) == KING && piece != NO_PIECE &&
                    isWhitePiece(piece) == state.whiteToMove
                ) {
                    drawRect(Color(0x66E8907F), Offset(left, top), Size(cell, cell))
                }

                if (square == selected) {
                    // A square border, drawn as four bars so it stays pixel-sharp.
                    val thick = pixel
                    drawRect(Arcade.Glow, Offset(left, top), Size(cell, thick))
                    drawRect(Arcade.Glow, Offset(left, top + cell - thick), Size(cell, thick))
                    drawRect(Arcade.Glow, Offset(left, top), Size(thick, cell))
                    drawRect(Arcade.Glow, Offset(left + cell - thick, top), Size(thick, cell))
                }

                // The piece.
                val sprite = spriteFor(pieceType(piece))
                if (sprite != null) {
                    val white = isWhitePiece(piece)
                    drawSprite(
                        sprite = sprite,
                        left = left,
                        top = top,
                        pixel = pixel,
                        fill = if (white) WHITE_FILL else BLACK_FILL,
                        edge = if (white) WHITE_EDGE else BLACK_EDGE,
                    )
                }

                // Where the selected piece may go: a dot on an empty square, corner
                // marks on one holding a piece to take, so a capture is obvious even
                // with the target drawn on top of it.
                if (square in targets) {
                    if (piece == NO_PIECE) {
                        val dot = pixel * 2.4f
                        drawRect(
                            color = Arcade.Glow,
                            topLeft = Offset(left + (cell - dot) / 2f, top + (cell - dot) / 2f),
                            size = Size(dot, dot),
                        )
                    } else {
                        val mark = pixel * 2.6f
                        val thick = pixel
                        listOf(
                            Offset(left, top) to Size(mark, thick),
                            Offset(left, top) to Size(thick, mark),
                            Offset(left + cell - mark, top) to Size(mark, thick),
                            Offset(left + cell - thick, top) to Size(thick, mark),
                            Offset(left, top + cell - thick) to Size(mark, thick),
                            Offset(left, top + cell - mark) to Size(thick, mark),
                            Offset(left + cell - mark, top + cell - thick) to Size(mark, thick),
                            Offset(left + cell - thick, top + cell - mark) to Size(thick, mark),
                        ).forEach { (corner, size) -> drawRect(Arcade.Glow, corner, size) }
                    }
                }
            }
        }
    }
}

/**
 * Paints one sprite, outline first then body.
 *
 * The outline is every empty cell that touches the piece, which is what lets a white
 * piece sit on a light square and a black piece on a dark one without either
 * disappearing into it.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSprite(
    sprite: List<String>,
    left: Float,
    top: Float,
    pixel: Float,
    fill: Color,
    edge: Color,
) {
    fun filled(row: Int, col: Int): Boolean =
        row in sprite.indices && col in sprite[row].indices && sprite[row][col] == '#'

    // Overdrawn very slightly so neighbouring pixels meet with no hairline between them.
    val step = pixel * 1.02f

    sprite.indices.forEach { row ->
        sprite[row].indices.forEach { col ->
            if (filled(row, col)) return@forEach
            val touches = filled(row - 1, col) || filled(row + 1, col) ||
                filled(row, col - 1) || filled(row, col + 1)
            if (touches) {
                drawRect(edge, Offset(left + col * pixel, top + row * pixel), Size(step, step))
            }
        }
    }
    sprite.indices.forEach { row ->
        sprite[row].indices.forEach { col ->
            if (filled(row, col)) {
                drawRect(fill, Offset(left + col * pixel, top + row * pixel), Size(step, step))
            }
        }
    }
}

@Composable
private fun TurnStatus(turnLine: String, heldLine: String?, hintLine: String, checkColor: Boolean) {
    // Pinned to one line each: on an ordinary phone none of these strings are long
    // enough to wrap anyway, but a very large accessibility font size could force a
    // wrap, and a wrap is exactly the kind of height change this whole footer exists to
    // rule out. Truncating instead is the safer failure.
    Text(
        text = turnLine,
        style = BitsText.PixelBody.copy(color = if (checkColor) Arcade.Glow else BitsColors.Ink),
        maxLines = 1,
        softWrap = false,
    )
    Spacer(Modifier.height(6.dp))
    if (heldLine != null) {
        // Say out loud what has been picked up. The sprites are small, so naming the
        // piece saves squinting at it - and it confirms the tap landed on the square
        // that was meant.
        Text(text = heldLine, style = BitsText.PixelBody.copy(color = Arcade.Glow), maxLines = 1, softWrap = false)
        Spacer(Modifier.height(4.dp))
    }
    Text(text = hintLine, style = BitsText.PixelBody.copy(color = BitsColors.Muted), maxLines = 1, softWrap = false)
}

/** The live promotion grid: two rows of two, each button calling [onChoose]. */
@Composable
private fun PromotionPicker(onChoose: (Int) -> Unit) {
    Text("PROMOTE TO", style = BitsText.PixelBody.copy(color = Arcade.Glow))
    Spacer(Modifier.height(8.dp))
    // Two rows of two rather than one row of four. At nine points the pixel face makes
    // "KNIGHT" about eighty-six dp wide, which does not fit in a quarter of a phone's
    // width, so it broke across two lines. Half the width fits every name easily, and
    // sharing each row equally makes all four the same size.
    PROMOTION_CHOICES.chunked(2).forEach { pair ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pair.forEach { (type, label) ->
                PixelButton(label = label, modifier = Modifier.weight(1f), fillWidth = true) {
                    onChoose(type)
                }
            }
        }
    }
}

private fun pieceName(type: Int): String = when (type) {
    PAWN -> "PAWN"
    KNIGHT -> "KNIGHT"
    BISHOP -> "BISHOP"
    ROOK -> "ROOK"
    QUEEN -> "QUEEN"
    KING -> "KING"
    else -> ""
}

private fun outcomeText(outcome: ChessOutcome): String = when (outcome) {
    ChessOutcome.WHITE_MATES -> "White wins"
    ChessOutcome.BLACK_MATES -> "Black wins"
    ChessOutcome.STALEMATE -> "Stalemate"
    ChessOutcome.FIFTY_MOVE -> "Draw, fifty moves"
    ChessOutcome.REPETITION -> "Draw, repetition"
    ChessOutcome.DEAD_POSITION -> "Draw, no mate possible"
    ChessOutcome.ONGOING -> ""
}
