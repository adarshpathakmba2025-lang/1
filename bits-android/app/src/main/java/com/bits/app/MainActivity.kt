package com.bits.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.bits.app.data.BitsRepository
import com.bits.app.time.MidnightScheduler
import com.bits.app.ui.BitsApp
import com.bits.app.ui.LaunchRequest
import com.bits.app.ui.theme.BitsTheme

class MainActivity : ComponentActivity() {

    private val launchRequest = mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        MidnightScheduler.schedule(this)
        if (savedInstanceState == null) consumeIntent(intent)

        setContent {
            BitsTheme {
                BitsApp(
                    launchRequest = launchRequest.value,
                    onLaunchHandled = { launchRequest.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        BitsRepository.get(this).refresh()
    }

    override fun onPause() {
        BitsRepository.get(this).flush()
        super.onPause()
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        val categoryId = intent.getStringExtra(Launch.EXTRA_CATEGORY)
        when {
            categoryId != null -> launchRequest.value = LaunchRequest.OpenCategory(categoryId)
            intent.getBooleanExtra(Launch.EXTRA_SETTINGS, false) -> launchRequest.value = LaunchRequest.OpenSettings
        }
    }
}
