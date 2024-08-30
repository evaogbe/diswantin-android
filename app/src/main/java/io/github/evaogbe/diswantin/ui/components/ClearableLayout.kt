package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun ClearableLayout(
    canClear: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        content()

        if (canClear) {
            Spacer(Modifier.size(SpaceSm))
            IconButton(
                onClick = onClear,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.clear_button)
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun ClearableContainerPreview() {
    DiswantinTheme {
        Surface {
            ClearableLayout(canClear = true, onClear = {}) {
                OutlinedTextField(value = "", onValueChange = {}, label = { Text(text = "Name") })
            }
        }
    }
}
