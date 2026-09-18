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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import com.bits.app.billing.PlayProStore
import com.bits.app.data.BitsRepository
import com.bits.app.data.FreeStore
import com.bits.app.data.findActivity
import com.bits.app.data.Monetization
import com.bits.app.data.ProPlan
import com.bits.app.data.ProStore
import com.bits.app.data.TODAY_ID
import com.bits.app.data.ClockStyles
import com.bits.app.data.WidgetThemes
import com.bits.app.data.claimEasterEgg
import com.bits.app.data.spendOnReveal
import com.bits.app.data.spendOnHint
import com.bits.app.data.withRejectedAttempt
import com.bits.app.data.startWordleDay
import com.bits.app.data.withWordleGuess
import com.bits.app.data.withHideHintSeen
import com.bits.app.data.resetMemoryBest
import com.bits.app.data.withHighScore
import com.bits.app.data.withMemoryTries
import com.bits.app.data.withOnboardingDone
import com.bits.app.data.withTutorialSeen
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.delay

sealed interface LaunchRequest {
    data class OpenCategory(val categoryId: String) : LaunchRequest
    data object OpenSettings : LaunchRequest
    data object OpenGames : LaunchRequest
    data object OpenHome : LaunchRequest
    data object OpenCustomize : LaunchRequest
}

private enum class Screen { Onboarding, Home, Settings, Customize, Paywall, GamesHub, Playing }

@Composable
fun BitsApp(launchRequest: LaunchRequest?, onLaunchHandled: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { BitsRepository.get(context) }
    val state by repository.state.collectAsState()
    val targets = remember { TutorialTargets() }

    // The only line in the app outside the billing package that names PlayProStore. If
    // that file ever fails to build against a new Billing version, replacing this whole
    // block with `val proStore: ProStore = FreeStore` restores a working app, minus the
    // ability to sell anything.
    val proStore: ProStore = remember {
        if (Monetization.ENABLED) {
            PlayProStore(context) { isPro, plan -> repository.applyEntitlement(isPro, plan) }
        } else {
            FreeStore
        }
    }
    // Held open only while the app is on screen; Play's connection is not free to keep.
    DisposableEffect(proStore) {
        proStore.start()
        onDispose { proStore.stop() }
    }
    val offers by proStore.offers.collectAsState()

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    // Where the Pro page was opened from, so backing out of it returns there instead of
    // always dumping the user in Settings.
    var paywallOrigin by rememberSaveable { mutableStateOf(Screen.Settings) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(TODAY_ID) }
    var playing by remember { mutableStateOf<GameId?>(null) }
    var showFounder by remember { mutableStateOf(false) }
    // The founder note is a one-time hello per app run, not a wall in front of every
    // locked item. After it's been seen, locked things open the Pro page directly.
    var founderShownThisSession by rememberSaveable { mutableStateOf(false) }

    // Remembers which screen asked for Pro, so backing out returns there.
    val openPro: () -> Unit = {
        paywallOrigin = if (screen == Screen.Paywall) paywallOrigin else screen
        if (founderShownThisSession) {
            screen = Screen.Paywall
        } else {
            founderShownThisSession = true
            showFounder = true
        }
    }

    // Easter egg: six taps on the "Bits" title, once per device.
    var tapCount by remember { mutableIntStateOf(0) }
    var showUnlock by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    var toast by remember { mutableStateOf<String?>(null) }
    val lastDeleted by repository.lastDeleted.collectAsState()

    // The undo offer is short-lived; after a few seconds the deletion just stands.
    LaunchedEffect(lastDeleted) {
        if (lastDeleted != null) {
            delay(5000)
            repository.clearUndo()
        }
    }

    LaunchedEffect(Unit) {
        repository.load()
        while (true) {
            delay(30_000)
            repository.load()
        }
    }

    // Taps run out after a moment, so ordinary taps never accumulate into the egg.
    LaunchedEffect(tapCount) {
        if (tapCount in 1 until 4) {
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
            LaunchRequest.OpenCustomize -> screen = Screen.Customize
            null -> return@LaunchedEffect
        }
        onLaunchHandled()
    }

    BackHandler(enabled = screen == Screen.Playing) { playing = null; screen = Screen.GamesHub }
    BackHandler(enabled = screen == Screen.Paywall) { screen = paywallOrigin }
    BackHandler(
        enabled = screen == Screen.Settings || screen == Screen.GamesHub || screen == Screen.Customize
    ) { screen = Screen.Home }

    val current = state

    // New installs are walked through placing the widget before anything else.
    LaunchedEffect(current?.preferences?.onboardingDone) {
        val prefs = current?.preferences ?: return@LaunchedEffect
        if (!prefs.onboardingDone && screen == Screen.Home) screen = Screen.Onboarding
    }

    val tutorialActive = current != null &&
        current.preferences.onboardingDone &&
        !current.preferences.tutorialSeen &&
        screen == Screen.Home

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
                        Screen.Onboarding -> OnboardingScreen(
                            onDone = {
                                repository.edit { it.withOnboardingDone() }
                                screen = Screen.Home
                            },
                        )

                        Screen.Home -> HomeScreen(
                            state = current,
                            repository = repository,
                            selectedCategoryId = selectedCategoryId,
                            onSelectCategory = { selectedCategoryId = it },
                            onOpenSettings = { screen = Screen.Settings },
                            onOpenGames = { screen = Screen.GamesHub },
                            onOpenCustomize = { screen = Screen.Customize },
                            onTitleTap = {
                                if (current.easterEggAvailable) {
                                    tapCount += 1
                                    if (tapCount >= 4) {
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
                            onOpenPaywall = openPro,
                            onReplayTour = {
                                repository.edit { it.withTutorialSeen(false) }
                                screen = Screen.Home
                            },
                            onRestorePurchases = {
                                toast = "Nothing to restore yet \u2014 purchases aren't switched on."
                            },
                        )

                        Screen.Customize -> CustomizeScreen(
                            state = current,
                            repository = repository,
                            onBack = { screen = Screen.Home },
                            onOpenPaywall = openPro,
                        )

                        Screen.Paywall -> PaywallScreen(
                            state = current,
                            onBack = { screen = paywallOrigin },
                            offers = offers,
                            onPurchase = { plan ->
                                val activity = context.findActivity()
                                val productId = when (plan) {
                                    ProPlan.MONTHLY -> Monetization.MONTHLY_PRODUCT_ID
                                    else -> Monetization.LIFETIME_PRODUCT_ID
                                }
                                if (activity == null) {
                                    toast = "Couldn't open Google Play."
                                } else {
                                    proStore.purchase(activity, productId)
                                }
                            },
                        )

                        Screen.GamesHub -> GamesHubScreen(
                            canPlay = { id, free -> current.canPlayGame(id, free) },
                            highScoreFor = { current.highScore(it) },
                            onBack = { screen = Screen.Home },
                            onPlay = { game ->
                                playing = game
                                screen = Screen.Playing
                            },
                            onUpgrade = openPro,
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
                                    bestTries = current.preferences.memoryBestTries,
                                    onCleared = { tries -> repository.edit { it.withMemoryTries(tries) } },
                                    onResetBest = { repository.edit { it.resetMemoryBest() } },
                                    onBack = back,
                                )
                                GameId.TicTacToe -> TicTacToeScreen(onBack = back)
                                GameId.Spasa -> SpasaScreen(
                                    best = current.highScore(GameId.Spasa.key),
                                    onScore = { record(GameId.Spasa.key, it) },
                                    onBack = back,
                                )
                                GameId.Bitris -> BitrisScreen(
                                    best = current.highScore(GameId.Bitris.key),
                                    onScore = { record(GameId.Bitris.key, it) },
                                    onBack = back,
                                )
                                // Chess keeps a count of wins against the computer
                                // rather than a score, which the same store handles.
                                GameId.Chess -> ChessScreen(
                                    wins = current.highScore(GameId.Chess.key),
                                    onWin = { record(GameId.Chess.key, it) },
                                    onBack = back,
                                )
                                GameId.Wordle -> {
                                    val day = java.time.LocalDate.now().toEpochDay()
                                    // A new day wipes the board; a skipped day also breaks the streak.
                                    LaunchedEffect(day) {
                                        if (current.preferences.wordleDay != day) {
                                            val missed = current.preferences.wordleDay != day - 1L &&
                                                current.preferences.wordleDay != 0L
                                            repository.edit { it.startWordleDay(day, brokeStreak = missed) }
                                        }
                                    }
                                    if (current.preferences.wordleDay == day) {
                                        WordleScreen(
                                            dayIndex = day,
                                            guesses = current.preferences.wordleGuesses,
                                            streak = current.preferences.wordleStreak,
                                            best = current.highScore(GameId.Wordle.key),
                                            hintPoints = current.preferences.hintPoints,
                                            purchased = current.preferences.wordleRevealed,
                                            attempts = current.preferences.wordleAttempts,
                                            onGuess = { guess, won ->
                                                repository.edit { s ->
                                                    val next = s.withWordleGuess(day, guess, won)
                                                    if (won) next.withHighScore(GameId.Wordle.key, next.preferences.wordleStreak)
                                                    else next
                                                }
                                            },
                                            onBuyReveal = { index ->
                                                repository.edit { it.spendOnReveal(index, com.bits.app.games.Wordle.REVEAL_COST) }
                                            },
                                            onHint = { index ->
                                                repository.edit { it.spendOnHint(index, com.bits.app.games.Wordle.PEEK_COST) }
                                            },
                                            onRejected = { repository.edit { it.withRejectedAttempt() } },
                                            onBack = back,
                                        )
                                    }
                                }
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

                if (showUnlock && current.easterEggAvailable) {
                    // Only things still locked are offered, so a set is never wasted.
                    EasterEggDialog(
                        lockedThemes = WidgetThemes.all.filter { !it.free && !current.canUseTheme(it.id) },
                        lockedGames = GameId.entries
                            .filter { !it.free && !current.canPlayGame(it.key, it.free) }
                            .map { it.key to it.title },
                        lockedClocks = ClockStyles.all
                            .filter { !it.free && !current.canUseClockStyle(it.id) }
                            .map { it.id to it.displayName },
                        onClaim = { themeId, gameId, clockId ->
                            repository.edit { it.claimEasterEgg(themeId, gameId, clockId) }
                            showUnlock = false
                            celebrate = true
                            toast = "Unlocked! Three things are yours to keep."
                        },
                        onDismiss = { showUnlock = false },
                    )
                }

                if (celebrate) {
                    ConfettiBurst(onFinished = { celebrate = false })
                }

                UndoBar(
                    item = lastDeleted,
                    onUndo = { repository.undoDelete() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                )

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

/** Offers a few seconds to put back whatever was just deleted. */
@Composable
private fun UndoBar(item: com.bits.app.data.Item?, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = item != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        Row(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BitsColors.PanelBase)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Deleted “${item?.text.orEmpty().take(28)}”",
                style = BitsText.Small.copy(color = BitsColors.Ink),
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(vertical = 13.dp),
            )
            TextAction("Undo", BitsColors.Amber, onUndo)
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
