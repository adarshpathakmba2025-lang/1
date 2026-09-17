package com.bits.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bits.app.R
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText

enum class GameId(val key: String, val title: String, val blurb: String, val icon: Int, val free: Boolean) {
    TwentyFortyEight("2048", "2048", "Slide tiles, double them up", R.drawable.ic_game_2048, true),
    Snake("snake", "Snake", "Eat, grow, don't bite yourself", R.drawable.ic_game_snake, true),
    Memory("memory", "Memory Match", "Flip cards, find the pairs", R.drawable.ic_game_memory, false),
    TicTacToe("tictactoe", "X and O", "Three in a row, if you can", R.drawable.ic_game_tictactoe, false),
    Wordle("wordle", "Word Guess", "Five letters, one word a day", R.drawable.ic_game_wordle, false),
    Flappy("flappy", "Flappy", "Tap to flap, mind the pipes", R.drawable.ic_game_flappy, false),
    Spaca("spaca", "Spaca", "Steer, shoot, clear the sky", R.drawable.ic_game_spaca, false),
    Bitris("bitris", "Bitris", "Stack the blocks, clear the rows", R.drawable.ic_game_bitris, false),
}

@Composable
fun GamesHubScreen(
    canPlay: (String, Boolean) -> Boolean,
    highScoreFor: (String) -> Int,
    onBack: () -> Unit,
    onPlay: (GameId) -> Unit,
    onUpgrade: () -> Unit,
) {
    val allUnlocked = GameId.entries.all { canPlay(it.key, it.free) }
    Column(Modifier.fillMaxSize().background(Arcade.Screen)) {
        ArcadeHeader(title = "Games", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // The pixel font is wide, so the quote is sized to sit on one line
                // and the reply is a separate line beneath it, never re-wrapped.
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = "\u201CGames section in a widget list app?\u201D",
                        style = BitsText.PixelBody.copy(color = BitsColors.Ink),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = "Yeah, because why not. Haha.",
                        style = BitsText.PixelBody.copy(color = BitsColors.Muted),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            items(GameId.entries) { game ->
                val locked = !canPlay(game.key, game.free)
                GameCard(
                    game = game,
                    locked = locked,
                    best = highScoreFor(game.key),
                    onClick = { if (locked) onUpgrade() else onPlay(game) },
                )
            }

            if (!allUnlocked) {
                item {
                    PixelPanel(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("MORE GAMES WITH PRO", style = BitsText.PixelHeading.copy(color = Arcade.Glow))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "More games to unlock, plus widget themes and clock styles.",
                            style = BitsText.PixelBody,
                        )
                        Spacer(Modifier.height(12.dp))
                        PixelButton("See Pro", onClick = onUpgrade)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameId, locked: Boolean, best: Int, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            Modifier
                .padding(start = 4.dp, top = 4.dp)
                .matchParentSize()
                .background(Color(0x66000000))
        )
        Row(
            Modifier
                .padding(end = 4.dp, bottom = 4.dp)
                .fillMaxWidth()
                .background(Arcade.Border)
                .padding(2.dp)
                .background(Arcade.Panel)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = game.title.uppercase(),
                    style = BitsText.PixelHeading.copy(
                        color = if (locked) BitsColors.Muted else BitsColors.Ink,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Text(game.blurb, style = BitsText.PixelBody)
                if (best > 0 && !locked) {
                    Spacer(Modifier.height(6.dp))
                    Text("BEST $best", style = BitsText.PixelScore)
                }
                if (locked) {
                    Spacer(Modifier.height(6.dp))
                    Text("PRO", style = BitsText.PixelBody.copy(color = Arcade.Glow))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(game.icon),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                )
                if (locked) {
                    // Dim the sprite and lay a padlock over it.
                    Box(Modifier.matchParentSize().background(Arcade.Panel.copy(alpha = 0.68f)))
                    Image(
                        painter = painterResource(R.drawable.ic_lock_pixel),
                        contentDescription = "Locked",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
