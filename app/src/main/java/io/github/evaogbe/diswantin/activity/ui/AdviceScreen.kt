package io.github.evaogbe.diswantin.activity.ui

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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdviceScreen(onClose: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.advice_title)) }, navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_button)
                )
            }
        })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            BulletedList(
                items = listOf(
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_stop)),
                    BulletedItem(
                        annotatedStringResource(R.string.suggestion_item_move),
                        stringArrayResource(R.array.suggestion_item_move_sublist).map {
                            BulletedItem(AnnotatedString(it))
                        }
                    ),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_imagine)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_snack)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_meditate)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_check_the_facts)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_journal)),
                    BulletedItem(annotatedStringResource(R.string.suggestion_item_break_down_task)),
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

data class BulletedItem(val text: AnnotatedString, val children: List<BulletedItem> = emptyList())

@Composable
fun BulletedList(items: List<BulletedItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(Modifier.size(SpaceSm))
            }

            Row {
                Text(text = "\u2022", modifier = Modifier.padding(start = SpaceMd, end = 4.dp))
                SelectionContainer {
                    Text(item.text)
                }
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
fun AdviceScreenPreview() {
    DiswantinTheme {
        AdviceScreen(onClose = {})
    }
}
