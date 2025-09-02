package io.github.evaogbe.diswantin.advice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf

@Composable
fun PainAdviceScreen(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd)
                .widthIn(max = ScreenLg)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            SelectionContainer {
                StyledList(
                    items = persistentListOf(
                        StyledListItem(annotatedStringResource(R.string.advice_pain_suggestion_bath)),
                        StyledListItem(annotatedStringResource(R.string.advice_pain_suggestion_hot_pack)),
                        StyledListItem(annotatedStringResource(R.string.advice_pain_suggestion_tea)),
                        StyledListItem(annotatedStringResource(R.string.advice_pain_suggestion_massage)),
                        StyledListItem(annotatedStringResource(R.string.advice_pain_suggestion_stretch)),
                        StyledListItem(annotatedStringResource(R.string.advice_pain_suggestion_meds)),
                    )
                )
            }
            TextButton(onClick = onContinueClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.continue_button), textAlign = TextAlign.Center)
            }
        }
    }
}

@DevicePreviews
@Composable
private fun PainAdviceScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = { InnerAdviceTopBar(onBackClick = {}, onRestart = {}) }) { innerPadding ->
            PainAdviceScreen(onContinueClick = {}, modifier = Modifier.padding(innerPadding))
        }
    }
}
