package io.github.evaogbe.diswantin.task.ui

import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.core.text.HtmlCompat
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.theme.SpaceXs
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdviceTopBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        title = {},
        modifier = modifier,
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_tasks_button),
                )
            }
        },
    )
}

@Composable
fun AdviceScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SelectionContainer {
            BulletedList(
                items = persistentListOf(
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_stop)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_meditate)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_break_down_task)),
                    BulletedItem(
                        annotatedStringResource(R.string.suggestion_item_check_the_facts),
                        stringArrayResource(R.array.suggestion_item_check_the_facts_steps).map {
                            BulletedItem(
                                AnnotatedString(it)
                            )
                        }.toImmutableList()
                    ),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_body_doubling)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_journal)),
                    BulletedItem(
                        annotatedStringResource(R.string.suggestion_item_move),
                        stringArrayResource(R.array.suggestion_item_move_sublist).map {
                            BulletedItem(AnnotatedString(it))
                        }.toImmutableList()
                    ),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_snack)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_sleep)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_friend)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_meds)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_day_off))
                ),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(SpaceMd)
                    .widthIn(max = ScreenLg)
            )
        }
    }
}

data class BulletedItem(
    val text: AnnotatedString,
    val children: ImmutableList<BulletedItem> = persistentListOf(),
)

@Composable
fun BulletedList(items: ImmutableList<BulletedItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(Modifier.size(SpaceSm))
            }

            Row {
                Text(text = "\u2022", modifier = Modifier.padding(start = SpaceMd, end = SpaceXs))
                Text(item.text)
            }

            if (item.children.isNotEmpty()) {
                BulletedList(
                    items = item.children,
                    modifier = Modifier.padding(start = SpaceLg)
                )
            }
        }
    }
}

@Composable
@ReadOnlyComposable
fun annotatedStringResource(@StringRes id: Int): AnnotatedString {
    val text = HtmlCompat.fromHtml(stringResource(id), HtmlCompat.FROM_HTML_MODE_COMPACT)
    return buildAnnotatedString {
        append(text.toString())
        text.getSpans(0, text.length, Any::class.java).forEach { span ->
            if (span is StyleSpan && span.style == Typeface.BOLD) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    text.getSpanStart(span),
                    text.getSpanEnd(span)
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun AdviceScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = { AdviceTopBar(onSearch = {}) }) { innerPadding ->
            AdviceScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
