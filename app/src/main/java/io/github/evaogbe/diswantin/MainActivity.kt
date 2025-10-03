package io.github.evaogbe.diswantin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.evaogbe.diswantin.app.ui.DiswantinApp
import io.github.evaogbe.diswantin.app.ui.rememberDiswantinAppState
import io.github.evaogbe.diswantin.data.ClockMonitor
import io.github.evaogbe.diswantin.ui.preferences.LocalClock
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var clockMonitor: ClockMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = rememberDiswantinAppState(clockMonitor = clockMonitor)
            val clock by appState.clock.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalClock provides clock) {
                DiswantinTheme {
                    DiswantinApp(appState = appState)
                }
            }
        }
    }
}
