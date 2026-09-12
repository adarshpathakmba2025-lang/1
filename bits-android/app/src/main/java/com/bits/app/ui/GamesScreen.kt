package com.bits.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bits.app.R
import com.bits.app.games.Direction
import com.bits.app.games.Game2048
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlin.math.abs

private data class GameEntry(
    val title: String,
    val subtitle: String,
    val free: Boolean,
)

// "2048" is the one real game so far. The other two are brainstormed candidates —
// placeholders until one is actually picked and built, so they're clearly unplayable for now.
private val gameEntries = listOf(
    GameEntry("2048", "Slide and merge. Free to play.", free = true),
    GameEntry("Snake", "Idea for Pro — not built yet.", free = false),
    GameEntry("Memory Match", "Idea for Pro — not built yet.", free = false),
)

@Composable
fun GamesHubScreen(isPro: Boolean, onBack: () -> Unit, onPlay2048: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Games", onBack = onBack)
        Text(
            text = "A little something for a break. One game's ready to play now, a couple more are on the way.",
            style = BitsText.Small,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(2.dp)) }
            items(gameEntries) { entry ->
                val locked = !entry.free && !isPro
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BitsColors.Panel)
                        .clickable(enabled = entry.free) { if (entry.free) onPlay2048() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.title, style = BitsText.Subtitle)
                        Text(entry.subtitle, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (locked) {
                        Icon(Icons.Filled.Lock, contentDescription = "Unlocks with Pro", tint = BitsColors.Muted)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BitsColors.Ink)
        }
        Text(title, style = BitsText.Brand, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun Game2048Screen(onBack: () -> Unit) {
    var board by remember { mutableStateOf(Game2048.newGame()) }
    var score by remember { mutableIntStateOf(0) }
    var celebrated by remember { mutableStateOf(false) }
    val gameOver = Game2048.isGameOver(board)
    val won = Game2048.hasWon(board)

    fun attempt(direction: Direction) {
        if (gameOver) return
        val result = Game2048.move(board, direction)
        if (result.moved) {
            board = Game2048.spawnTile(result.board)
            score += result.gained
        }
    }

    fun restart() {
        board = Game2048.newGame()
        score = 0
        celebrated = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "2048", onBack = onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Score: $score", style = BitsText.Subtitle)
            TextAction("Restart", BitsColors.Amber) { restart() }
        }

        if (won && !celebrated) {
            Text(
                text = "You hit 2048! Keep sliding for a higher score, or restart any time.",
                style = BitsText.Small.copy(color = BitsColors.Amber),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Board2048(board = board, onSwipe = ::attempt, modifier = Modifier.padding(16.dp))

        if (gameOver) {
            Text(
                text = "No more moves. Give it another go?",
                style = BitsText.Body.copy(color = BitsColors.Danger),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            FilledAction("Restart", modifier = Modifier.padding(16.dp)) { restart() }
        } else {
            DirectionPad(onMove = ::attempt, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun Board2048(board: com.bits.app.games.Board, onSwipe: (Direction) -> Unit, modifier: Modifier = Modifier) {
    var dragTotal by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(BitsColors.PanelBase)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragTotal = Offset.Zero },
                    onDrag = { change, amount ->
                        dragTotal += amount
                        change.consume()
                    },
                    onDragEnd = {
                        val dx = dragTotal.x
                        val dy = dragTotal.y
                        if (maxOf(abs(dx), abs(dy)) > 48f) {
                            val direction = if (abs(dx) > abs(dy)) {
                                if (dx > 0) Direction.RIGHT else Direction.LEFT
                            } else {
                                if (dy > 0) Direction.DOWN else Direction.UP
                            }
                            onSwipe(direction)
                        }
                    },
                )
            }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            board.forEach { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value ->
                        Tile(value, Modifier.weight(1f).fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(value: Int, modifier: Modifier = Modifier) {
    val background = tileColor(value)
    val textColor = if (value <= 4) BitsColors.Ink else Color(0xFF16202B)
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(value.toString(), style = BitsText.BodyBold.copy(color = textColor))
        }
    }
}

private fun tileColor(value: Int): Color = when (value) {
    0 -> Color(0x14EAE6DA)
    2 -> Color(0xFF3A4B5E)
    4 -> Color(0xFF4E6478)
    8 -> Color(0xFFF2B544)
    16 -> Color(0xFFEFA93E)
    32 -> Color(0xFFEC9038)
    64 -> Color(0xFFE87A32)
    128 -> Color(0xFFE8C25B)
    256 -> Color(0xFFE8BB43)
    512 -> Color(0xFFE8B42B)
    1024 -> Color(0xFFE8AD13)
    else -> Color(0xFFE8A600)
}

@Composable
private fun DirectionPad(onMove: (Direction) -> Unit, modifier: Modifier = Modifier) {
    val buttonSize = 52.dp
    @Composable
    fun Pad(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(12.dp))
                .background(BitsColors.PanelBase)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = BitsColors.Ink)
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Pad(Icons.Filled.KeyboardArrowUp, "Slide up") { onMove(Direction.UP) }
        Row(horizontalArrangement = Arrangement.spacedBy(52.dp)) {
            Pad(Icons.Filled.KeyboardArrowLeft, "Slide left") { onMove(Direction.LEFT) }
            Pad(Icons.Filled.KeyboardArrowRight, "Slide right") { onMove(Direction.RIGHT) }
        }
        Pad(Icons.Filled.KeyboardArrowDown, "Slide down") { onMove(Direction.DOWN) }
    }
}
