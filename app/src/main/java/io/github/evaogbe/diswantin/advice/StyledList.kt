package io.github.evaogbe.diswantin.advice

import android.graphics.Typeface
import android.text.style.StyleSpan
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.theme.SpaceXs
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
@ReadOnlyComposable
fun annotatedStringResource(@StringRes id: Int, html: Boolean = true) =
    if (html) {
        val text = HtmlCompat.fromHtml(stringResource(id), HtmlCompat.FROM_HTML_MODE_COMPACT)
        buildAnnotatedString {
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
    } else {
        AnnotatedString(stringResource(id))
    }

enum class StyledListType {
    Bulleted, Numbered
}

data class StyledListItem(
    val text: AnnotatedString,
    val children: ImmutableList<StyledListItem> = persistentListOf(),
)

@Composable
fun StyledList(
    items: ImmutableList<StyledListItem>,
    modifier: Modifier = Modifier,
    type: StyledListType = StyledListType.Bulleted,
) {
    val resources = LocalResources.current

    Column(
        modifier = modifier.semantics {
            collectionInfo = CollectionInfo(rowCount = items.size, columnCount = 1)
        },
    ) {
        items.forEachIndexed { index, item ->
            var expanded by remember { mutableStateOf(false) }
            val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)

            if (index > 0) {
                Spacer(Modifier.size(SpaceSm))
            }

            Row(
                modifier = Modifier
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(
                            rowIndex = index,
                            rowSpan = 0,
                            columnIndex = 0,
                            columnSpan = 0,
                        )

                        if (item.children.isNotEmpty()) {
                            stateDescription = if (expanded) {
                                resources.getString(R.string.accordion_expanded)
                            } else {
                                resources.getString(R.string.accordion_collapsed)
                            }
                        }
                    }
                    .toggleable(value = expanded, onValueChange = { expanded = !expanded }),
            ) {
                when (type) {
                    StyledListType.Bulleted -> {
                        Text(
                            text = "\u2022",
                            modifier = Modifier.padding(start = SpaceMd, end = SpaceXs),
                        )
                    }

                    StyledListType.Numbered -> {
                        Text(
                            text = "${index + 1}.",
                            modifier = Modifier.padding(start = SpaceMd, end = SpaceXs),
                        )
                    }
                }

                Text(item.text)

                if (item.children.isNotEmpty()) {
                    Spacer(Modifier.weight(1f))
                    Icon(
                        painterResource(R.drawable.baseline_arrow_drop_down_24),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(arrowRotation),
                    )
                }
            }

            if (item.children.isNotEmpty()) {
                AnimatedVisibility(visible = expanded) {
                    StyledList(
                        items = item.children,
                        modifier = Modifier.padding(start = SpaceLg),
                        type = type,
                    )
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun StyledListPreview_Bulleted() {
    DiswantinTheme {
        Surface {
            StyledList(
                items = persistentListOf(
                    StyledListItem(
                        AnnotatedString("Move your body"),
                        persistentListOf(
                            StyledListItem(AnnotatedString("wiggle")),
                            StyledListItem(AnnotatedString("stretch")),
                        )
                    ),
                    StyledListItem(AnnotatedString("Body double")),
                    StyledListItem(AnnotatedString("Do just the first 2 minutes")),
                    StyledListItem(AnnotatedString("Write in a journal")),
                ),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun StyledListPreview_Numbered() {
    DiswantinTheme {
        Surface {
            StyledList(
                items = persistentListOf(
                    StyledListItem(AnnotatedString("Tip the temperature")),
                    StyledListItem(AnnotatedString("Intense exercise")),
                    StyledListItem(AnnotatedString("Paced breathing")),
                    StyledListItem(AnnotatedString("Paired muscle relaxation")),
                ),
                type = StyledListType.Numbered,
            )
        }
    }
}
