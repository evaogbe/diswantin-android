package io.github.evaogbe.diswantin.advice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ExtremeDistressAdviceScreen(modifier: Modifier = Modifier) {
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
                    stringResource(R.string.advice_extreme_distress_intro),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(SpaceLg))
            SelectionContainer {
                StyledList(
                    items = persistentListOf(
                        StyledListItem(
                            annotatedStringResource(
                                R.string.advice_tipp_step1,
                                html = false
                            )
                        ),
                        StyledListItem(
                            annotatedStringResource(
                                R.string.advice_tipp_step2,
                                html = false
                            )
                        ),
                        StyledListItem(
                            annotatedStringResource(
                                R.string.advice_tipp_step3,
                                html = false
                            )
                        ),
                        StyledListItem(
                            annotatedStringResource(
                                R.string.advice_tipp_step4,
                                html = false
                            )
                        ),
                    ),
                    type = StyledListType.Numbered,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun ExtremeDistressAdviceScreenPreview() {
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
            ExtremeDistressAdviceScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
