package io.github.evaogbe.diswantin.advice

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.ui.TaskSearchTopBarButton

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun StartAdviceTopBar(
    onSearchTask: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    ) {
    TopAppBar(
        title = {
            TaskSearchTopBarButton(
                onClick = onSearchTask,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun InnerAdviceTopBar(
    onSearchTask: () -> Unit,
    onBackClick: () -> Unit,
    onRestart: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            TaskSearchTopBarButton(
                onClick = onSearchTask,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painterResource(R.drawable.baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.back_button)
                )
            }
        },
        actions = {
            IconButton(onClick = onRestart) {
                Icon(
                    painterResource(R.drawable.baseline_replay_24),
                    contentDescription = stringResource(R.string.restart_button),
                )
            }
        },
    )
}
