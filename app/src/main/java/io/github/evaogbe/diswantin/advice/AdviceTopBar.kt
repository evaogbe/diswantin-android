package io.github.evaogbe.diswantin.advice

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartAdviceTopBar(modifier: Modifier = Modifier) {
    TopAppBar(title = {}, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InnerAdviceTopBar(
    onBackClick: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {},
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
        }
    )
}
