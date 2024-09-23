package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.AutocompleteField
import io.github.evaogbe.diswantin.ui.components.ClearableLayout
import io.github.evaogbe.diswantin.ui.components.DiswantinDatePickerDialog
import io.github.evaogbe.diswantin.ui.components.DiswantinTimePickerDialog
import io.github.evaogbe.diswantin.ui.components.EditFieldButton
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.components.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormTopBar(
    uiState: TaskFormTopBarState,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = if (uiState.isNew) {
                    stringResource(R.string.task_form_title_new)
                } else {
                    stringResource(R.string.task_form_title_edit)
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
fun TaskFormScreen(
    onPopBackStack: () -> Unit,
    setTopBarState: (TaskFormTopBarState) -> Unit,
    topBarAction: TaskFormTopBarAction?,
    topBarActionHandled: () -> Unit,
    setUserMessage: (String) -> Unit,
    onSelectCategoryType: (String) -> Unit,
    onEditRecurrence: () -> Unit,
    taskFormViewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by taskFormViewModel.uiState.collectAsStateWithLifecycle()
    val isNew = taskFormViewModel.isNew
    val nameInput = taskFormViewModel.nameInput
    val resources = LocalContext.current.resources

    if (uiState is TaskFormUiState.Saved) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    LaunchedEffect(setTopBarState, uiState, nameInput, isNew) {
        setTopBarState(
            TaskFormTopBarState(
                isNew = isNew,
                showSave = isNew || uiState is TaskFormUiState.Success,
                saveEnabled = nameInput.isNotBlank(),
            )
        )
    }

    LaunchedEffect(topBarAction, taskFormViewModel) {
        when (topBarAction) {
            null -> {}
            TaskFormTopBarAction.Save -> {
                taskFormViewModel.saveTask()
                topBarActionHandled()
            }
        }
    }

    (uiState as? TaskFormUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, setUserMessage) {
            setUserMessage(resources.getString(message))
            taskFormViewModel.userMessageShown()
        }
    }

    when (val state = uiState) {
        is TaskFormUiState.Pending,
        is TaskFormUiState.Saved -> {
            PendingLayout()
        }

        is TaskFormUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_form_fetch_error))
        }

        is TaskFormUiState.Success -> {
            TaskFormLayout(
                isNew = taskFormViewModel.isNew,
                uiState = state,
                name = nameInput,
                onNameChange = taskFormViewModel::updateNameInput,
                onSelectCategoryType = onSelectCategoryType,
                onDeadlineDateChange = taskFormViewModel::updateDeadlineDate,
                onDeadlineTimeChange = taskFormViewModel::updateDeadlineTime,
                onScheduledDateChange = taskFormViewModel::updateScheduledDate,
                onScheduledTimeChange = taskFormViewModel::updateScheduledTime,
                onEditRecurrence = onEditRecurrence,
                onClearRecurrence = { taskFormViewModel.updateRecurrence(null) },
                onParentTaskChange = taskFormViewModel::updateParentTask,
                onTaskSearch = taskFormViewModel::searchTasks,
            )
        }
    }
}

enum class FieldDialogType {
    DeadlineDate, DeadlineTime, ScheduledDate, ScheduledTime
}

@Composable
fun TaskFormLayout(
    isNew: Boolean,
    uiState: TaskFormUiState.Success,
    name: String,
    onNameChange: (String) -> Unit,
    onSelectCategoryType: (String) -> Unit,
    onDeadlineDateChange: (LocalDate?) -> Unit,
    onDeadlineTimeChange: (LocalTime?) -> Unit,
    onScheduledDateChange: (LocalDate?) -> Unit,
    onScheduledTimeChange: (LocalTime?) -> Unit,
    onEditRecurrence: () -> Unit,
    onClearRecurrence: () -> Unit,
    onParentTaskChange: (Task?) -> Unit,
    onTaskSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogType by rememberSaveable {
        mutableStateOf<FieldDialogType?>(null)
    }
    val parentTask = uiState.parentTask
    val (parentTaskQuery, setParentTaskQuery) = rememberSaveable(parentTask) {
        mutableStateOf(parentTask?.name ?: "")
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            if (uiState.hasSaveError) {
                SelectionContainer {
                    Text(
                        text = if (isNew) {
                            stringResource(R.string.task_form_save_error_new)
                        } else {
                            stringResource(R.string.task_form_save_error_edit)
                        },
                        color = colorScheme.error,
                        style = typography.titleSmall,
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name_label)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )

            if (isNew) {
                FormTypeButtonGroup(
                    selectedIndex = 0,
                    onSelect = {
                        if (it == 1) {
                            onSelectCategoryType(name)
                        }
                    },
                )
            }

            when {
                uiState.deadlineDate != null || uiState.deadlineTime != null -> {
                    if (uiState.deadlineDate == null) {
                        TextButtonWithIcon(
                            onClick = { dialogType = FieldDialogType.DeadlineDate },
                            painter = painterResource(R.drawable.baseline_schedule_24),
                            text = stringResource(R.string.add_deadline_date_button),
                        )
                    } else {
                        Column {
                            Text(
                                text = stringResource(R.string.deadline_date_label),
                                style = typography.bodyLarge,
                            )
                            Spacer(Modifier.size(SpaceSm))
                            ClearableLayout(
                                onClear = { onDeadlineDateChange(null) },
                                invert = false,
                            ) {
                                EditFieldButton(
                                    onClick = { dialogType = FieldDialogType.DeadlineDate },
                                    text = uiState.deadlineDate.format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                    ),
                                )
                            }
                        }
                    }

                    if (uiState.deadlineTime == null) {
                        TextButtonWithIcon(
                            onClick = { dialogType = FieldDialogType.DeadlineTime },
                            painter = painterResource(R.drawable.baseline_schedule_24),
                            text = stringResource(R.string.add_deadline_time_button),
                        )
                    } else {
                        Column {
                            Text(
                                text = stringResource(R.string.deadline_time_label),
                                style = typography.bodyLarge,
                            )
                            Spacer(Modifier.size(SpaceSm))
                            ClearableLayout(
                                onClear = { onDeadlineTimeChange(null) },
                                invert = false,
                            ) {
                                EditFieldButton(
                                    onClick = { dialogType = FieldDialogType.DeadlineTime },
                                    text = uiState.deadlineTime.format(
                                        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                    ),
                                )
                            }
                        }
                    }
                }

                uiState.scheduledTime != null -> {
                    if (uiState.scheduledDate == null) {
                        TextButtonWithIcon(
                            onClick = { dialogType = FieldDialogType.ScheduledDate },
                            painter = painterResource(R.drawable.baseline_schedule_24),
                            text = stringResource(R.string.add_scheduled_date_button),
                        )
                    } else {
                        Column {
                            Text(
                                text = stringResource(R.string.scheduled_date_label),
                                style = typography.bodyLarge,
                            )
                            Spacer(Modifier.size(SpaceSm))
                            ClearableLayout(
                                onClear = { onScheduledDateChange(null) },
                                invert = false,
                            ) {
                                EditFieldButton(
                                    onClick = { dialogType = FieldDialogType.ScheduledDate },
                                    text = uiState.scheduledDate.format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                    ),
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.scheduled_time_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(onClear = { onScheduledTimeChange(null) }, invert = false) {
                            EditFieldButton(
                                onClick = { dialogType = FieldDialogType.ScheduledTime },
                                text = uiState.scheduledTime.format(
                                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                ),
                            )
                        }
                    }
                }

                else -> {
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.DeadlineDate },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_deadline_date_button),
                    )
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.DeadlineTime },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_deadline_time_button),
                    )
                    TextButtonWithIcon(
                        onClick = {
                            dialogType = if (uiState.recurrence == null) {
                                FieldDialogType.ScheduledDate
                            } else {
                                FieldDialogType.ScheduledTime
                            }
                        },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_scheduled_at_button),
                    )
                }
            }

            if (uiState.recurrence == null) {
                TextButtonWithIcon(
                    onClick = onEditRecurrence,
                    imageVector = Icons.Default.Refresh,
                    text = stringResource(R.string.add_recurrence_button),
                )
            } else {
                Column {
                    Text(
                        text = stringResource(R.string.recurrence_label),
                        style = typography.bodyLarge,
                    )
                    Spacer(Modifier.size(SpaceSm))
                    ClearableLayout(onClear = onClearRecurrence, invert = false) {
                        EditFieldButton(
                            onClick = onEditRecurrence,
                            text = taskRecurrenceText(uiState.recurrence),
                        )
                    }
                }
            }

            if (uiState.showParentTaskField) {
                if (parentTask == null) {
                    AutocompleteField(
                        query = parentTaskQuery,
                        onQueryChange = setParentTaskQuery,
                        label = { Text(stringResource(R.string.parent_task_label)) },
                        onSearch = onTaskSearch,
                        options = uiState.parentTaskOptions,
                        formatOption = Task::name,
                        onSelectOption = onParentTaskChange,
                        autoFocus = false,
                    )
                } else {
                    var isEditingParent by rememberSaveable { mutableStateOf(false) }

                    if (isEditingParent) {
                        ClearableLayout(onClear = { onParentTaskChange(null) }, invert = true) {
                            AutocompleteField(
                                query = parentTaskQuery,
                                onQueryChange = setParentTaskQuery,
                                label = { Text(stringResource(R.string.parent_task_label)) },
                                onSearch = onTaskSearch,
                                options = uiState.parentTaskOptions,
                                formatOption = Task::name,
                                onSelectOption = {
                                    onParentTaskChange(it)
                                    isEditingParent = false
                                },
                                autoFocus = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = stringResource(R.string.parent_task_label),
                                style = typography.bodyLarge,
                            )
                            Spacer(Modifier.size(SpaceSm))
                            ClearableLayout(
                                onClear = { onParentTaskChange(null) },
                                invert = false,
                            ) {
                                EditFieldButton(
                                    onClick = { isEditingParent = true },
                                    text = parentTask.name,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (dialogType) {
        null -> {}
        FieldDialogType.DeadlineDate -> {
            DiswantinDatePickerDialog(
                onDismissRequest = { dialogType = null },
                dateTime = uiState.deadlineDate,
                onSelectDateTime = {
                    if (it != null) {
                        onDeadlineDateChange(it)
                    }
                    dialogType = null
                },
            )
        }

        FieldDialogType.DeadlineTime -> {
            DiswantinTimePickerDialog(
                onDismissRequest = { dialogType = null },
                time = uiState.deadlineTime,
                onSelectTime = {
                    onDeadlineTimeChange(it)
                    dialogType = null
                },
            )
        }

        FieldDialogType.ScheduledDate -> {
            DiswantinDatePickerDialog(
                onDismissRequest = { dialogType = null },
                dateTime = uiState.scheduledDate,
                onSelectDateTime = {
                    if (it != null) {
                        onScheduledDateChange(it)
                    }
                    dialogType = null
                },
            )
        }

        FieldDialogType.ScheduledTime -> {
            DiswantinTimePickerDialog(
                onDismissRequest = { dialogType = null },
                time = uiState.scheduledTime,
                onSelectTime = {
                    onScheduledTimeChange(it)
                    dialogType = null
                },
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormScreenPreview_New() {
    DiswantinTheme {
        Scaffold(topBar = {
            TaskFormTopBar(
                uiState = TaskFormTopBarState(isNew = true, showSave = true, saveEnabled = false),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TaskFormLayout(
                isNew = true,
                uiState = TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                ),
                name = "",
                onNameChange = {},
                onSelectCategoryType = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onParentTaskChange = {},
                onTaskSearch = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormScreenPreview_Edit() {
    DiswantinTheme {
        Scaffold(topBar = {
            TaskFormTopBar(
                uiState = TaskFormTopBarState(isNew = false, showSave = true, saveEnabled = true),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TaskFormLayout(
                isNew = false,
                uiState = TaskFormUiState.Success(
                    deadlineDate = LocalDate.now(),
                    deadlineTime = LocalTime.now(),
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = TaskRecurrenceUiState(
                        start = LocalDate.now(),
                        type = RecurrenceType.Day,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = Locale.getDefault(),
                    ),
                    showParentTaskField = true,
                    parentTask = Task(id = 1L, createdAt = Instant.now(), name = "Brush teeth"),
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                ),
                name = "Shower",
                onNameChange = {},
                onSelectCategoryType = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onParentTaskChange = {},
                onTaskSearch = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormLayoutPreview_ScheduledAt() {
    DiswantinTheme {
        Surface {
            TaskFormLayout(
                isNew = true,
                uiState = TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = LocalDate.now(),
                    scheduledTime = LocalTime.now(),
                    recurrence = null,
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                ),
                name = "Shower",
                onNameChange = {},
                onSelectCategoryType = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onParentTaskChange = {},
                onTaskSearch = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormLayoutPreview_ScheduledTime() {
    DiswantinTheme {
        Surface {
            TaskFormLayout(
                isNew = false,
                uiState = TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = LocalTime.now(),
                    recurrence = TaskRecurrenceUiState(
                        start = LocalDate.now(),
                        type = RecurrenceType.Week,
                        step = 2,
                        weekdays = persistentSetOf(LocalDate.now().dayOfWeek),
                        locale = Locale.getDefault(),
                    ),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                ),
                name = "",
                onNameChange = {},
                onSelectCategoryType = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onParentTaskChange = {},
                onTaskSearch = {},
            )
        }
    }
}
