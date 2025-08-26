package io.github.evaogbe.diswantin.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun ClearableLayout(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    invert: Boolean = false,
    iconContentDescription: String = stringResource(R.string.clear_button),
    content: @Composable (RowScope.() -> Unit),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()

        IconButton(
            onClick = onClear,
            colors = if (invert) {
                IconButtonDefaults.iconButtonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant,
                )
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        ) {
            Icon(
                painterResource(R.drawable.baseline_close_24),
                contentDescription = iconContentDescription,
                tint = colorScheme.onSurfaceVariant,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ClearableLayoutPreview() {
    DiswantinTheme {
        Surface {
            ClearableLayout(onClear = {}) {
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 40.dp)
                        .background(colorScheme.surfaceVariant),
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun ClearableLayoutPreview_Inverted() {
    DiswantinTheme {
        Surface {
            ClearableLayout(onClear = {}, invert = true) {
                OutlinedTextField(state = TextFieldState(), label = { Text(text = "Name") })
            }
        }
    }
}
