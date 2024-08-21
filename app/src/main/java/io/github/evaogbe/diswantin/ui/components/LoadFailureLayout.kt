package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun LoadFailureLayout(message: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.errorContainer) {
        Column(
            modifier = Modifier.padding(SpaceMd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_sentiment_very_dissatisfied_24),
                contentDescription = null,
                modifier = Modifier.size(IconSizeLg),
            )
            Spacer(Modifier.size(SpaceXl))
            SelectionContainer(modifier = Modifier.widthIn(max = ScreenLg)) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = typography.headlineLarge
                )
            }
        }
    }
}

@DevicePreviews
@Composable
fun LoadFailureLayoutPreview() {
    DiswantinTheme {
        Surface {
            LoadFailureLayout(message = "Something went wrong performing that operation")
        }
    }
}
