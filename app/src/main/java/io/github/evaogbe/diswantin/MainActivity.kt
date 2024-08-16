package io.github.evaogbe.diswantin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.evaogbe.diswantin.app.ui.DiswantinApp
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiswantinTheme {
                DiswantinApp()
            }
        }
    }
}
