package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.button.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.dialog.DiscardConfirmationDialog
import io.github.evaogbe.diswantin.ui.dialog.DiswantinDatePickerDialog
import io.github.evaogbe.diswantin.ui.dialog.DiswantinTimePickerDialog
import io.github.evaogbe.diswantin.ui.form.AutocompleteField
import io.github.evaogbe.diswantin.ui.form.ClearableLayout
import io.github.evaogbe.diswantin.ui.form.EditFieldButton
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
    setUserMessage: (UserMessage) -> Unit,
    initialName: String,
    onEditRecurrence: () -> Unit,
    taskFormViewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by taskFormViewModel.uiState.collectAsStateWithLifecycle()
    val isNew = taskFormViewModel.isNew
    var nameInput by rememberSaveable { mutableStateOf(initialName) }
    var noteInput by rememberSaveable { mutableStateOf("") }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState, nameInput, isNew) {
        setTopBarState(
            TaskFormTopBarState(
                isNew = isNew,
                showSave = isNew || uiState is TaskFormUiState.Success,
                saveEnabled = nameInput.isNotBlank(),
            )
        )
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TaskFormTopBarAction.Save -> {
                taskFormViewModel.saveTask()
                topBarActionHandled()
            }

            TaskFormTopBarAction.Close -> {
                if ((uiState as? TaskFormUiState.Success)?.changed == true) {
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
            taskFormViewModel.updateName(it)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { noteInput }.distinctUntilChanged().collectLatest {
            taskFormViewModel.updateNote(it)
        }
    }

    when (val state = uiState) {
        is TaskFormUiState.Pending -> {
            PendingLayout()
        }

        is TaskFormUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_form_fetch_error))
        }

        is TaskFormUiState.Saved -> {
            LaunchedEffect(Unit) {
                onPopBackStack()
            }

            PendingLayout()
        }

        is TaskFormUiState.Success -> {
            LaunchedEffect(state.name) {
                if (state.name != nameInput) {
                    nameInput = state.name
                }
            }

            LaunchedEffect(state.note) {
                if (state.note != noteInput) {
                    noteInput = state.note
                }
            }

            LaunchedEffect(state.userMessage) {
                if (state.userMessage != null) {
                    setUserMessage(state.userMessage)
                    taskFormViewModel.userMessageShown()
                }
            }

            TaskFormLayout(
                uiState = state,
                name = nameInput,
                onNameChange = { nameInput = it },
                note = noteInput,
                onNoteChange = { noteInput = it },
                onDeadlineDateChange = taskFormViewModel::updateDeadlineDate,
                onDeadlineTimeChange = taskFormViewModel::updateDeadlineTime,
                onStartAfterDateChange = taskFormViewModel::updateStartAfterDate,
                onStartAfterTimeChange = taskFormViewModel::updateStartAfterTime,
                onScheduledDateChange = taskFormViewModel::updateScheduledDate,
                onScheduledTimeChange = taskFormViewModel::updateScheduledTime,
                onEditRecurrence = onEditRecurrence,
                onClearRecurrence = { taskFormViewModel.updateRecurrence(null) },
                onAddTag = taskFormViewModel::addTag,
                onRemoveTag = taskFormViewModel::removeTag,
                startEditTag = taskFormViewModel::startEditTag,
                onTagSearch = taskFormViewModel::searchTags,
                onParentTaskChange = taskFormViewModel::updateParentTask,
                onTaskSearch = taskFormViewModel::searchParentTasks,
            )
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

enum class FieldDialogType {
    DeadlineDate, DeadlineTime, StartAfterDate, StartAfterTime, ScheduledDate, ScheduledTime
}

@Composable
fun TaskFormLayout(
    uiState: TaskFormUiState.Success,
    name: String,
    onNameChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onDeadlineDateChange: (LocalDate?) -> Unit,
    onDeadlineTimeChange: (LocalTime?) -> Unit,
    onStartAfterDateChange: (LocalDate?) -> Unit,
    onStartAfterTimeChange: (LocalTime?) -> Unit,
    onScheduledDateChange: (LocalDate?) -> Unit,
    onScheduledTimeChange: (LocalTime?) -> Unit,
    onEditRecurrence: () -> Unit,
    onClearRecurrence: () -> Unit,
    onAddTag: (Tag) -> Unit,
    onRemoveTag: (Tag) -> Unit,
    startEditTag: () -> Unit,
    onTagSearch: (String) -> Unit,
    onParentTaskChange: (Task?) -> Unit,
    onTaskSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogType by rememberSaveable { mutableStateOf<FieldDialogType?>(null) }
    var tagQuery by rememberSaveable(uiState.tagFieldState) { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name_label)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.note_label)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                minLines = 2,
            )

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
                    ClearableLayout(
                        onClear = onClearRecurrence,
                        iconContentDescription = stringResource(R.string.remove_button),
                    ) {
                        EditFieldButton(
                            onClick = onEditRecurrence,
                            text = taskRecurrenceText(uiState.recurrence),
                        )
                    }
                }
            }

            if (!uiState.isScheduled) {
                if (uiState.deadlineDate != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.deadline_date_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onDeadlineDateChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
                        ) {
                            EditFieldButton(
                                onClick = { dialogType = FieldDialogType.DeadlineDate },
                                text = uiState.deadlineDate.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                ),
                            )
                        }
                    }
                } else if (uiState.recurrence == null) {
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.DeadlineDate },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_deadline_date_button),
                    )
                }

                if (uiState.deadlineTime != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.deadline_time_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onDeadlineTimeChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
                        ) {
                            EditFieldButton(
                                onClick = { dialogType = FieldDialogType.DeadlineTime },
                                text = uiState.deadlineTime.format(
                                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                ),
                            )
                        }
                    }
                } else {
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.DeadlineTime },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_deadline_time_button),
                    )
                }

                if (uiState.startAfterDate != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.start_after_date_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onStartAfterDateChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
                        ) {
                            EditFieldButton(
                                onClick = { dialogType = FieldDialogType.StartAfterDate },
                                text = uiState.startAfterDate.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                ),
                            )
                        }
                    }
                } else if (uiState.recurrence == null) {
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.StartAfterDate },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_start_after_date_button),
                    )
                }

                if (uiState.startAfterTime != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.start_after_time_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onStartAfterTimeChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
                        ) {
                            EditFieldButton(
                                onClick = { dialogType = FieldDialogType.StartAfterTime },
                                text = uiState.startAfterTime.format(
                                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                ),
                            )
                        }
                    }
                } else {
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.StartAfterTime },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_start_after_time_button),
                    )
                }

                if (uiState.canSchedule) {
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
            } else {
                if (uiState.scheduledDate != null) {
                    Column {
                        Text(
                            text = stringResource(R.string.scheduled_date_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onScheduledDateChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
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

                if (uiState.scheduledTime == null) {
                    TextButtonWithIcon(
                        onClick = { dialogType = FieldDialogType.ScheduledTime },
                        painter = painterResource(R.drawable.baseline_schedule_24),
                        text = stringResource(R.string.add_scheduled_time_button),
                    )
                } else {
                    Column {
                        Text(
                            text = stringResource(R.string.scheduled_time_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onScheduledTimeChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
                        ) {
                            EditFieldButton(
                                onClick = { dialogType = FieldDialogType.ScheduledTime },
                                text = uiState.scheduledTime.format(
                                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                ),
                            )
                        }
                    }
                }
            }

            if (uiState.showParentTaskField) {
                SelectableAutocompleteField(
                    selectedOption = uiState.parentTask,
                    label = stringResource(R.string.parent_task_label),
                    onSearch = onTaskSearch,
                    options = uiState.parentTaskOptions,
                    formatOption = Task::name,
                    onSelectOption = onParentTaskChange,
                )
            }

            when (uiState.tagFieldState) {
                TagFieldState.Open -> {
                    Column {
                        Text(stringResource(R.string.tags_label), style = typography.titleMedium)

                        TagList(tags = uiState.tags, onRemoveTag = onRemoveTag)

                        AutocompleteField(
                            query = tagQuery,
                            onQueryChange = { tagQuery = it },
                            label = { Text(stringResource(R.string.tag_name_label)) },
                            onSearch = onTagSearch,
                            options = uiState.tagOptions,
                            formatOption = Tag::name,
                            onSelectOption = onAddTag,
                            autoFocus = true,
                        )
                    }
                }

                TagFieldState.Closed -> {
                    Column {
                        Text(stringResource(R.string.tags_label), style = typography.titleMedium)

                        TagList(tags = uiState.tags, onRemoveTag = onRemoveTag)

                        if (uiState.tags.size < 20) {
                            TextButtonWithIcon(
                                onClick = startEditTag,
                                imageVector = Icons.Default.Add,
                                text = stringResource(R.string.add_tag_button),
                            )
                        }
                    }
                }

                TagFieldState.Hidden -> {}
            }
        }
    }

    when (dialogType) {
        null -> {}
        FieldDialogType.DeadlineDate -> {
            DiswantinDatePickerDialog(
                onDismiss = { dialogType = null },
                date = uiState.deadlineDate,
                onSelectDate = onDeadlineDateChange,
            )
        }

        FieldDialogType.DeadlineTime -> {
            DiswantinTimePickerDialog(
                onDismiss = { dialogType = null },
                time = uiState.deadlineTime,
                onSelectTime = onDeadlineTimeChange,
            )
        }

        FieldDialogType.StartAfterDate -> {
            DiswantinDatePickerDialog(
                onDismiss = { dialogType = null },
                date = uiState.startAfterDate,
                onSelectDate = onStartAfterDateChange,
            )
        }

        FieldDialogType.StartAfterTime -> {
            DiswantinTimePickerDialog(
                onDismiss = { dialogType = null },
                time = uiState.startAfterTime,
                onSelectTime = onStartAfterTimeChange,
            )
        }

        FieldDialogType.ScheduledDate -> {
            DiswantinDatePickerDialog(
                onDismiss = { dialogType = null },
                date = uiState.scheduledDate,
                onSelectDate = onScheduledDateChange,
            )
        }

        FieldDialogType.ScheduledTime -> {
            DiswantinTimePickerDialog(
                onDismiss = { dialogType = null },
                time = uiState.scheduledTime,
                onSelectTime = onScheduledTimeChange,
            )
        }
    }
}

@Composable
fun <T : Any> SelectableAutocompleteField(
    selectedOption: T?,
    label: String,
    onSearch: (String) -> Unit,
    options: ImmutableList<T>,
    formatOption: (T) -> String,
    onSelectOption: (T?) -> Unit,
) {
    var query by rememberSaveable(selectedOption) {
        mutableStateOf(selectedOption?.let(formatOption).orEmpty())
    }

    if (selectedOption == null) {
        AutocompleteField(
            query = query,
            onQueryChange = { query = it },
            label = { Text(text = label) },
            onSearch = onSearch,
            options = options,
            formatOption = formatOption,
            onSelectOption = onSelectOption,
            autoFocus = false,
        )
    } else {
        var isEditing by rememberSaveable { mutableStateOf(false) }

        if (isEditing) {
            ClearableLayout(onClear = { isEditing = false }, invert = true) {
                AutocompleteField(
                    query = query,
                    onQueryChange = { query = it },
                    label = { Text(text = label) },
                    onSearch = onSearch,
                    options = options,
                    formatOption = formatOption,
                    onSelectOption = {
                        onSelectOption(it)
                        isEditing = false
                    },
                    autoFocus = false,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column {
                Text(text = label, style = typography.bodyLarge)
                Spacer(Modifier.size(SpaceSm))
                ClearableLayout(
                    onClear = { onSelectOption(null) },
                    iconContentDescription = stringResource(R.string.remove_button)
                ) {
                    EditFieldButton(
                        onClick = {
                            query = formatOption(selectedOption)
                            isEditing = true
                        },
                        text = formatOption(selectedOption),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

@Composable
fun TagList(tags: ImmutableList<Tag>, onRemoveTag: (Tag) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SpaceSm)) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { onRemoveTag(tag) },
                label = { Text(text = tag.name) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_button),
                        modifier = Modifier.size(InputChipDefaults.AvatarSize),
                    )
                },
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormScreenPreview_New() {
    val name = ""
    val note = ""

    DiswantinTheme {
        Scaffold(topBar = {
            TaskFormTopBar(
                uiState = TaskFormTopBarState(isNew = true, showSave = true, saveEnabled = false),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    name = name,
                    note = note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                ),
                name = name,
                onNameChange = {},
                note = note,
                onNoteChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onStartAfterDateChange = {},
                onStartAfterTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onAddTag = {},
                onRemoveTag = {},
                startEditTag = {},
                onTagSearch = {},
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
    val name = "Shower"
    val note = "Wash hair and deep condition before appointment at hair salon"

    DiswantinTheme {
        Scaffold(topBar = {
            TaskFormTopBar(
                uiState = TaskFormTopBarState(isNew = false, showSave = true, saveEnabled = true),
                onClose = {},
                onSave = {},
            )
        }) { innerPadding ->
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    name = name,
                    note = note,
                    recurrence = TaskRecurrenceUiState(
                        start = LocalDate.now(),
                        type = RecurrenceType.Day,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = Locale.getDefault(),
                    ),
                    deadlineDate = null,
                    deadlineTime = LocalTime.now().plusHours(1),
                    startAfterDate = null,
                    startAfterTime = LocalTime.now(),
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Closed,
                    tags = persistentListOf(
                        Tag(id = 1L, name = "morning routine"),
                        Tag(id = 2L, name = "hygiene"),
                        Tag(id = 3L, name = "low effort"),
                        Tag(id = 3L, name = "goal"),
                    ),
                    tagOptions = persistentListOf(),
                    showParentTaskField = true,
                    parentTask = Task(id = 1L, createdAt = Instant.now(), name = "Brush teeth"),
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                ),
                name = name,
                onNameChange = {},
                note = note,
                onNoteChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onStartAfterDateChange = {},
                onStartAfterTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onAddTag = {},
                onRemoveTag = {},
                startEditTag = {},
                onTagSearch = {},
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
    val name = "Shower"
    val note = ""

    DiswantinTheme {
        Surface {
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    name = name,
                    note = note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = LocalDate.now(),
                    scheduledTime = LocalTime.now(),
                    tagFieldState = TagFieldState.Closed,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                ),
                name = name,
                onNameChange = {},
                note = note,
                onNoteChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onStartAfterDateChange = {},
                onStartAfterTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onAddTag = {},
                onRemoveTag = {},
                startEditTag = {},
                onTagSearch = {},
                onParentTaskChange = {},
                onTaskSearch = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormLayoutPreview_ScheduledDate() {
    val name = ""
    val note = ""

    DiswantinTheme {
        Surface {
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    name = name,
                    note = note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = LocalDate.now(),
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                ),
                name = name,
                onNameChange = {},
                note = note,
                onNoteChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onStartAfterDateChange = {},
                onStartAfterTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onAddTag = {},
                onRemoveTag = {},
                startEditTag = {},
                onTagSearch = {},
                onParentTaskChange = {},
                onTaskSearch = {},
            )
        }
    }
}


@DevicePreviews
@Composable
private fun TaskFormLayoutPreview_EditingTag() {
    val name = ""
    val note = ""

    DiswantinTheme {
        Surface {
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    name = name,
                    note = note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Open,
                    tags = persistentListOf(Tag(id = 1L, name = "morning routine")),
                    tagOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                ),
                name = name,
                onNameChange = {},
                note = note,
                onNoteChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onStartAfterDateChange = {},
                onStartAfterTimeChange = {},
                onScheduledDateChange = {},
                onScheduledTimeChange = {},
                onEditRecurrence = {},
                onClearRecurrence = {},
                onAddTag = {},
                onRemoveTag = {},
                startEditTag = {},
                onTagSearch = {},
                onParentTaskChange = {},
                onTaskSearch = {},
            )
        }
    }
}
