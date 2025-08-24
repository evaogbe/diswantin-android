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
import androidx.compose.runtime.snapshotFlow
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
import io.github.evaogbe.diswantin.task.data.TaggedTask
import io.github.evaogbe.diswantin.ui.button.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.dialog.DiscardConfirmationDialog
import io.github.evaogbe.diswantin.ui.form.AutocompleteField
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.loadstate.pagedListFooter
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFormTopBar(
    uiState: TagFormTopBarState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = if (uiState.isNew) {
                    stringResource(R.string.tag_form_title_new)
                } else {
                    stringResource(R.string.tag_form_title_edit)
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
fun TagFormScreen(
    onPopBackStack: () -> Unit,
    setTopBarState: (TagFormTopBarState) -> Unit,
    topBarAction: TagFormTopBarAction?,
    topBarActionHandled: () -> Unit,
    setUserMessage: (UserMessage) -> Unit,
    initialName: String,
    onSelectTaskType: (String) -> Unit,
    tagFormViewModel: TagFormViewModel = hiltViewModel(),
) {
    val uiState by tagFormViewModel.uiState.collectAsStateWithLifecycle()
    val isNew = tagFormViewModel.isNew
    var nameInput by rememberSaveable { mutableStateOf(initialName) }
    val existingTaskPagingItems =
        tagFormViewModel.existingTaskPagingData.collectAsLazyPagingItems()
    var showDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState, isNew, nameInput) {
        setTopBarState(
            TagFormTopBarState(
                isNew = isNew,
                showSave = isNew || uiState is TagFormUiState.Success,
                saveEnabled = nameInput.isNotBlank(),
            )
        )
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TagFormTopBarAction.Save -> {
                tagFormViewModel.saveTag()
                topBarActionHandled()
            }

            TagFormTopBarAction.Close -> {
                if ((uiState as? TagFormUiState.Success)?.changed == true) {
                    showDialog = true
                } else {
                    onPopBackStack()
                }
                topBarActionHandled()
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { nameInput }.distinctUntilChanged().collectLatest {
            tagFormViewModel.updateName(it)
        }
    }

    when (val state = uiState) {
        is TagFormUiState.Pending -> PendingLayout()

        is TagFormUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.tag_form_fetch_error))
        }

        is TagFormUiState.Saved -> {
            LaunchedEffect(Unit) {
                onPopBackStack()
            }

            PendingLayout()
        }

        is TagFormUiState.Success -> {
            when (existingTaskPagingItems.loadState.refresh) {
                is LoadState.Loading -> PendingLayout()

                is LoadState.Error -> {
                    LoadFailureLayout(
                        message = stringResource(R.string.tag_form_fetch_error),
                    )
                }

                else -> {
                    LaunchedEffect(state.name) {
                        if (nameInput != state.name) {
                            nameInput = state.name
                        }
                    }

                    LaunchedEffect(state.userMessage) {
                        if (state.userMessage != null) {
                            setUserMessage(state.userMessage)
                            tagFormViewModel.userMessageShown()
                        }
                    }

                    TagFormLayout(
                        isNew = tagFormViewModel.isNew,
                        uiState = state,
                        name = nameInput,
                        onNameChange = { nameInput = it },
                        existingTaskItems = existingTaskPagingItems,
                        onSelectTaskType = onSelectTaskType,
                        onRemoveTask = tagFormViewModel::removeTask,
                        onTaskSearch = tagFormViewModel::searchTasks,
                        onSelectTaskOption = tagFormViewModel::addTask,
                        startEditTask = tagFormViewModel::startEditTask,
                    )
                }
            }
        }
    }

    if (showDialog) {
        DiscardConfirmationDialog(
            confirm = {
                showDialog = false
                onPopBackStack()
            },
            dismiss = { showDialog = false },
        )
    }
}

const val TagFormLayoutTestTag = "TagFormLayoutTestTag"

@Composable
fun TagFormLayout(
    isNew: Boolean,
    uiState: TagFormUiState.Success,
    name: String,
    onNameChange: (String) -> Unit,
    existingTaskItems: LazyPagingItems<TaggedTask>,
    onSelectTaskType: (String) -> Unit,
    onRemoveTask: (TaggedTask) -> Unit,
    onTaskSearch: (String) -> Unit,
    onSelectTaskOption: (TaggedTask) -> Unit,
    startEditTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val newTasks = uiState.newTasks
    val isEditing = uiState.isEditing
    var taskQuery by rememberSaveable(isEditing) { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .testTag(TagFormLayoutTestTag)
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

            items(
                existingTaskItems.itemCount,
                key = existingTaskItems.itemKey(TaggedTask::id)
            ) { index ->
                val task = existingTaskItems[index]!!
                TagFormTaskItem(task = task, onRemoveTask = onRemoveTask)
                HorizontalDivider()
            }

            items(newTasks, key = TaggedTask::id) { task ->
                TagFormTaskItem(task = task, onRemoveTask = onRemoveTask)
                HorizontalDivider()
            }

            pagedListFooter(
                pagingItems = existingTaskItems,
                errorMessage = {
                    Text(stringResource(R.string.tag_form_fetch_tasks_error))
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
                            formatOption = TaggedTask::name,
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
private fun TagFormTaskItem(task: TaggedTask, onRemoveTask: (TaggedTask) -> Unit) {
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
private fun TagFormScreenPreview_New() {
    val name = ""
    val existingTaskItems = flowOf(PagingData.empty<TaggedTask>()).collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(topBar = {
            TagFormTopBar(
                uiState = TagFormTopBarState(
                    isNew = true,
                    showSave = true,
                    saveEnabled = false,
                ),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TagFormLayout(
                isNew = true,
                uiState = TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                ),
                name = name,
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
private fun TagFormScreenPreview_Edit() {
    val name = "Morning routine"
    val existingTaskItems = flowOf(
        PagingData.from(
            listOf(
                TaggedTask(id = 1L, name = "Go to work", isTagged = true),
                TaggedTask(id = 2L, name = "Do laundry", isTagged = true),
                TaggedTask(id = 3L, name = "Go shopping", isTagged = true),
            )
        )
    ).collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(topBar = {
            TagFormTopBar(
                uiState = TagFormTopBarState(
                    isNew = false,
                    showSave = true,
                    saveEnabled = true,
                ),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TagFormLayout(
                isNew = false,
                uiState = TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(
                        TaggedTask(id = 4L, name = "Brush teeth", isTagged = false),
                        TaggedTask(id = 5L, name = "Shower", isTagged = false),
                        TaggedTask(id = 6L, name = "Eat breakfast", isTagged = false),
                    ),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                ),
                name = name,
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
private fun TagFormLayoutPreview() {
    val name = ""
    val existingTaskItems = flowOf(PagingData.empty<TaggedTask>()).collectAsLazyPagingItems()

    DiswantinTheme {
        Surface {
            TagFormLayout(
                isNew = true,
                uiState = TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(
                        TaggedTask(id = 1L, name = "Brush teeth", isTagged = false)
                    ),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                ),
                name = name,
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
