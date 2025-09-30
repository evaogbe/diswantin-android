package io.github.evaogbe.diswantin.advice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun DistressLevelAdviceScreen(
    onLowClick: () -> Unit,
    onHighClick: () -> Unit,
    onExtremeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd)
                .widthIn(max = ScreenLg)
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    stringResource(R.string.advice_distress_level_intro),
                    style = typography.titleMedium,
                )
            }
            Spacer(Modifier.size(SpaceLg))
            AdviceButton(
                onClick = onLowClick,
                text = stringResource(R.string.advice_distress_low_button),
            )
            Spacer(Modifier.size(SpaceMd))
            AdviceButton(
                onClick = onHighClick,
                text = stringResource(R.string.advice_distress_high_button),
            )
            Spacer(Modifier.size(SpaceMd))
            AdviceButton(
                onClick = onExtremeClick,
                text = stringResource(R.string.advice_distress_extreme_button),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun DistressLevelAdviceScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = { InnerAdviceTopBar(onBackClick = {}, onRestart = {}) }) { innerPadding ->
            DistressLevelAdviceScreen(
                onLowClick = {},
                onHighClick = {},
                onExtremeClick = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
