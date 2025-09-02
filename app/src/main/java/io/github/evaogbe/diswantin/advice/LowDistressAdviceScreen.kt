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
import androidx.compose.material3.TextButton
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
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LowDistressAdviceScreen(onCheckTheFactsClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd)
                .widthIn(max = ScreenLg)
                .fillMaxWidth(),
        ) {
            SelectionContainer {
                StyledList(
                    items = persistentListOf(
                        StyledListItem(annotatedStringResource(R.string.advice_low_distress_suggestion_body_double)),
                        StyledListItem(annotatedStringResource(R.string.advice_low_distress_suggestion_break_down_task)),
                        StyledListItem(annotatedStringResource(R.string.advice_low_distress_suggestion_journal)),
                    )
                )
            }
            Spacer(Modifier.size(SpaceLg))
            SelectionContainer {
                Text(
                    stringResource(R.string.advice_low_distress_wise_mind),
                    style = typography.titleMedium,
                )
            }
            Spacer(Modifier.size(SpaceMd))
            TextButton(onClick = onCheckTheFactsClick) {
                Text(stringResource(R.string.advice_low_distress_details_button))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun LowDistressAdviceScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = { InnerAdviceTopBar(onBackClick = {}, onRestart = {}) }) { innerPadding ->
            LowDistressAdviceScreen(
                onCheckTheFactsClick = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
