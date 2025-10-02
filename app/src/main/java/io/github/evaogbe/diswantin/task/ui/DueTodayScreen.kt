package io.github.evaogbe.diswantin.task.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.loadstate.pagedListFooter
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeXl
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentSetOf
import java.time.LocalDate

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DueTodayTopBar(
    onSearchTask: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            TaskSearchTopBarButton(
                onClick = onSearchTask,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        modifier = modifier,
    )
}

@Composable
fun DueTodayScreen(
    onSelectTask: (Long) -> Unit,
    dueTodayViewModel: DueTodayViewModel = hiltViewModel(),
) {
    val taskPagingItems = dueTodayViewModel.taskPagingData.collectAsLazyPagingItems()

    when (taskPagingItems.loadState.refresh) {
        is LoadState.Loading -> PendingLayout()
        is LoadState.Error -> {
            LoadFailureLayout(
                message = stringResource(R.string.due_tasks_fetch_error),
                onRetry = taskPagingItems::retry,
            )
        }

        is LoadState.NotLoading -> {
            if (taskPagingItems.itemCount > 0) {
                DueTodayLayout(taskItems = taskPagingItems, onSelectTask = onSelectTask)
            } else {
                EmptyDueTodayLayout()
            }
        }
    }
}

@Composable
fun DueTodayLayout(taskItems: LazyPagingItems<DueTaskUiState>, onSelectTask: (Long) -> Unit) {
    DueTodayLayout(
        taskItems = {
            items(
                taskItems.itemCount,
                key = taskItems.itemKey(DueTaskUiState::id),
            ) { index ->
                val task = taskItems[index]!!
                DueTaskItem(task = task, onSelectTask = onSelectTask)
                HorizontalDivider()
            }

            pagedListFooter(
                pagingItems = taskItems,
                errorMessage = { Text(stringResource(R.string.due_tasks_fetch_error)) },
            )
        },
    )
}

@Composable
fun DueTodayLayout(taskItems: LazyListScope.() -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth(),
        ) {
            taskItems()
        }
    }
}

@Composable
private fun DueTaskItem(task: DueTaskUiState, onSelectTask: (Long) -> Unit) {
    ListItem(
        headlineContent = { Text(text = task.name) },
        modifier = Modifier.clickable { onSelectTask(task.id) },
        supportingContent = if (task.recurrence != null) {
            { Text(text = taskRecurrenceText(task.recurrence)) }
        } else null,
    )
}

@Composable
fun EmptyDueTodayLayout(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpaceMd)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.calendar_check_24px),
                contentDescription = null,
                modifier = Modifier.size(IconSizeXl)
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.due_tasks_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun DueTodayScreen_Present() {
    val taskItems = listOf(
        DueTaskUiState(
            id = 1L,
            name = "Brush teeth",
            recurrence = null,
        ),
        DueTaskUiState(
            id = 2L,
            name = "Shower",
            recurrence = TaskRecurrenceUiState(
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(6),
                type = RecurrenceType.DayOfMonth,
                step = 2,
                weekdays = persistentSetOf(),
            ),
        ),
        DueTaskUiState(
            id = 3L,
            name = "Eat breakfast",
            recurrence = TaskRecurrenceUiState(
                startDate = LocalDate.now(),
                endDate = null,
                type = RecurrenceType.Day,
                step = 1,
                weekdays = persistentSetOf(),
            ),
        ),
    )

    DiswantinTheme {
        Scaffold(
            topBar = {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DueTodayTopBar(
                            onSearchTask = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                        )
                    }
                }
            }
        ) { innerPadding ->
            DueTodayLayout(
                taskItems = {
                    items(taskItems, key = DueTaskUiState::id) { task ->
                        DueTaskItem(task = task, onSelectTask = {})
                        HorizontalDivider()
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun DueTodayScreen_Empty() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DueTodayTopBar(
                            onSearchTask = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility,
                        )
                    }
                }
            }
        ) { innerPadding ->
            EmptyDueTodayLayout(modifier = Modifier.padding(innerPadding))
        }
    }
}
