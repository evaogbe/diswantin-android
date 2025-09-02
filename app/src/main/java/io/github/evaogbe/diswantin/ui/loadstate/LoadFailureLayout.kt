package io.github.evaogbe.diswantin.ui.loadstate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.button.OutlinedButtonWithIcon
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeXl
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun LoadFailureLayout(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.errorContainer) {
        Column(
            modifier = Modifier.padding(SpaceMd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_sentiment_very_dissatisfied_24),
                contentDescription = null,
                modifier = Modifier.size(IconSizeXl),
            )
            Spacer(Modifier.size(SpaceXl))
            SelectionContainer(modifier = Modifier.widthIn(max = ScreenLg)) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = typography.headlineLarge,
                )
            }

            if (onRetry != null) {
                Spacer(Modifier.size(SpaceLg))
                OutlinedButtonWithIcon(
                    onClick = onRetry,
                    painter = painterResource(R.drawable.outline_refresh_24),
                    text = stringResource(R.string.retry_button),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.error,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(colorScheme.error),
                    ),
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun LoadFailureLayoutPreview_WithoutRetry() {
    DiswantinTheme {
        LoadFailureLayout(message = "Something went wrong performing that operation")
    }
}

@DevicePreviews
@Composable
private fun LoadFailureLayoutPreview_WithRetry() {
    DiswantinTheme {
        LoadFailureLayout(message = "Something went wrong performing that operation", onRetry = {})
    }
}
