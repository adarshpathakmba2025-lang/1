package com.bits.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.bits.app.games.Board
import com.bits.app.games.Direction
import com.bits.app.games.FlappyBird
import com.bits.app.games.FlappyState
import com.bits.app.games.Game2048
import com.bits.app.games.LetterMark
import com.bits.app.games.MemoryCard
import com.bits.app.games.MemoryDeck
import com.bits.app.games.MemoryMatch
import com.bits.app.games.Snake
import com.bits.app.games.SnakeState
import com.bits.app.games.TicTacToe
import com.bits.app.games.Wordle
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Detects a swipe in one of four directions. Shared by 2048 and Snake. */
private fun Modifier.swipeable(onSwipe: (Direction) -> Unit): Modifier = pointerInput(Unit) {
    var total = Offset.Zero
    detectDragGestures(
        onDragStart = { total = Offset.Zero },
        onDrag = { change, amount ->
            total += amount
            change.consume()
        },
        onDragEnd = {
            val dx = total.x
            val dy = total.y
            if (maxOf(abs(dx), abs(dy)) > 48f) {
                onSwipe(
                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (dy > 0) Direction.DOWN else Direction.UP
                    }
                )
            }
        },
    )
}

@Composable
private fun GameFrame(
    title: String,
    score: Int,
    best: Int,
    onBack: () -> Unit,
    leftLabel: String = "SCORE",
    rightLabel: String = "BEST",
    onResetBest: (() -> Unit)? = null,
    footer: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Arcade.Screen)) {
        ArcadeHeader(title = title, onBack = onBack)
        ScoreBar(
            score = score,
            best = best,
            modifier = Modifier.padding(horizontal = 14.dp),
            leftLabel = leftLabel,
            rightLabel = rightLabel,
            onResetBest = onResetBest,
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) { content() }
        Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 18.dp)) { footer() }
    }
}

@Composable
private fun GameOverBanner(message: String, onRestart: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(message.uppercase(), style = BitsText.PixelBody.copy(color = Arcade.Glow), modifier = Modifier.weight(1f))
        PixelButton("Again", onClick = onRestart)
    }
}

/* ------------------------------- 2048 ------------------------------- */

@Composable
fun Game2048Screen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit) {
    var board by remember { mutableStateOf(Game2048.newGame()) }
    var score by remember { mutableIntStateOf(0) }
    val gameOver = Game2048.isGameOver(board)

    val latestScore by rememberUpdatedState(score)
    LaunchedEffect(gameOver) { if (gameOver) onScore(latestScore) }

    fun move(direction: Direction) {
        if (gameOver) return
        val result = Game2048.move(board, direction)
        if (result.moved) {
            board = Game2048.spawnTile(result.board)
            score += result.gained
        }
    }

    fun restart() {
        onScore(score)
        board = Game2048.newGame()
        score = 0
    }

    GameFrame(
        title = "2048",
        score = score,
        best = best,
        onBack = { onScore(score); onBack() },
        footer = {
            if (gameOver) GameOverBanner("No moves left", ::restart)
            else Text("SWIPE TO SLIDE", style = BitsText.PixelBody)
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Arcade.Border)
                .padding(3.dp)
                .background(Arcade.Panel)
                .padding(6.dp)
                .swipeable(::move),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            board.forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { value -> Tile2048(value, Modifier.weight(1f).fillMaxSize()) }
                }
            }
        }
    }
}

@Composable
private fun Tile2048(value: Int, modifier: Modifier = Modifier) {
    val background = when (value) {
        0 -> Color(0xFF1B2733)
        2 -> Color(0xFF35506B)
        4 -> Color(0xFF3F6285)
        8 -> Color(0xFFF2B544)
        16 -> Color(0xFFEC9F3C)
        32 -> Color(0xFFE58A33)
        64 -> Color(0xFFDD7430)
        128 -> Color(0xFF7FD68A)
        256 -> Color(0xFF5BD3D3)
        512 -> Color(0xFF9DBBFF)
        1024 -> Color(0xFFCFA6FF)
        else -> Color(0xFFF3A6B8)
    }
    val ink = if (value in listOf(0, 2, 4)) BitsColors.Ink else Arcade.Screen
    Box(modifier.background(background), contentAlignment = Alignment.Center) {
        if (value != 0) {
            Text(
                text = value.toString(),
                style = BitsText.PixelBody.copy(color = ink),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/* ------------------------------- Snake ------------------------------- */

@Composable
fun SnakeScreen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit) {
    var state by remember { mutableStateOf(Snake.newGame()) }
    val latest by rememberUpdatedState(state)
    // Kept purely for the glide animation: where every segment was one tick ago, so the
    // canvas can draw a smooth slide from there to where it is now rather than a jump.
    var previousBody by remember { mutableStateOf(state.body) }
    var tickStart by remember { mutableStateOf(0L) }
    var tickLength by remember { mutableStateOf(220L) }

    LaunchedEffect(state.dead) {
        if (state.dead) {
            onScore(latest.score)
            return@LaunchedEffect
        }
        while (true) {
            // Speeds up gently as the snake grows, but never past a playable pace.
            val interval = (260L - latest.score * 6L).coerceAtLeast(110L)
            tickStart = System.currentTimeMillis()

            // Rather than sleeping the whole interval, poll in short slices so a queued
            // turn can cut the wait short. Without this a swipe could sit unhandled for
            // most of a tick, which is what made turning feel unresponsive.
            // A turn may only cut the wait short once a decent part of the tick has run.
            // Without that floor, swiping repeatedly would shorten every tick in a row and
            // let the snake race forward far faster than its normal pace.
            val reactFloor = maxOf(MIN_REACT, (interval * REACT_FLOOR_FRACTION).toLong())
            var waited = 0L
            while (waited < interval) {
                delay(TICK_SLICE)
                waited = System.currentTimeMillis() - tickStart
                if (latest.pendingDirection != null && waited >= reactFloor) break
            }

            // The glide is measured against however long this tick actually ran, so a
            // tick cut short by a turn animates over that shorter span instead of
            // appearing to lag behind.
            tickLength = waited.coerceAtLeast(MIN_REACT)
            previousBody = state.body
            state = Snake.step(state)
            if (latest.dead) break
        }
    }

    GameFrame(
        title = "Snake",
        score = state.score,
        best = best,
        onBack = { onScore(state.score); onBack() },
        footer = {
            if (state.dead) {
                GameOverBanner("Game over") {
                    onScore(state.score)
                    // Built into a local first: reading back through the delegated
                    // `state` right after assigning it can't be smart-cast.
                    val fresh = Snake.newGame()
                    state = fresh
                    previousBody = fresh.body
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("SWIPE OR USE THE PAD", style = BitsText.PixelBody)
                    Spacer(Modifier.height(10.dp))
                    PixelDpad(onMove = { state = Snake.turn(state, it) })
                }
            }
        },
    ) {
        // Redraws every animation frame, gliding each segment from its previous cell to
        // its current one, so the snake reads as continuous motion rather than a series
        // of teleports between ticks.
        var frameNow by remember { mutableStateOf(System.currentTimeMillis()) }
        LaunchedEffect(state.dead) {
            while (!state.dead) {
                withFrameMillis { frameNow = it }
            }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Arcade.Border)
                .padding(3.dp)
                .background(Color(0xFF0E1922))
                .swipeable { state = Snake.turn(state, it) }
        ) {
            val cell = size.width / Snake.GRID
            val elapsed = frameNow - tickStart
            // Completing the slide in a fraction of the tick keeps the motion smooth but
            // still snappy; stretching it across the whole tick is what felt sluggish.
            val glideSpan = (tickLength * GLIDE_FRACTION).coerceAtLeast(1f)
            val progress = (elapsed.toFloat() / glideSpan).coerceIn(0f, 1f)

            state.body.forEachIndexed { index, point ->
                // A body can grow between ticks (after eating), so any segment with no
                // matching previous one just holds still rather than gliding from nowhere.
                val from = previousBody.getOrNull(index) ?: point
                val x = lerp(from.x.toFloat(), point.x.toFloat(), progress)
                val y = lerp(from.y.toFloat(), point.y.toFloat(), progress)
                drawRect(
                    color = if (index == 0) Color(0xFFBDF0C4) else Color(0xFF7FD68A),
                    topLeft = Offset(x * cell, y * cell),
                    size = Size(cell - 2f, cell - 2f),
                )
            }
            drawRect(
                color = Color(0xFFE8907F),
                topLeft = Offset(state.food.x * cell, state.food.y * cell),
                size = Size(cell - 2f, cell - 2f),
            )
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** How often the Snake loop checks whether a turn is waiting. */
private const val TICK_SLICE = 8L

/**
 * The shortest a tick may be cut to when a turn is queued. Low enough that a swipe feels
 * immediate, high enough that frantic swiping can't race the snake forward.
 */
private const val MIN_REACT = 55L

/** Portion of a tick the glide animation occupies. */
private const val GLIDE_FRACTION = 0.55f

/**
 * Smallest share of a tick that must elapse before a queued turn can cut it short. Caps
 * how much faster than normal the snake can ever be made to move by swiping constantly.
 */
private const val REACT_FLOOR_FRACTION = 0.45f

/* ---------------------------- Memory match ---------------------------- */

private val memoryPalette = listOf(
    Color(0xFFF2B544), Color(0xFF7FD68A), Color(0xFF5BD3D3), Color(0xFF9DBBFF),
    Color(0xFFCFA6FF), Color(0xFFF3A6B8), Color(0xFFFF9E6B), Color(0xFFC9B688),
    Color(0xFF8FD694), Color(0xFFE8907F),
)

private val fruitGlyphs = listOf("\uD83C\uDF4E","\uD83C\uDF4C","\uD83C\uDF47","\uD83C\uDF53","\uD83C\uDF4A","\uD83C\uDF49","\uD83C\uDF52","\uD83C\uDF51","\uD83C\uDF50","\uD83E\uDD5D")
private val foodGlyphs = listOf("\uD83C\uDF55","\uD83C\uDF54","\uD83C\uDF5C","\uD83C\uDF69","\uD83C\uDF71","\uD83E\uDDC7","\uD83C\uDF2E","\uD83C\uDF67","\uD83E\uDD68","\uD83C\uDF7F")
private val animalGlyphs = listOf("\uD83D\uDC31","\uD83D\uDC36","\uD83E\uDD8A","\uD83D\uDC3C","\uD83D\uDC38","\uD83E\uDD89","\uD83D\uDC22","\uD83E\uDD80","\uD83D\uDC1D","\uD83E\uDD84")
private val flagGlyphs = listOf("\uD83C\uDDEE\uD83C\uDDF3","\uD83C\uDDEF\uD83C\uDDF5","\uD83C\uDDE7\uD83C\uDDF7","\uD83C\uDDE8\uD83C\uDDE6","\uD83C\uDDEB\uD83C\uDDF7","\uD83C\uDDE9\uD83C\uDDEA","\uD83C\uDDEE\uD83C\uDDF9","\uD83C\uDDF0\uD83C\uDDF7","\uD83C\uDDF2\uD83C\uDDFD","\uD83C\uDDE6\uD83C\uDDFA")
private val spaceGlyphs = listOf("\uD83D\uDE80","\uD83E\uDE90","\u2B50","\uD83C\uDF0D","\uD83C\uDF19","\u2604\uFE0F","\uD83D\uDEF8","\uD83C\uDF0C","\uD83D\uDD2D","\uD83D\uDC7D")
private val weatherGlyphs = listOf("\u2600\uFE0F","\u26C5","\u2601\uFE0F","\uD83C\uDF27\uFE0F","\u26C8\uFE0F","\u2744\uFE0F","\uD83C\uDF2A\uFE0F","\uD83C\uDF08","\uD83D\uDD25","\uD83D\uDCA7")
private val codeLabels = listOf("2U","9A","7K","4Z","6M","3Q","8R","5T","1B","0X")

/** Every glyph deck, used by the jumbled round. */
private val glyphDecks = listOf(fruitGlyphs, foodGlyphs, animalGlyphs, flagGlyphs, spaceGlyphs, weatherGlyphs)

/** Picks a face for a card. Jumbled rounds vary the deck per symbol, deterministically. */
private fun glyphFor(deck: MemoryDeck, symbol: Int): String? = when (deck) {
    MemoryDeck.FRUIT -> fruitGlyphs[symbol % fruitGlyphs.size]
    MemoryDeck.FOOD -> foodGlyphs[symbol % foodGlyphs.size]
    MemoryDeck.ANIMALS -> animalGlyphs[symbol % animalGlyphs.size]
    MemoryDeck.FLAGS -> flagGlyphs[symbol % flagGlyphs.size]
    MemoryDeck.SPACE -> spaceGlyphs[symbol % spaceGlyphs.size]
    MemoryDeck.WEATHER -> weatherGlyphs[symbol % weatherGlyphs.size]
    MemoryDeck.CODES -> codeLabels[symbol % codeLabels.size]
    // Same symbol always maps to the same deck, so a pair still matches visually.
    MemoryDeck.JUMBLED -> glyphDecks[symbol % glyphDecks.size][(symbol * 3) % 10]
    else -> null
}

@Composable
fun MemoryScreen(
    bestTries: Int,
    onCleared: (Int) -> Unit,
    onResetBest: () -> Unit,
    onBack: () -> Unit,
) {
    var level by remember { mutableIntStateOf(1) }
    var deal by remember { mutableStateOf(MemoryMatch.newLevel(1)) }
    var cards by remember { mutableStateOf(deal.cards) }
    var moves by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }
    val complete = MemoryMatch.isComplete(cards)

    LaunchedEffect(complete) {
        if (complete) {
            celebrate = true
            // Flips taken, so fewer is better; the bar records the lowest ever.
            onCleared(moves)
        }
    }

    LaunchedEffect(cards) {
        if (MemoryMatch.faceUpUnmatched(cards).size == 2) {
            busy = true
            delay(650)
            cards = MemoryMatch.resolve(cards)
            moves += 1
            busy = false
        }
    }

    fun start(newLevel: Int) {
        val dealt = MemoryMatch.newLevel(newLevel)
        level = newLevel
        deal = dealt
        cards = dealt.cards
        moves = 0
        celebrate = false
    }

    Box {
        GameFrame(
            title = "Memory Match",
            score = moves,
            best = bestTries,
            leftLabel = "TRIES",
            rightLabel = "BEST",
            onResetBest = onResetBest,
            onBack = onBack,
            footer = {
                if (complete) {
                    GameOverBanner("Cleared in $moves tries") { start(level + 1) }
                } else {
                    Text(deal.deck.label.uppercase(), style = BitsText.PixelBody)
                }
            },
        ) {
            Column(
                Modifier.fillMaxWidth().aspectRatio(if (deal.pairs > 8) 0.78f else 0.86f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                cards.chunked(4).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { card ->
                            MemoryTile(
                                card = card,
                                deck = deal.deck,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                onClick = { if (!busy) cards = MemoryMatch.flip(cards, card.id) },
                            )
                        }
                        // Keeps the last row aligned when it isn't full.
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        if (celebrate) {
            ConfettiBurst(onFinished = { celebrate = false })
        }
    }
}

@Composable
private fun MemoryTile(card: MemoryCard, deck: MemoryDeck, modifier: Modifier, onClick: () -> Unit) {
    val revealed = card.faceUp || card.matched
    val alpha by animateFloatAsState(if (card.matched) 0.5f else 1f, label = "matched")
    val scale by animateFloatAsState(if (revealed) 1f else 0.97f, label = "flip")
    val colour = memoryPalette[card.symbol % memoryPalette.size]

    // Colour decks paint the whole tile; the rest show a symbol on a neutral face.
    val face = if (deck == MemoryDeck.COLORS) colour.copy(alpha = alpha) else Color(0xFF22303D)

    Box(
        modifier
            .scale(scale)
            .background(Arcade.Border)
            .padding(2.dp)
            .background(if (revealed) face else Arcade.Panel)
            .clickable(enabled = !revealed, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = glyphFor(deck, card.symbol)
        when {
            !revealed -> Text("?", style = BitsText.PixelHeading.copy(color = BitsColors.Muted))
            deck == MemoryDeck.COLORS -> Unit
            deck == MemoryDeck.NUMBERS -> Text(
                (card.symbol + 1).toString(),
                style = BitsText.PixelHeading.copy(color = colour.copy(alpha = alpha)),
            )
            deck == MemoryDeck.LETTERS -> Text(
                ('A' + card.symbol).toString(),
                style = BitsText.PixelHeading.copy(color = colour.copy(alpha = alpha)),
            )
            deck == MemoryDeck.SHAPES -> PixelShape(index = card.symbol, colour = colour.copy(alpha = alpha))
            deck == MemoryDeck.CODES -> Text(
                glyph.orEmpty(),
                style = BitsText.PixelHeading.copy(color = colour.copy(alpha = alpha)),
            )
            // Emoji decks get their own larger size; the pixel font sizes are too small
            // for these to read at a glance.
            glyph != null -> Text(
                glyph,
                style = BitsText.MemoryGlyph.copy(color = Color.White.copy(alpha = alpha)),
            )
            else -> PixelShape(index = card.symbol, colour = colour.copy(alpha = alpha))
        }
    }
}

/** Blocky shapes drawn from squares, matching the pixel look of this section. */
@Composable
private fun PixelShape(index: Int, colour: Color) {
    val grid = when (index % 5) {
        0 -> listOf("01110", "11111", "11111", "11111", "01110") // blob
        1 -> listOf("00100", "01110", "11111", "01110", "00100") // diamond
        2 -> listOf("11111", "10001", "10001", "10001", "11111") // frame
        3 -> listOf("10001", "01010", "00100", "01010", "10001") // cross
        else -> listOf("00100", "00100", "11111", "00100", "00100") // plus
    }
    Canvas(Modifier.fillMaxSize().padding(9.dp)) {
        val cell = minOf(size.width, size.height) / 5f
        val offsetX = (size.width - cell * 5) / 2f
        val offsetY = (size.height - cell * 5) / 2f
        grid.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                if (ch == '1') {
                    drawRect(
                        color = colour,
                        topLeft = Offset(offsetX + col * cell, offsetY + row * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}

/* ------------------------------ X and O ------------------------------ */

@Composable
fun TicTacToeScreen(onBack: () -> Unit) {
    var twoPlayer by remember { mutableStateOf(false) }
    var board by remember { mutableStateOf(TicTacToe.empty()) }
    var playerTurn by remember { mutableStateOf(true) }
    var wins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }
    val random = remember { Random(System.currentTimeMillis()) }
    // Re-rolled each round: usually sharp, but every so often it plays loose
    // enough to lose, so the game doesn't feel hopeless.
    var mistakeChance by remember { mutableFloatStateOf(0f) }
    val winner = TicTacToe.winner(board)
    val over = TicTacToe.isOver(board)
    val line = TicTacToe.winningLine(board)

    fun reset() {
        board = TicTacToe.empty()
        playerTurn = true
        mistakeChance = if (random.nextFloat() < 0.30f) 0.35f else 0f
    }

    LaunchedEffect(Unit) { reset() }

    LaunchedEffect(playerTurn, board, twoPlayer) {
        if (!twoPlayer && !playerTurn && !over) {
            delay(350)
            val move = TicTacToe.chooseMove(board, TicTacToe.O, mistakeChance, random)
            if (move >= 0) board = TicTacToe.play(board, move, TicTacToe.O)
            playerTurn = true
        }
    }

    LaunchedEffect(over) {
        if (over) {
            if (winner == TicTacToe.X) wins += 1
            if (winner == TicTacToe.EMPTY) draws += 1
        }
    }

    GameFrame(
        title = "X and O",
        score = wins,
        best = draws,
        onBack = onBack,
        footer = {
            val message = when {
                winner == TicTacToe.X -> if (twoPlayer) "X wins!" else "You win!"
                winner == TicTacToe.O -> if (twoPlayer) "O wins!" else "Computer wins"
                over -> "Draw"
                else -> null
            }
            if (message != null) {
                GameOverBanner(message) { reset() }
            } else {
                Text(
                    text = when {
                        twoPlayer && playerTurn -> "X\u2019S TURN"
                        twoPlayer -> "O\u2019S TURN"
                        playerTurn -> "YOUR TURN \u2014 X"
                        else -> "THINKING\u2026"
                    },
                    style = BitsText.PixelBody,
                )
            }
        },
    ) {
        Column(Modifier.fillMaxWidth()) {
            // One compact switch keeps the board the hero; no extra card for this.
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(false to "VS PHONE", true to "2 PLAYERS").forEach { (value, label) ->
                    val active = twoPlayer == value
                    Text(
                        text = label,
                        style = BitsText.PixelBody.copy(color = if (active) Arcade.Screen else BitsColors.Muted),
                        modifier = Modifier
                            .weight(1f)
                            .background(if (active) Arcade.Glow else Arcade.Panel)
                            .clickable {
                                if (twoPlayer != value) {
                                    twoPlayer = value
                                    reset()
                                }
                            }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (twoPlayer) {
                Text(
                    text = "GO ON, DARE THE PERSON NEXT TO YOU.",
                    style = BitsText.PixelBody.copy(color = Arcade.Glow),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

        Column(
            Modifier.fillMaxWidth().aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (row in 0 until 3) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        val value = board[index]
                        val highlight = line?.contains(index) == true
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (highlight) Arcade.Glow else Arcade.Border)
                                .padding(2.dp)
                                .background(Arcade.Panel)
                                .clickable(enabled = value == TicTacToe.EMPTY && !over && (twoPlayer || playerTurn)) {
                                    val mark = if (twoPlayer && !playerTurn) TicTacToe.O else TicTacToe.X
                                    board = TicTacToe.play(board, index, mark)
                                    playerTurn = !playerTurn
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (value != TicTacToe.EMPTY) {
                                Text(
                                    text = if (value == TicTacToe.X) "X" else "O",
                                    style = BitsText.PixelTitle.copy(
                                        color = if (value == TicTacToe.X) Arcade.Glow else Color(0xFF5BD3D3),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/* ---------------------------- Word guess ---------------------------- */

@Composable
fun WordleScreen(
    dayIndex: Long,
    guesses: List<String>,
    streak: Int,
    best: Int,
    hintPoints: Int,
    purchased: Set<Int>,
    attempts: Int,
    onGuess: (String, Boolean) -> Unit,
    onBuyReveal: (Int) -> Unit,
    onHint: (Int) -> Unit,
    onRejected: () -> Unit,
    onBack: () -> Unit,
) {
    val puzzle = remember(dayIndex) { Wordle.puzzleFor(dayIndex) }
    val answer = puzzle.answer
    var typed by remember(dayIndex) { mutableStateOf("") }
    var message by remember(dayIndex) { mutableStateOf<String?>(null) }
    var shake by remember(dayIndex) { mutableIntStateOf(0) }
    var celebrate by remember { mutableStateOf(false) }
    // Full-screen and unmissable on arrival; reopened later from the button in the header.
    var showHelp by remember(dayIndex) { mutableStateOf(guesses.isEmpty()) }
    var pickingReveal by remember { mutableStateOf(false) }

    val solved = guesses.lastOrNull() == answer
    val slots = Wordle.editableIndices(puzzle, purchased)
    val shown = Wordle.shownIndices(puzzle, purchased)

    LaunchedEffect(slots.size) { if (typed.length > slots.size) typed = typed.take(slots.size) }

    fun submit() {
        if (solved || !Wordle.isComplete(typed, puzzle, purchased)) return
        val guess = Wordle.assembleGuess(typed, puzzle, purchased)
        if (!Wordle.isAcceptable(guess)) {
            message = "Not in word list"
            shake += 1
            typed = ""
            // Still counts as a go, so the final tally matches what the player actually did.
            onRejected()
            return
        }
        message = null
        typed = ""
        val won = guess == answer
        if (won) celebrate = true
        onGuess(guess, won)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Arcade.Screen)) {
            ArcadeHeader(
                title = "Word Guess",
                onBack = onBack,
                trailing = {
                    Text(
                        text = "INSTRUCTIONS",
                        style = BitsText.PixelBody.copy(color = Arcade.Glow),
                        modifier = Modifier
                            .background(Arcade.Border)
                            .padding(1.dp)
                            .background(Arcade.Panel)
                            .clickable { showHelp = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                },
            )
            ScoreBar(score = streak, best = best, modifier = Modifier.padding(horizontal = 14.dp))

            HintBar(
                points = hintPoints,
                canPeek = !solved && hintPoints >= Wordle.PEEK_COST &&
                    Wordle.nextHintIndex(puzzle, guesses, purchased) != null,
                canReveal = !solved && hintPoints >= Wordle.REVEAL_COST &&
                    Wordle.revealableIndices(puzzle, purchased).isNotEmpty(),
                picking = pickingReveal,
                onPeek = {
                    // Fills in the next unknown box rather than printing a line of text,
                    // and because each hint consumes a box, no two hints repeat.
                    val index = Wordle.nextHintIndex(puzzle, guesses, purchased)
                    if (index == null) message = "Nothing left to hint"
                    else {
                        onHint(index)
                        message = null
                    }
                },
                onStartReveal = { pickingReveal = !pickingReveal },
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )

            // Exactly the rows that have been used, plus the one being typed. No empty
            // rows implying tries that don't exist.
            val rowCount = (guesses.size + if (solved) 0 else 1).coerceAtLeast(1)
            val boardScroll = rememberScrollState()
            LaunchedEffect(guesses.size) { boardScroll.animateScrollTo(boardScroll.maxValue) }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(boardScroll),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (rowIndex in 0 until rowCount) {
                    val guess = guesses.getOrNull(rowIndex)
                    val isCurrent = rowIndex == guesses.size && !solved
                    val marks = guess?.let { Wordle.mark(it, answer) }
                    val nudge by animateFloatAsState(
                        targetValue = shake.toFloat(),
                        animationSpec = tween(90),
                        label = "shake",
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .offset(x = if (isCurrent) (sin(nudge * 12f) * 5f).dp else 0.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (i in 0 until Wordle.LENGTH) {
                            val isShown = i in shown
                            val isBought = i in purchased
                            val slotIndex = slots.indexOf(i)
                            // Free and bought letters show on every not-yet-played row.
                            val letter = when {
                                guess != null -> guess[i].toString()
                                isShown -> answer[i].toString()
                                isCurrent && slotIndex in typed.indices -> typed[slotIndex].toString()
                                else -> ""
                            }
                            val fill = when (marks?.getOrNull(i)) {
                                LetterMark.CORRECT -> Color(0xFF7FD68A)
                                LetterMark.PRESENT -> Arcade.Glow
                                LetterMark.ABSENT -> Color(0xFF2B333C)
                                null -> Color(0xFF121A22)
                            }
                            val revealTarget = pickingReveal && rowIndex == guesses.size && !isShown
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        when {
                                            revealTarget -> Color(0xFF7FD68A)
                                            else -> Color(0xFF2A3A46)
                                        }
                                    )
                                    .padding(2.dp)
                                    .background(fill)
                                    .then(
                                        if (revealTarget) Modifier.clickable {
                                            onBuyReveal(i)
                                            pickingReveal = false
                                            message = null
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = letter,
                                    style = BitsText.WordCell.copy(
                                        color = when {
                                            marks != null -> Arcade.Screen
                                            isBought -> Color(0xFF7FD68A)
                                            else -> BitsColors.Ink
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = when {
                    solved -> "SOLVED IN $attempts ${if (attempts == 1) "TRY" else "TRIES"}"
                    pickingReveal -> "TAP A BOX TO REVEAL IT"
                    else -> message?.uppercase() ?: "+1 HINT POINT PER ROW"
                },
                style = BitsText.PixelBody.copy(
                    color = if (solved) Arcade.Glow else BitsColors.Muted,
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )

            if (!solved) {
                LetterKeyboard(
                    guesses = guesses,
                    answer = answer,
                    onLetter = { if (typed.length < slots.size) typed += it },
                    onDelete = { typed = typed.dropLast(1) },
                    onEnter = ::submit,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 14.dp),
                )
            } else {
                Text(
                    text = "COME BACK TOMORROW FOR A NEW WORD",
                    style = BitsText.PixelBody.copy(color = BitsColors.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                )
            }
        }

        if (celebrate) {
            ConfettiBurst(onFinished = { celebrate = false })
        }

        if (showHelp) {
            WordleInstructions(onDismiss = { showHelp = false })
        }
    }
}

/** Covers the whole screen on arrival, so the rules can't be scrolled past or missed. */
@Composable
private fun WordleInstructions(onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Arcade.Screen)
            .padding(22.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("HOW TO PLAY", style = BitsText.PixelTitle.copy(color = Arcade.Glow))
        Box(Modifier.padding(top = 14.dp, bottom = 20.dp).fillMaxWidth().height(3.dp).background(Arcade.Border))

        val rules = listOf(
            "ONE WORD A DAY" to "Everyone gets the same five-letter word. A new one lands at midnight.",
            "NO TRY LIMIT" to "Keep guessing until you get it. At the end you'll see how many tries it took.",
            "FREE LETTERS" to "A letter or two is filled in for you. Those boxes can't be typed over.",
            "EARN HINTS" to "Every completed row earns one hint point.",
            "SPEND HINTS" to "1 point tells you a letter that's in the word. 5 points reveals a whole box you choose.",
            "COLOURS" to "Green means right letter, right spot. Amber means right letter, wrong spot.",
        )
        rules.forEach { (title, body) ->
            Column(Modifier.padding(bottom = 16.dp)) {
                Text(title, style = BitsText.PixelBody.copy(color = BitsColors.Ink))
                Spacer(Modifier.height(6.dp))
                Text(body, style = BitsText.PixelBody.copy(color = BitsColors.Muted))
            }
        }

        Spacer(Modifier.weight(1f))
        PixelButton(
            label = "Let's play",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onDismiss,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Shows the hint balance and what it can buy. */
@Composable
private fun HintBar(
    points: Int,
    canPeek: Boolean,
    canReveal: Boolean,
    picking: Boolean,
    onPeek: () -> Unit,
    onStartReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "HINTS $points",
            style = BitsText.PixelBody.copy(color = if (points > 0) Arcade.Glow else BitsColors.Muted),
            modifier = Modifier.weight(1f),
        )
        HintChip("LETTER \u00b7 ${Wordle.PEEK_COST}", canPeek, false, onPeek)
        Spacer(Modifier.width(6.dp))
        HintChip("REVEAL \u00b7 ${Wordle.REVEAL_COST}", canReveal, picking, onStartReveal)
    }
}

@Composable
private fun HintChip(label: String, enabled: Boolean, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = BitsText.PixelBody.copy(
            color = when {
                active -> Arcade.Screen
                enabled -> BitsColors.Ink
                else -> BitsColors.Muted.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier
            .background(if (active) Arcade.Glow else Arcade.Panel)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun LetterKeyboard(
    guesses: List<String>,
    answer: String,
    onLetter: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Best-known state per letter, so the keyboard reflects what's been learned.
    val states = mutableMapOf<Char, LetterMark>()
    guesses.forEach { guess ->
        Wordle.mark(guess, answer).forEachIndexed { i, mark ->
            val letter = guess[i]
            val existing = states[letter]
            val better = existing == null ||
                (existing == LetterMark.ABSENT) ||
                (existing == LetterMark.PRESENT && mark == LetterMark.CORRECT)
            if (better) states[letter] = mark
        }
    }

    fun fillFor(letter: Char): Color = when (states[letter]) {
        LetterMark.CORRECT -> Color(0xFF7FD68A)
        LetterMark.PRESENT -> Arcade.Glow
        LetterMark.ABSENT -> Color(0xFF232C36)
        null -> Arcade.Panel
    }
    fun inkFor(letter: Char): Color =
        if (states[letter] == LetterMark.CORRECT || states[letter] == LetterMark.PRESENT)
            Arcade.Screen else BitsColors.Ink

    // Ten key-widths per row on every row, so no row ends up with fatter keys than another.
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Row 1: ten letters.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            rows[0].forEach { KeyCap(it.toString(), Modifier.weight(1f), fillFor(it), inkFor(it)) { onLetter(it) } }
        }
        // Row 2: nine letters, half a key of padding each side to keep it centred.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.weight(0.5f))
            rows[1].forEach { KeyCap(it.toString(), Modifier.weight(1f), fillFor(it), inkFor(it)) { onLetter(it) } }
            Spacer(Modifier.weight(0.5f))
        }
        // Row 3: DEL + seven letters + GO, the two wide keys totalling three key-widths.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyCap("DEL", Modifier.weight(1.5f), Arcade.Panel, BitsColors.Muted, onClick = onDelete)
            rows[2].forEach { KeyCap(it.toString(), Modifier.weight(1f), fillFor(it), inkFor(it)) { onLetter(it) } }
            KeyCap("GO", Modifier.weight(1.5f), Arcade.Glow, Arcade.Screen, onClick = onEnter)
        }
    }
}

@Composable
private fun KeyCap(
    label: String,
    modifier: Modifier = Modifier,
    fill: Color = Arcade.Panel,
    ink: Color = BitsColors.Ink,
    onClick: () -> Unit,
) {
    Box(
        modifier
            // A fixed height on every key means rows can never differ in size.
            .height(46.dp)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = BitsText.KeyCap.copy(color = ink), maxLines = 1)
    }
}

/* ------------------------------ Flappy ------------------------------ */

@Composable
fun FlappyScreen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit) {
    var state by remember { mutableStateOf(FlappyBird.newGame()) }
    val latest by rememberUpdatedState(state)

    LaunchedEffect(state.started, state.dead) {
        if (!state.started || state.dead) {
            if (state.dead) onScore(latest.score)
            return@LaunchedEffect
        }
        while (true) {
            delay(16)
            state = FlappyBird.step(state)
            if (latest.dead) break
        }
    }

    GameFrame(
        title = "Flappy",
        score = state.score,
        best = best,
        onBack = { onScore(state.score); onBack() },
        footer = {
            when {
                state.dead -> GameOverBanner("Ouch") {
                    onScore(state.score)
                    state = FlappyBird.newGame()
                }
                !state.started -> Text("TAP TO START", style = BitsText.PixelBody)
                else -> Text("TAP TO FLAP", style = BitsText.PixelBody)
            }
        },
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .background(Arcade.Border)
                .padding(3.dp)
                .background(Color(0xFF101D28))
                .pointerInput(Unit) {
                    detectTapGestures { if (!latest.dead) state = FlappyBird.flap(state) }
                }
        ) {
            val w = size.width
            val h = size.height
            state.pipes.forEach { pipe ->
                val x = pipe.x * w
                val pipeWidth = FlappyState.PIPE_WIDTH * w
                drawRect(Color(0xFF7FD68A), Offset(x, 0f), Size(pipeWidth, pipe.gapTop * h))
                // Drawn with this pipe's own stored gap, so what's visible always matches
                // exactly what can be collided with.
                val lowerTop = (pipe.gapTop + pipe.gap) * h
                drawRect(Color(0xFF7FD68A), Offset(x, lowerTop), Size(pipeWidth, h - lowerTop))
            }
            val birdSize = FlappyState.BIRD_SIZE * h
            drawRect(
                color = Arcade.Glow,
                topLeft = Offset(FlappyState.BIRD_X * w, state.birdY * h),
                size = Size(birdSize, birdSize),
            )
        }
    }
}
