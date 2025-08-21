package io.github.evaogbe.diswantin.task.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.AutocompleteField
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.components.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.components.pagedListFooter
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoryFormTopBar(
    uiState: TaskCategoryFormTopBarState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = if (uiState.isNew) {
                    stringResource(R.string.task_category_form_title_new)
                } else {
                    stringResource(R.string.task_category_form_title_edit)
                }
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_button),
                )
            }
        },
        actions = {
            if (uiState.showSave) {
                Button(
                    onClick = onSave,
                    enabled = uiState.saveEnabled,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(stringResource(R.string.save_button))
                }
            }
        },
    )
}

@Composable
fun TaskCategoryFormScreen(
    onPopBackStack: () -> Unit,
    setTopBarState: (TaskCategoryFormTopBarState) -> Unit,
    topBarAction: TaskCategoryFormTopBarAction?,
    topBarActionHandled: () -> Unit,
    setUserMessage: (UserMessage) -> Unit,
    onSelectTaskType: (String) -> Unit,
    taskCategoryFormViewModel: TaskCategoryFormViewModel = hiltViewModel(),
) {
    val uiState by taskCategoryFormViewModel.uiState.collectAsStateWithLifecycle()
    val isNew = taskCategoryFormViewModel.isNew
    val nameInput = taskCategoryFormViewModel.nameInput
    val existingTaskPagingItems =
        taskCategoryFormViewModel.existingTaskPagingData.collectAsLazyPagingItems()

    if (uiState is TaskCategoryFormUiState.Saved) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    LaunchedEffect(setTopBarState, uiState, isNew, nameInput) {
        setTopBarState(
            TaskCategoryFormTopBarState(
                isNew = isNew,
                showSave = isNew || uiState is TaskCategoryFormUiState.Success,
                saveEnabled = nameInput.isNotBlank(),
            )
        )
    }

    LaunchedEffect(topBarAction, taskCategoryFormViewModel) {
        when (topBarAction) {
            null -> {}
            TaskCategoryFormTopBarAction.Save -> {
                taskCategoryFormViewModel.saveCategory()
                topBarActionHandled()
            }
        }
    }

    (uiState as? TaskCategoryFormUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(message)
            taskCategoryFormViewModel.userMessageShown()
        }
    }

    when (val state = uiState) {
        is TaskCategoryFormUiState.Pending, is TaskCategoryFormUiState.Saved -> PendingLayout()

        is TaskCategoryFormUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_category_form_fetch_error))
        }

        is TaskCategoryFormUiState.Success -> {
            when (existingTaskPagingItems.loadState.refresh) {
                is LoadState.Loading -> PendingLayout()

                is LoadState.Error -> {
                    LoadFailureLayout(
                        message = stringResource(R.string.task_category_form_fetch_error),
                    )
                }

                else -> {
                    TaskCategoryFormLayout(
                        isNew = taskCategoryFormViewModel.isNew,
                        uiState = state,
                        name = nameInput,
                        onNameChange = taskCategoryFormViewModel::updateNameInput,
                        existingTaskItems = existingTaskPagingItems,
                        onSelectTaskType = onSelectTaskType,
                        onRemoveTask = taskCategoryFormViewModel::removeTask,
                        onTaskSearch = taskCategoryFormViewModel::searchTasks,
                        onSelectTaskOption = taskCategoryFormViewModel::addTask,
                        startEditTask = taskCategoryFormViewModel::startEditTask,
                    )
                }
            }
        }
    }
}

const val TaskCategoryFormLayoutTestTag = "TaskCategoryFormLayoutTestTag"

@Composable
fun TaskCategoryFormLayout(
    isNew: Boolean,
    uiState: TaskCategoryFormUiState.Success,
    name: String,
    onNameChange: (String) -> Unit,
    existingTaskItems: LazyPagingItems<Task>,
    onSelectTaskType: (String) -> Unit,
    onRemoveTask: (Task) -> Unit,
    onTaskSearch: (String) -> Unit,
    onSelectTaskOption: (Task) -> Unit,
    startEditTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val newTasks = uiState.newTasks
    val isEditing = uiState.isEditing
    var taskQuery by rememberSaveable(isEditing) { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .testTag(TaskCategoryFormLayoutTestTag)
                .widthIn(max = ScreenLg)
                .padding(SpaceMd),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.name_label)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                )
            }

            if (isNew) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    FormTypeButtonGroup(
                        selectedIndex = 1,
                        onSelect = {
                            if (it == 0) {
                                onSelectTaskType(name)
                            }
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.size(SpaceLg))
                Text(stringResource(R.string.tasks_label), style = typography.titleMedium)
            }

            items(existingTaskItems.itemCount, key = existingTaskItems.itemKey(Task::id)) { index ->
                val task = existingTaskItems[index]!!
                TaskCategoryFormTaskItem(task = task, onRemoveTask = onRemoveTask)
                HorizontalDivider()
            }

            items(newTasks, key = Task::id) { task ->
                TaskCategoryFormTaskItem(task = task, onRemoveTask = onRemoveTask)
                HorizontalDivider()
            }

            pagedListFooter(
                pagingItems = existingTaskItems,
                errorMessage = {
                    Text(stringResource(R.string.task_category_form_fetch_tasks_error))
                },
            )

            if (existingTaskItems.itemCount + newTasks.size < 20) {
                item {
                    if (isEditing) {
                        Spacer(Modifier.size(SpaceSm))
                        AutocompleteField(
                            query = taskQuery,
                            onQueryChange = { taskQuery = it },
                            label = { Text(stringResource(R.string.task_name_label)) },
                            onSearch = onTaskSearch,
                            options = uiState.taskOptions,
                            formatOption = Task::name,
                            onSelectOption = onSelectTaskOption,
                            autoFocus = true,
                        )
                    } else {
                        TextButtonWithIcon(
                            onClick = startEditTask,
                            imageVector = Icons.Default.Add,
                            text = stringResource(R.string.add_task_button),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCategoryFormTaskItem(task: Task, onRemoveTask: (Task) -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) {
            onRemoveTask(task)
            true
        } else {
            false
        }
    })

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    colorScheme.error
                } else {
                    colorScheme.surfaceDim
                },
                label = "dismissColor",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
            )
        },
        enableDismissFromStartToEnd = false,
    ) {
        ListItem(headlineContent = { Text(text = task.name) })
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryFormScreenPreview_New() {
    val existingTaskItems = flowOf(PagingData.empty<Task>()).collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(topBar = {
            TaskCategoryFormTopBar(
                uiState = TaskCategoryFormTopBarState(
                    isNew = true,
                    showSave = true,
                    saveEnabled = false,
                ),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TaskCategoryFormLayout(
                isNew = true,
                uiState = TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                ),
                name = "",
                onNameChange = {},
                existingTaskItems = existingTaskItems,
                onSelectTaskType = {},
                onRemoveTask = {},
                onTaskSearch = {},
                onSelectTaskOption = {},
                startEditTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryFormScreenPreview_Edit() {
    val existingTaskItems = flowOf(
        PagingData.from(
            listOf(
                Task(id = 1L, createdAt = Instant.now(), name = "Go to work"),
                Task(id = 2L, createdAt = Instant.now(), name = "Do laundry"),
                Task(id = 3L, createdAt = Instant.now(), name = "Go shopping"),
            )
        )
    ).collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(topBar = {
            TaskCategoryFormTopBar(
                uiState = TaskCategoryFormTopBarState(
                    isNew = false,
                    showSave = true,
                    saveEnabled = true,
                ),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TaskCategoryFormLayout(
                isNew = false,
                uiState = TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(
                        Task(
                            id = 4L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                        ),
                        Task(
                            id = 5L,
                            createdAt = Instant.now(),
                            name = "Shower",
                        ),
                        Task(
                            id = 6L,
                            createdAt = Instant.now(),
                            name = "Eat breakfast",
                        ),
                    ),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                ),
                name = "Morning routine",
                onNameChange = {},
                existingTaskItems = existingTaskItems,
                onSelectTaskType = {},
                onRemoveTask = {},
                onTaskSearch = {},
                onSelectTaskOption = {},
                startEditTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryFormLayoutPreview() {
    val existingTaskItems = flowOf(PagingData.empty<Task>()).collectAsLazyPagingItems()

    DiswantinTheme {
        Surface {
            TaskCategoryFormLayout(
                isNew = true,
                uiState = TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(
                        Task(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                        )
                    ),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                ),
                name = "",
                onNameChange = {},
                existingTaskItems = existingTaskItems,
                onSelectTaskType = {},
                onRemoveTask = {},
                onTaskSearch = {},
                onSelectTaskOption = {},
                startEditTask = {},
            )
        }
    }
}
