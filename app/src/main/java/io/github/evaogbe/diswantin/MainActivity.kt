package io.github.evaogbe.diswantin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import dagger.hilt.android.AndroidEntryPoint
import io.github.evaogbe.diswantin.app.ui.DiswantinApp
import io.github.evaogbe.diswantin.ui.preferences.LocalLocale
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var locale: Locale

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLocale provides locale) {
                DiswantinTheme {
                    DiswantinApp()
                }
            }
        }
    }
}
