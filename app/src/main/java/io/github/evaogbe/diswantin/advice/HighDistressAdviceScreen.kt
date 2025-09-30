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
fun HighDistressAdviceScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SelectionContainer {
            StyledList(
                items = persistentListOf(
                    StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_compassion)),
                    StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_imagery)),
                    StyledListItem(
                        annotatedStringResource(R.string.advice_high_distress_suggestion_move),
                        persistentListOf(
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_wiggle,
                                    html = false
                                )
                            ),
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_stretch,
                                    html = false
                                )
                            ),
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_dance,
                                    html = false
                                )
                            ),
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_home, html = false
                                )
                            ),
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_block,
                                    html = false
                                )
                            ),
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_backyard,
                                    html = false
                                )
                            ),
                            StyledListItem(
                                annotatedStringResource(
                                    R.string.advice_high_distress_suggestion_move_library,
                                    html = false
                                )
                            ),
                        ),
                    ),
                    StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_meditate)),
                    StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_positive_attributes)),
                    StyledListItem(
                        annotatedStringResource(R.string.advice_high_distress_suggestion_distract),
                        persistentListOf(
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_friend)),
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_gratitude)),
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_clean)),
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_shower)),
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_sing)),
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_read)),
                            StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_distract_art)),
                        )
                    ),
                    StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_meds)),
                    StyledListItem(annotatedStringResource(R.string.advice_high_distress_suggestion_day_off)),
                ),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(SpaceMd)
                    .widthIn(max = ScreenLg)
                    .fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun HighDistressAdviceScreenPreview() {
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
            HighDistressAdviceScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
