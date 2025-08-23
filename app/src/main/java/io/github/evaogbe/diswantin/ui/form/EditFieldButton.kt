package io.github.evaogbe.diswantin.ui.form

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun EditFieldButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shapes.extraSmall,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = text)
    }
}

@DevicePreviews
@Composable
private fun EditFieldButtonPreview() {
    DiswantinTheme {
        EditFieldButton(onClick = {}, text = "Lorem ipsum")
    }
}
