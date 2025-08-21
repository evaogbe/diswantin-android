package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

fun <T : Any> LazyListScope.pagedListFooter(
    pagingItems: LazyPagingItems<T>, errorMessage: @Composable () -> Unit
) {
    when (pagingItems.loadState.append) {
        is LoadState.Loading -> {
            item {
                NextPagePendingLayout()
            }
        }

        is LoadState.Error -> {
            item {
                NextPageErrorLayout(errorMessage = errorMessage, onRetry = { pagingItems.retry() })
            }
        }

        is LoadState.NotLoading -> {}
    }
}

@Composable
private fun NextPagePendingLayout() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SpaceSm),
        contentAlignment = Alignment.TopCenter,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun NextPageErrorLayout(errorMessage: @Composable (() -> Unit), onRetry: () -> Unit) {
    Surface(
        shape = ListItemDefaults.shape,
        color = colorScheme.errorContainer,
        contentColor = colorScheme.onErrorContainer,
        tonalElevation = ListItemDefaults.Elevation,
        shadowElevation = ListItemDefaults.Elevation,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpaceMd, vertical = SpaceSm),
            verticalArrangement = Arrangement.spacedBy(SpaceSm)
        ) {
            val mergedStyle = LocalTextStyle.current.merge(typography.bodyLarge)
            CompositionLocalProvider(
                LocalTextStyle provides mergedStyle,
                content = errorMessage,
            )
            TextButtonWithIcon(
                onClick = onRetry,
                imageVector = Icons.Default.Refresh,
                text = stringResource(R.string.retry_button),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.error,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun NextPendingLayoutPreview() {
    DiswantinTheme {
        Surface {
            NextPagePendingLayout()
        }
    }
}

@DevicePreviews
@Composable
private fun NextPageErrorLayoutPreview() {
    DiswantinTheme {
        NextPageErrorLayout(
            errorMessage = { Text("Something went wrong loading the items") },
            onRetry = { },
        )
    }
}
