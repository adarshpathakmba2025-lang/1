package com.bits.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bits.app.data.BitsRepository
import com.bits.app.data.TODAY_ID
import com.bits.app.data.claimBonusTheme
import com.bits.app.data.withEasterEggUsed
import com.bits.app.data.withHideHintSeen
import com.bits.app.data.withHighScore
import com.bits.app.data.withTutorialSeen
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.delay

sealed interface LaunchRequest {
    data class OpenCategory(val categoryId: String) : LaunchRequest
    data object OpenSettings : LaunchRequest
    data object OpenGames : LaunchRequest
    data object OpenHome : LaunchRequest
}

private enum class Screen { Home, Settings, Paywall, GamesHub, Playing }

@Composable
fun BitsApp(launchRequest: LaunchRequest?, onLaunchHandled: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { BitsRepository.get(context) }
    val state by repository.state.collectAsState()
    val targets = remember { TutorialTargets() }

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(TODAY_ID) }
    var playing by remember { mutableStateOf<GameId?>(null) }
    var showFounder by remember { mutableStateOf(false) }

    // Easter egg: six taps on the "Bits" title, once per device.
    var tapCount by remember { mutableIntStateOf(0) }
    var showUnlock by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.load()
        while (true) {
            delay(30_000)
            repository.load()
        }
    }

    // Taps run out after a moment, so ordinary taps never accumulate into the egg.
    LaunchedEffect(tapCount) {
        if (tapCount in 1 until 6) {
            delay(1200)
            tapCount = 0
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    LaunchedEffect(launchRequest) {
        when (launchRequest) {
            is LaunchRequest.OpenCategory -> {
                selectedCategoryId = launchRequest.categoryId
                screen = Screen.Home
            }
            LaunchRequest.OpenSettings -> screen = Screen.Settings
            LaunchRequest.OpenGames -> screen = Screen.GamesHub
            LaunchRequest.OpenHome -> screen = Screen.Home
            null -> return@LaunchedEffect
        }
        onLaunchHandled()
    }

    BackHandler(enabled = screen == Screen.Playing) { playing = null; screen = Screen.GamesHub }
    BackHandler(enabled = screen == Screen.Paywall) { screen = Screen.Settings }
    BackHandler(enabled = screen == Screen.Settings || screen == Screen.GamesHub) { screen = Screen.Home }

    val current = state
    val tutorialActive = current != null && !current.preferences.tutorialSeen && screen == Screen.Home

    CompositionLocalProvider(LocalTutorialTargets provides targets) {
        Box(Modifier.fillMaxSize().background(BitsColors.Bg)) {
            DotGrid(Modifier.fillMaxSize())

            if (current == null) {
                Text(
                    text = "Opening Bits\u2026",
                    style = BitsText.Body.copy(color = BitsColors.Muted),
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            state = current,
                            repository = repository,
                            selectedCategoryId = selectedCategoryId,
                            onSelectCategory = { selectedCategoryId = it },
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenGames = { screen = Screen.GamesHub },
                            onTitleTap = {
                                if (!current.preferences.easterEggUsed) {
                                    tapCount += 1
                                    if (tapCount >= 6) {
                                        tapCount = 0
                                        showUnlock = true
                                    }
                                }
                            },
                            onHiddenCategory = {
                                if (!current.preferences.hideHintSeen) {
                                    toast = "Hidden from your widget. Tap the name again to bring it back."
                                    repository.edit { it.withHideHintSeen() }
                                }
                            },
                            tutorialActive = tutorialActive,
                        )

                        Screen.Settings -> SettingsScreen(
                            state = current,
                            repository = repository,
                            onBack = { screen = Screen.Home },
                            onOpenPaywall = { showFounder = true },
                            onReplayTour = {
                                repository.edit { it.withTutorialSeen(false) }
                                screen = Screen.Home
                            },
                            onRestorePurchases = {
                                toast = "Nothing to restore yet \u2014 purchases aren't switched on."
                            },
                        )

                        Screen.Paywall -> PaywallScreen(
                            state = current,
                            onBack = { screen = Screen.Settings },
                            onPurchase = { toast = "Payments aren't switched on yet." },
                        )

                        Screen.GamesHub -> GamesHubScreen(
                            isPro = current.preferences.isPro,
                            highScoreFor = { current.highScore(it) },
                            onBack = { screen = Screen.Home },
                            onPlay = { game ->
                                playing = game
                                screen = Screen.Playing
                            },
                            onUpgrade = { showFounder = true },
                        )

                        Screen.Playing -> {
                            val back = { playing = null; screen = Screen.GamesHub }
                            val record: (String, Int) -> Unit = { key, score ->
                                repository.edit { it.withHighScore(key, score) }
                            }
                            when (playing) {
                                GameId.TwentyFortyEight -> Game2048Screen(
                                    best = current.highScore(GameId.TwentyFortyEight.key),
                                    onScore = { record(GameId.TwentyFortyEight.key, it) },
                                    onBack = back,
                                )
                                GameId.Snake -> SnakeScreen(
                                    best = current.highScore(GameId.Snake.key),
                                    onScore = { record(GameId.Snake.key, it) },
                                    onBack = back,
                                )
                                GameId.Memory -> MemoryScreen(
                                    best = current.highScore(GameId.Memory.key),
                                    onScore = { record(GameId.Memory.key, it) },
                                    onBack = back,
                                )
                                GameId.TicTacToe -> TicTacToeScreen(onBack = back)
                                GameId.Wordle -> WordleScreen(
                                    best = current.highScore(GameId.Wordle.key),
                                    onScore = { record(GameId.Wordle.key, it) },
                                    onBack = back,
                                )
                                GameId.Flappy -> FlappyScreen(
                                    best = current.highScore(GameId.Flappy.key),
                                    onScore = { record(GameId.Flappy.key, it) },
                                    onBack = back,
                                )
                                null -> back()
                            }
                        }
                    }
                }

                if (tutorialActive) {
                    TutorialOverlay(
                        targets = targets,
                        onFinish = { repository.edit { it.withTutorialSeen(true) } },
                    )
                }

                if (showFounder) {
                    FounderDialog(
                        isPro = current.preferences.isPro,
                        onContinue = {
                            showFounder = false
                            screen = Screen.Paywall
                        },
                    )
                }

                if (showUnlock) {
                    ThemeUnlockDialog(
                        onPick = { theme ->
                            repository.edit { it.claimBonusTheme(theme.id).withEasterEggUsed() }
                            showUnlock = false
                            celebrate = true
                            toast = "${theme.displayName} unlocked. Enjoy!"
                        },
                        onDismiss = { showUnlock = false },
                    )
                }

                if (celebrate) {
                    ConfettiBurst(onFinished = { celebrate = false })
                }

                Toast(
                    message = toast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                )
            }
        }
    }
}

/** A brief message that fades in at the bottom and leaves on its own. */
@Composable
private fun Toast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Text(
            text = message.orEmpty(),
            style = BitsText.Small.copy(color = BitsColors.Ink),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BitsColors.PanelBase)
                .padding(horizontal = 16.dp, vertical = 13.dp),
        )
    }
}
