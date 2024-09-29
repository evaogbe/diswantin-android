package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.ui.components.ButtonWithIcon
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.components.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoryListTopBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
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
fun TaskCategoryListScreen(
    onAddCategory: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    taskCategoryListViewModel: TaskCategoryListViewModel = hiltViewModel(),
) {
    val categoryPagingItems =
        taskCategoryListViewModel.categoryPagingData.collectAsLazyPagingItems()

    when (categoryPagingItems.loadState.refresh) {
        is LoadState.Loading -> PendingLayout()
        is LoadState.Error -> {
            LoadFailureLayout(
                message = stringResource(R.string.task_category_list_fetch_error),
                onRetry = categoryPagingItems::retry,
            )
        }

        is LoadState.NotLoading -> {
            if (categoryPagingItems.itemCount > 0) {
                TaskCategoryListLayout(
                    categoryItems = categoryPagingItems,
                    onSelectCategory = { onSelectCategory(it.id) },
                )
            } else {
                EmptyTaskCategoryListLayout(onAddCategory = onAddCategory)
            }
        }
    }
}

@Composable
fun TaskCategoryListLayout(
    categoryItems: LazyPagingItems<TaskCategory>,
    onSelectCategory: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            items(categoryItems.itemCount, key = categoryItems.itemKey(TaskCategory::id)) { index ->
                val category = categoryItems[index]!!
                ListItem(
                    headlineContent = { Text(text = category.name) },
                    modifier = Modifier.clickable { onSelectCategory(category) },
                )
                HorizontalDivider()
            }

            when (categoryItems.loadState.append) {
                is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpaceSm),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }

                is LoadState.Error -> {
                    item {
                        ListItem(
                            headlineContent = {
                                Text(stringResource(R.string.task_category_list_fetch_error))
                            },
                            supportingContent = {
                                TextButtonWithIcon(
                                    onClick = { categoryItems.retry() },
                                    imageVector = Icons.Default.Refresh,
                                    text = stringResource(R.string.retry_button),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = colorScheme.error,
                                    ),
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = colorScheme.errorContainer,
                                headlineColor = colorScheme.onErrorContainer,
                            ),
                        )
                    }
                }

                is LoadState.NotLoading -> {}
            }
        }
    }
}

@Composable
fun EmptyTaskCategoryListLayout(onAddCategory: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.list_alt_add_24px),
                contentDescription = null,
                modifier = Modifier.size(IconSizeLg),
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.task_category_list_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge,
            )
            Spacer(Modifier.size(SpaceLg))
            ButtonWithIcon(
                onClick = onAddCategory,
                imageVector = Icons.Default.Add,
                text = stringResource(R.string.add_task_category_button),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryListScreenPreview_Present() {
    val categoryItems = flowOf(
        PagingData.from(
            listOf(
                TaskCategory(id = 1L, name = "Morning routine"),
                TaskCategory(id = 2L, name = "Work"),
                TaskCategory(id = 3L, name = "Bedtime routine"),
            )
        )
    ).collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(topBar = { TaskCategoryListTopBar(onSearch = {}) }) { innerPadding ->
            TaskCategoryListLayout(
                categoryItems = categoryItems,
                onSelectCategory = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryListScreenPreview_Empty() {
    DiswantinTheme {
        Scaffold(topBar = { TaskCategoryListTopBar(onSearch = {}) }) { innerPadding ->
            EmptyTaskCategoryListLayout(
                onAddCategory = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskCategoryListLayout_Loading() {
    val categoryItems = flowOf(
        PagingData.from(
            listOf(
                TaskCategory(id = 1L, name = "Morning routine"),
                TaskCategory(id = 2L, name = "Work"),
                TaskCategory(id = 3L, name = "Bedtime routine"),
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
            TaskCategoryListLayout(categoryItems = categoryItems, onSelectCategory = {})
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryListLayout_Error() {
    val categoryItems = flowOf(
        PagingData.from(
            listOf(
                TaskCategory(id = 1L, name = "Morning routine"),
                TaskCategory(id = 2L, name = "Work"),
                TaskCategory(id = 3L, name = "Bedtime routine"),
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
            TaskCategoryListLayout(categoryItems = categoryItems, onSelectCategory = {})
        }
    }
}
