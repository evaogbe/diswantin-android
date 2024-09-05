package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoryDetailTopBar(
    uiState: TaskCategoryDetailTopBarState,
    onBackClick: () -> Unit,
    onEditCategory: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back_button),
                )
            }
        },
        actions = {
            if (uiState.categoryId != null) {
                IconButton(onClick = { onEditCategory(uiState.categoryId) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_button),
                    )
                }

                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_actions_button),
                    )
                }

                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_button)) },
                        onClick = uiState.onDeleteCategory,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                    )
                }
            }
        }
    )
}

@Composable
fun TaskCategoryDetailScreen(
    onPopBackStack: () -> Unit,
    setTopBarState: (TaskCategoryDetailTopBarState) -> Unit,
    setUserMessage: (String) -> Unit,
    onSelectTask: (Long) -> Unit,
    taskCategoryDetailViewModel: TaskCategoryDetailViewModel = hiltViewModel(),
) {
    val uiState by taskCategoryDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources

    if (uiState is TaskCategoryDetailUiState.Deleted) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    LaunchedEffect(setTopBarState, uiState, taskCategoryDetailViewModel) {
        setTopBarState(
            TaskCategoryDetailTopBarState(
                categoryId = (uiState as? TaskCategoryDetailUiState.Success)?.category?.id,
                onDeleteCategory = taskCategoryDetailViewModel::deleteCategory,
            )
        )
    }

    (uiState as? TaskCategoryDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(resources.getString(message))
            taskCategoryDetailViewModel.userMessageShown()
        }
    }

    when (val state = uiState) {
        is TaskCategoryDetailUiState.Pending,
        is TaskCategoryDetailUiState.Deleted -> {
            PendingLayout()
        }

        is TaskCategoryDetailUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_category_detail_fetch_error))
        }

        is TaskCategoryDetailUiState.Success -> {
            TaskCategoryDetailLayout(uiState = state, onSelectTask = onSelectTask)
        }
    }
}

@Composable
fun TaskCategoryDetailLayout(
    uiState: TaskCategoryDetailUiState.Success,
    onSelectTask: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd),
        ) {
            item {
                SelectionContainer(modifier = Modifier.padding(horizontal = SpaceMd)) {
                    Text(text = uiState.category.name, style = typography.displaySmall)
                }
            }

            if (uiState.tasks.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.tasks_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall,
                    )
                }

                items(uiState.tasks, key = TaskItemState::id) { task ->
                    ListItem(
                        headlineContent = {
                            if (task.isDone) {
                                Text(
                                    text = task.name,
                                    modifier = Modifier.semantics {
                                        contentDescription =
                                            resources.getString(R.string.task_name_done, task.name)
                                    },
                                    textDecoration = TextDecoration.LineThrough,
                                )
                            } else {
                                Text(text = task.name)
                            }
                        },
                        modifier = Modifier.clickable { onSelectTask(task.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryDetailScreenPreview() {
    DiswantinTheme {
        Scaffold(topBar = {
            TaskCategoryDetailTopBar(
                uiState = TaskCategoryDetailTopBarState(categoryId = 1L, onDeleteCategory = {}),
                onBackClick = {},
                onEditCategory = {},
            )
        }) { innerPadding ->
            TaskCategoryDetailLayout(
                uiState = TaskCategoryDetailUiState.Success(
                    category = TaskCategory(name = "Morning Routine"),
                    tasks = persistentListOf(
                        TaskItemState(
                            id = 1L,
                            name = "Brush teeth",
                            recurring = false,
                            isDone = true,
                        ),
                        TaskItemState(id = 2L, name = "Shower", recurring = false, isDone = false),
                        TaskItemState(
                            id = 3L,
                            name = "Eat breakfast",
                            recurring = false,
                            isDone = false,
                        ),
                    ),
                    userMessage = null,
                ),
                onSelectTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
