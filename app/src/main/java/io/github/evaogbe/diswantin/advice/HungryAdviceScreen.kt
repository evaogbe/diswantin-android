package io.github.evaogbe.diswantin.advice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HungryAdviceScreen(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(SpaceMd)
                .widthIn(max = ScreenLg)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            SelectionContainer {
                StyledList(
                    items = persistentListOf(
                        StyledListItem(annotatedStringResource(R.string.advice_hungry_suggestion_snack)),
                    )
                )
            }
            AdviceButton(onClick = onContinueClick, text = stringResource(R.string.continue_button))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun HungryAdviceScreenPreview() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        InnerAdviceTopBar(
                            onSearchTask = {},
                            onBackClick = {},
                            onRestart = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                        )
                    }
                }
            },
        ) { innerPadding ->
            HungryAdviceScreen(onContinueClick = {}, modifier = Modifier.padding(innerPadding))
        }
    }
}
