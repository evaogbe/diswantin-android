package io.github.evaogbe.diswantin.task.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.AutocompleteField
import io.github.evaogbe.diswantin.ui.components.ClearableLayout
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.components.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
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
        is TaskCategoryFormUiState.Pending,
        is TaskCategoryFormUiState.Saved -> PendingLayout()

        is TaskCategoryFormUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_category_form_fetch_error))
        }

        is TaskCategoryFormUiState.Success -> {
            TaskCategoryFormLayout(
                isNew = taskCategoryFormViewModel.isNew,
                uiState = state,
                name = nameInput,
                onNameChange = taskCategoryFormViewModel::updateNameInput,
                onSelectTaskType = onSelectTaskType,
                onRemoveTask = taskCategoryFormViewModel::removeTask,
                onTaskSearch = taskCategoryFormViewModel::searchTasks,
                onSelectTaskOption = taskCategoryFormViewModel::setTask,
                startEditTask = taskCategoryFormViewModel::startEditTask,
                stopEditTask = taskCategoryFormViewModel::stopEditTask,
            )
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
    onSelectTaskType: (String) -> Unit,
    onRemoveTask: (Task) -> Unit,
    onTaskSearch: (String) -> Unit,
    onSelectTaskOption: (Int, Task) -> Unit,
    startEditTask: (Int) -> Unit,
    stopEditTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tasks = uiState.tasks
    val editingTaskIndex = uiState.editingTaskIndex
    var taskQuery by rememberSaveable(tasks, editingTaskIndex) {
        mutableStateOf(editingTaskIndex?.let(tasks::getOrNull)?.name ?: "")
    }

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

            itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                if (index == editingTaskIndex) {
                    Spacer(Modifier.size(SpaceSm))
                    ClearableLayout(onClear = stopEditTask, invert = true) {
                        AutocompleteField(
                            query = taskQuery,
                            onQueryChange = { taskQuery = it },
                            label = { Text(stringResource(R.string.task_name_label)) },
                            onSearch = onTaskSearch,
                            options = uiState.taskOptions,
                            formatOption = Task::name,
                            onSelectOption = { onSelectTaskOption(index, it) },
                            autoFocus = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.size(SpaceSm))
                } else {
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
                        ListItem(
                            headlineContent = { Text(text = task.name) },
                            modifier = Modifier.clickable { startEditTask(index) }
                        )
                    }
                }
                HorizontalDivider()
            }

            if (tasks.size < 20) {
                item {
                    if (editingTaskIndex == tasks.size) {
                        Spacer(Modifier.size(SpaceSm))
                        AutocompleteField(
                            query = taskQuery,
                            onQueryChange = { taskQuery = it },
                            label = { Text(stringResource(R.string.task_name_label)) },
                            onSearch = onTaskSearch,
                            options = uiState.taskOptions,
                            formatOption = Task::name,
                            onSelectOption = { onSelectTaskOption(tasks.size, it) },
                            autoFocus = editingTaskIndex > 0,
                        )
                    } else {
                        TextButtonWithIcon(
                            onClick = { startEditTask(tasks.size) },
                            imageVector = Icons.Default.Add,
                            text = stringResource(R.string.add_task_button),
                        )
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryFormScreenPreview_New() {
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
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                ),
                name = "",
                onNameChange = {},
                onSelectTaskType = {},
                onRemoveTask = {},
                onTaskSearch = {},
                onSelectTaskOption = { _, _ -> },
                startEditTask = {},
                stopEditTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryFormScreenPreview_Edit() {
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
                    tasks = persistentListOf(
                        Task(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                        ),
                        Task(
                            id = 2L,
                            createdAt = Instant.now(),
                            name = "Shower",
                        ),
                        Task(
                            id = 3L,
                            createdAt = Instant.now(),
                            name = "Eat breakfast",
                        ),
                    ),
                    editingTaskIndex = null,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                ),
                name = "Morning routine",
                onNameChange = {},
                onSelectTaskType = {},
                onRemoveTask = {},
                onTaskSearch = {},
                onSelectTaskOption = { _, _ -> },
                startEditTask = {},
                stopEditTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryFormLayoutPreview() {
    DiswantinTheme {
        Surface {
            TaskCategoryFormLayout(
                isNew = true,
                uiState = TaskCategoryFormUiState.Success(
                    tasks = persistentListOf(
                        Task(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                        )
                    ),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                ),
                name = "",
                onNameChange = {},
                onSelectTaskType = {},
                onRemoveTask = {},
                onTaskSearch = {},
                onSelectTaskOption = { _, _ -> },
                startEditTask = {},
                stopEditTask = {},
            )
        }
    }
}
