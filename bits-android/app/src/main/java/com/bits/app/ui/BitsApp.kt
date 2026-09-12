package com.bits.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bits.app.data.BitsRepository
import com.bits.app.data.TODAY_ID
import com.bits.app.data.withTutorialSeen
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import kotlinx.coroutines.delay

sealed interface LaunchRequest {
    data class OpenCategory(val categoryId: String) : LaunchRequest
    data object OpenSettings : LaunchRequest
}

private enum class Screen { Home, Settings }

@Composable
fun BitsApp(launchRequest: LaunchRequest?, onLaunchHandled: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { BitsRepository.get(context) }
    val state by repository.state.collectAsState()
    val targets = remember { TutorialTargets() }

    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(TODAY_ID) }

    // Load, then keep checking for midnight while the app is open.
    LaunchedEffect(Unit) {
        repository.load()
        while (true) {
            delay(30_000)
            repository.load()
        }
    }

    // Taps on the widget open the app at the right place.
    LaunchedEffect(launchRequest) {
        when (launchRequest) {
            is LaunchRequest.OpenCategory -> {
                selectedCategoryId = launchRequest.categoryId
                screen = Screen.Home
            }
            LaunchRequest.OpenSettings -> screen = Screen.Settings
            null -> return@LaunchedEffect
        }
        onLaunchHandled()
    }

    BackHandler(enabled = screen == Screen.Settings) { screen = Screen.Home }

    val current = state
    val tutorialActive = current != null && !current.preferences.tutorialSeen && screen == Screen.Home

    LaunchedEffect(tutorialActive) {
        if (tutorialActive) selectedCategoryId = TODAY_ID
    }

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
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            state = current,
                            repository = repository,
                            selectedCategoryId = selectedCategoryId,
                            onSelectCategory = { selectedCategoryId = it },
                            onOpenSettings = { screen = Screen.Settings },
                            tutorialActive = tutorialActive,
                        )
                        Screen.Settings -> SettingsScreen(
                            state = current,
                            repository = repository,
                            onBack = { screen = Screen.Home },
                            onReplayTour = {
                                repository.edit { it.withTutorialSeen(false) }
                                screen = Screen.Home
                            },
                        )
                    }
                }

                if (tutorialActive) {
                    TutorialOverlay(
                        targets = targets,
                        onFinish = { repository.edit { it.withTutorialSeen(true) } },
                    )
                }
            }
        }
    }
}
