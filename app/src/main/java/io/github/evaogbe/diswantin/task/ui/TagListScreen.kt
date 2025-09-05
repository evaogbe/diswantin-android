package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.ui.button.ButtonWithIcon
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.loadstate.pagedListFooter
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarHandler
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeXl
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagListTopBar(modifier: Modifier = Modifier) {
    TopAppBar(title = {}, modifier = modifier)
}

@Composable
fun TagListScreen(
    onSelectTag: (Long) -> Unit,
    showSnackbar: SnackbarHandler,
    fabClicked: Boolean,
    fabClickHandled: () -> Unit,
    tagListViewModel: TagListViewModel = hiltViewModel(),
) {
    val currentShowSnackbar by rememberUpdatedState(showSnackbar)
    val tagPagingItems = tagListViewModel.tagPagingData.collectAsLazyPagingItems()
    val userMessage by tagListViewModel.userMessage.collectAsStateWithLifecycle()
    var showBottomSheet by rememberSaveable(fabClicked) { mutableStateOf(fabClicked) }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)

    when (tagPagingItems.loadState.refresh) {
        is LoadState.Loading -> PendingLayout()
        is LoadState.Error -> {
            LoadFailureLayout(
                message = stringResource(R.string.tag_list_fetch_error),
                onRetry = tagPagingItems::retry,
            )
        }

        is LoadState.NotLoading -> {
            LaunchedEffect(userMessage) {
                when (userMessage) {
                    null -> {}
                    TagListUserMessage.CreateError -> {
                        currentShowSnackbar(
                            SnackbarState.create(
                                currentResources.getString(R.string.tag_form_save_error_new)
                            )
                        )
                        tagListViewModel.userMessageShown()
                    }
                }
            }

            if (tagPagingItems.itemCount > 0) {
                TagListLayout(
                    tagItems = tagPagingItems,
                    onSelectTag = { onSelectTag(it.id) },
                )
            } else {
                EmptyTagListLayout(onAddTag = { showBottomSheet = true })
            }

            if (showBottomSheet) {
                TagFormSheet(
                    initialName = "",
                    onDismiss = {
                        showBottomSheet = false
                        fabClickHandled()
                    },
                    onSave = tagListViewModel::saveTag,
                )
            }
        }
    }
}

@Composable
fun TagListLayout(
    tagItems: LazyPagingItems<Tag>,
    onSelectTag: (Tag) -> Unit,
    modifier: Modifier = Modifier,
) {
    TagListLayout(
        tagItems = {
            items(tagItems.itemCount, key = tagItems.itemKey(Tag::id)) { index ->
                val tag = tagItems[index]!!
                ListItem(
                    headlineContent = { Text(text = tag.name) },
                    modifier = Modifier.clickable { onSelectTag(tag) },
                )
                HorizontalDivider()
            }

            pagedListFooter(
                pagingItems = tagItems,
                errorMessage = {
                    Text(stringResource(R.string.tag_list_fetch_error))
                },
            )
        },
        modifier = modifier,
    )
}

@Composable
fun TagListLayout(
    tagItems: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            tagItems()
        }
    }
}

@Composable
fun EmptyTagListLayout(onAddTag: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_new_label_24),
                contentDescription = null,
                modifier = Modifier.size(IconSizeXl),
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.tag_list_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge,
            )
            Spacer(Modifier.size(SpaceLg))
            ButtonWithIcon(
                onClick = onAddTag,
                painter = painterResource(R.drawable.baseline_add_24),
                text = stringResource(R.string.add_tag_button),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TagListScreenPreview_Present() {
    val tagItems = listOf(
        Tag(
            id = 1L,
            name = "Morning routine",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
        Tag(
            id = 2L,
            name = "Work",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
        Tag(
            id = 3L,
            name = "Bedtime routine",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        ),
    )

    DiswantinTheme {
        Scaffold(topBar = { TagListTopBar() }) { innerPadding ->
            TagListLayout(
                tagItems = {
                    items(tagItems, key = Tag::id) { tag ->
                        ListItem(headlineContent = { Text(text = tag.name) })
                        HorizontalDivider()
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TagListScreenPreview_Empty() {
    DiswantinTheme {
        Scaffold(topBar = { TagListTopBar() }) { innerPadding ->
            EmptyTagListLayout(
                onAddTag = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagListLayout_Loading() {
    val tagItems = flowOf(
        PagingData.from(
            listOf(
                Tag(
                    id = 1L,
                    name = "Morning routine",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = 2L,
                    name = "Work",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = 3L,
                    name = "Bedtime routine",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            ),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = false),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.Loading,
            ),
        ),
    ).collectAsLazyPagingItems()

    DiswantinTheme {
        Surface {
            TagListLayout(tagItems = tagItems, onSelectTag = {})
        }
    }
}

@Preview
@Composable
private fun TagListLayout_Error() {
    val tagItems = flowOf(
        PagingData.from(
            listOf(
                Tag(
                    id = 1L,
                    name = "Morning routine",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = 2L,
                    name = "Work",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
                Tag(
                    id = 3L,
                    name = "Bedtime routine",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            ),
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = false),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.Error(RuntimeException("Test")),
            ),
        ),
    ).collectAsLazyPagingItems()

    DiswantinTheme {
        Surface {
            TagListLayout(tagItems = tagItems, onSelectTag = {})
        }
    }
}
