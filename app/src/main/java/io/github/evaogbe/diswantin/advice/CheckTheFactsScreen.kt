package io.github.evaogbe.diswantin.advice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf

@Composable
fun CheckTheFactsScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SelectionContainer {
            StyledList(
                items = persistentListOf(
                    StyledListItem(
                        annotatedStringResource(
                            R.string.advice_check_the_facts_step1,
                            html = false
                        )
                    ),
                    StyledListItem(
                        annotatedStringResource(
                            R.string.advice_check_the_facts_step2,
                            html = false
                        )
                    ),
                    StyledListItem(
                        annotatedStringResource(
                            R.string.advice_check_the_facts_step3,
                            html = false
                        )
                    ),
                    StyledListItem(
                        annotatedStringResource(
                            R.string.advice_check_the_facts_step4,
                            html = false
                        )
                    ),
                    StyledListItem(
                        annotatedStringResource(
                            R.string.advice_check_the_facts_step5,
                            html = false
                        )
                    ),
                    StyledListItem(
                        annotatedStringResource(
                            R.string.advice_check_the_facts_step6,
                            html = false
                        )
                    ),
                ),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(SpaceMd)
                    .widthIn(max = ScreenLg)
                    .fillMaxWidth(),
                type = StyledListType.Numbered,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun CheckTheFactsScreenPreview() {
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
            CheckTheFactsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
