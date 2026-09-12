package com.bits.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.bits.app.games.MemoryMatch
import com.bits.app.games.Snake
import com.bits.app.games.SnakeState
import com.bits.app.games.TicTacToe
import com.bits.app.games.Wordle
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.delay
import kotlin.math.abs

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
    footer: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Arcade.Screen)) {
        ArcadeHeader(title = title, onBack = onBack)
        ScoreBar(score = score, best = best, modifier = Modifier.padding(horizontal = 14.dp))
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

    LaunchedEffect(state.dead) {
        if (state.dead) {
            onScore(latest.score)
            return@LaunchedEffect
        }
        while (true) {
            // Speeds up gently as the snake grows, but never past a playable pace.
            delay((260L - latest.score * 6L).coerceAtLeast(110L))
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
                    state = Snake.newGame()
                }
            } else {
                Text("SWIPE TO TURN", style = BitsText.PixelBody)
            }
        },
    ) {
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
            state.body.forEachIndexed { index, point ->
                drawRect(
                    color = if (index == 0) Color(0xFFBDF0C4) else Color(0xFF7FD68A),
                    topLeft = Offset(point.x * cell, point.y * cell),
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

/* ---------------------------- Memory match ---------------------------- */

private val memoryColors = listOf(
    Color(0xFFF2B544), Color(0xFF7FD68A), Color(0xFF5BD3D3), Color(0xFF9DBBFF),
    Color(0xFFCFA6FF), Color(0xFFF3A6B8), Color(0xFFFF9E6B), Color(0xFFC9B688),
)

@Composable
fun MemoryScreen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit) {
    var cards by remember { mutableStateOf(MemoryMatch.newGame()) }
    var moves by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val complete = MemoryMatch.isComplete(cards)

    // Fewer moves is better, so the score counts down from a generous ceiling.
    fun scoreFor(moveCount: Int) = (100 - moveCount * 2).coerceAtLeast(10)

    LaunchedEffect(complete) { if (complete) onScore(scoreFor(moves)) }

    LaunchedEffect(cards) {
        if (MemoryMatch.faceUpUnmatched(cards).size == 2) {
            busy = true
            delay(650)
            cards = MemoryMatch.resolve(cards)
            moves += 1
            busy = false
        }
    }

    GameFrame(
        title = "Memory Match",
        score = if (complete) scoreFor(moves) else moves,
        best = best,
        onBack = onBack,
        footer = {
            if (complete) {
                GameOverBanner("Cleared in $moves moves") {
                    cards = MemoryMatch.newGame()
                    moves = 0
                }
            } else {
                Text("FIND EVERY PAIR", style = BitsText.PixelBody)
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().aspectRatio(0.82f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cards.chunked(4).forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { card ->
                        MemoryTile(
                            card = card,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            onClick = { if (!busy) cards = MemoryMatch.flip(cards, card.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryTile(card: MemoryCard, modifier: Modifier, onClick: () -> Unit) {
    val revealed = card.faceUp || card.matched
    val alpha by animateFloatAsState(if (card.matched) 0.55f else 1f, label = "matched")
    Box(
        modifier
            .background(Arcade.Border)
            .padding(2.dp)
            .background(if (revealed) memoryColors[card.symbol].copy(alpha = alpha) else Arcade.Panel)
            .clickable(enabled = !revealed, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!revealed) {
            Text("?", style = BitsText.PixelHeading.copy(color = BitsColors.Muted))
        }
    }
}

/* ------------------------------ X and O ------------------------------ */

@Composable
fun TicTacToeScreen(onBack: () -> Unit) {
    var board by remember { mutableStateOf(TicTacToe.empty()) }
    var playerTurn by remember { mutableStateOf(true) }
    var wins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }
    val winner = TicTacToe.winner(board)
    val over = TicTacToe.isOver(board)
    val line = TicTacToe.winningLine(board)

    LaunchedEffect(playerTurn, board) {
        if (!playerTurn && !over) {
            delay(350)
            val move = TicTacToe.bestMove(board, TicTacToe.O)
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
                winner == TicTacToe.X -> "You win!"
                winner == TicTacToe.O -> "Computer wins"
                over -> "Draw"
                else -> null
            }
            if (message != null) {
                GameOverBanner(message) {
                    board = TicTacToe.empty()
                    playerTurn = true
                }
            } else {
                Text(if (playerTurn) "YOUR TURN \u2014 X" else "THINKING\u2026", style = BitsText.PixelBody)
            }
        },
    ) {
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
                                .clickable(enabled = playerTurn && value == TicTacToe.EMPTY && !over) {
                                    board = TicTacToe.play(board, index, TicTacToe.X)
                                    playerTurn = false
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

/* ---------------------------- Word guess ---------------------------- */

@Composable
fun WordleScreen(best: Int, onScore: (Int) -> Unit, onBack: () -> Unit) {
    var answer by remember { mutableStateOf(Wordle.randomAnswer()) }
    var guesses by remember { mutableStateOf(listOf<String>()) }
    var current by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var streak by remember { mutableIntStateOf(0) }

    val solved = guesses.lastOrNull() == answer
    val out = guesses.size >= Wordle.MAX_GUESSES && !solved

    fun submit() {
        val guess = current.uppercase()
        if (guess.length < Wordle.LENGTH) return
        if (!Wordle.isAcceptable(guess)) {
            message = "Not in word list"
            return
        }
        guesses = guesses + guess
        current = ""
        message = null
        if (guess == answer) {
            streak += 1
            onScore(streak)
        }
    }

    fun restart() {
        answer = Wordle.randomAnswer()
        guesses = emptyList()
        current = ""
        message = null
        if (out) streak = 0
    }

    GameFrame(
        title = "Word Guess",
        score = streak,
        best = best,
        onBack = onBack,
        footer = {
            when {
                solved -> GameOverBanner("Got it!", ::restart)
                out -> GameOverBanner("It was $answer", ::restart)
                else -> Text(message?.uppercase() ?: "TAP LETTERS TO GUESS", style = BitsText.PixelBody)
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowIndex in 0 until Wordle.MAX_GUESSES) {
                    val guess = guesses.getOrNull(rowIndex)
                    val isCurrent = rowIndex == guesses.size && !solved && !out
                    val marks = guess?.let { Wordle.mark(it, answer) }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        for (i in 0 until Wordle.LENGTH) {
                            val letter = when {
                                guess != null -> guess[i].toString()
                                isCurrent && i < current.length -> current[i].uppercase()
                                else -> ""
                            }
                            val fill = when (marks?.getOrNull(i)) {
                                LetterMark.CORRECT -> Color(0xFF7FD68A)
                                LetterMark.PRESENT -> Arcade.Glow
                                LetterMark.ABSENT -> Color(0xFF39434F)
                                null -> Arcade.Panel
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(Arcade.Border)
                                    .padding(2.dp)
                                    .background(fill),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    letter,
                                    style = BitsText.PixelHeading.copy(
                                        color = if (marks == null) BitsColors.Ink else Arcade.Screen,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (!solved && !out) {
                LetterKeyboard(
                    guesses = guesses,
                    answer = answer,
                    onLetter = { if (current.length < Wordle.LENGTH) current += it },
                    onDelete = { current = current.dropLast(1) },
                    onEnter = ::submit,
                )
            }
        }
    }
}

@Composable
private fun LetterKeyboard(
    guesses: List<String>,
    answer: String,
    onLetter: (Char) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
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

    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        rows.forEachIndexed { index, row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (index == 2) {
                    KeyCap("DEL", Modifier.weight(1.6f), onClick = onDelete)
                }
                row.forEach { letter ->
                    val fill = when (states[letter]) {
                        LetterMark.CORRECT -> Color(0xFF7FD68A)
                        LetterMark.PRESENT -> Arcade.Glow
                        LetterMark.ABSENT -> Color(0xFF232C36)
                        null -> Arcade.Panel
                    }
                    KeyCap(
                        label = letter.toString(),
                        modifier = Modifier.weight(1f),
                        fill = fill,
                        ink = if (states[letter] == LetterMark.CORRECT || states[letter] == LetterMark.PRESENT)
                            Arcade.Screen else BitsColors.Ink,
                        onClick = { onLetter(letter) },
                    )
                }
                if (index == 2) {
                    KeyCap("GO", Modifier.weight(1.6f), fill = Arcade.Glow, ink = Arcade.Screen, onClick = onEnter)
                }
            }
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
            .height(42.dp)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = BitsText.PixelBody.copy(color = ink))
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
                val lowerTop = (pipe.gapTop + FlappyState.GAP) * h
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
