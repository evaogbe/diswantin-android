package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
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
    ListItem(
        headlineContent = errorMessage,
        supportingContent = {
            TextButtonWithIcon(
                onClick = onRetry,
                imageVector = Icons.Default.Refresh,
                text = stringResource(R.string.retry_button),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.error,
                ),
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = colorScheme.errorContainer,
            headlineColor = colorScheme.onErrorContainer,
        ),
    )
}

@Preview
@Composable
private fun NextPendingLayoutPreview() {
    DiswantinTheme {
        NextPagePendingLayout()
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
