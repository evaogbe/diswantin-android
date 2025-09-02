package io.github.evaogbe.diswantin.ui.loadstate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeXl

const val PendingLayoutTestTag = "PendingLayoutTestTag"

@Composable
fun PendingLayout(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(PendingLayoutTestTag)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(IconSizeXl))
    }
}

@Preview
@Composable
private fun PendingLayoutPreview() {
    DiswantinTheme {
        Surface {
            PendingLayout()
        }
    }
}
