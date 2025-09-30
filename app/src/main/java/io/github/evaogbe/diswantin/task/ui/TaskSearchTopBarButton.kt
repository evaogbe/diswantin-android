package io.github.evaogbe.diswantin.task.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R

object TopBarSearchKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TaskSearchTopBarButton(
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    with(sharedTransitionScope) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier
                .sharedBounds(
                    rememberSharedContentState(key = TopBarSearchKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .fillMaxWidth(),
        ) {
            Text(stringResource(R.string.task_search_title))
        }
    }
}
