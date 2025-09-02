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
import androidx.compose.ui.text.style.TextAlign
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@Composable
fun BodySensationAdviceScreen(
    onHungryClick: () -> Unit,
    onTiredClick: () -> Unit,
    onPainClick: () -> Unit,
    onOtherClick: () -> Unit,
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
                    text = annotatedStringResource(R.string.advice_body_sensation_intro),
                    style = typography.titleMedium,
                )
            }
            Spacer(Modifier.size(SpaceLg))
            TextButton(onClick = onHungryClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.advice_body_sensation_hungry_button),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.size(SpaceMd))
            TextButton(onClick = onTiredClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.advice_body_sensation_tired_button),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.size(SpaceMd))
            TextButton(onClick = onPainClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.advice_body_sensation_pain_button),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.size(SpaceMd))
            TextButton(onClick = onOtherClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.advice_body_sensation_other_button),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun BodySensationAdviceScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = { StartAdviceTopBar() }) { innerPadding ->
            BodySensationAdviceScreen(
                onHungryClick = {},
                onTiredClick = {},
                onPainClick = {},
                onOtherClick = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
