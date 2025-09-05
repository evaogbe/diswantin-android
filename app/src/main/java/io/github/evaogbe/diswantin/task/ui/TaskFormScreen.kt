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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
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
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarHandler
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
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
                    painterResource(R.drawable.baseline_close_24),
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
    showSnackbar: SnackbarHandler,
    onEditRecurrence: () -> Unit,
    onEditParent: (String) -> Unit,
    taskFormViewModel: TaskFormViewModel = hiltViewModel(),
) {
    val currentOnPopBackStack by rememberUpdatedState(onPopBackStack)
    val currentTopBarActionHandled by rememberUpdatedState(topBarActionHandled)
    val currentShowSnackbar by rememberUpdatedState(showSnackbar)
    val uiState by taskFormViewModel.uiState.collectAsStateWithLifecycle()
    val isSuccess by remember {
        derivedStateOf { uiState is TaskFormUiState.Success }
    }
    val changed by remember {
        derivedStateOf {
            (uiState as? TaskFormUiState.Success)?.changed == true
        }
    }
    val isNew = taskFormViewModel.isNew
    val name = rememberTextFieldState()
    val note = rememberTextFieldState()
    val tagQuery = rememberTextFieldState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)

    LaunchedEffect(name.text, isNew, isSuccess, setTopBarState) {
        setTopBarState(
            TaskFormTopBarState(
                isNew = isNew,
                showSave = isNew || isSuccess,
                saveEnabled = name.text.isNotBlank(),
            )
        )
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TaskFormTopBarAction.Save -> {
                taskFormViewModel.saveTask()
                currentTopBarActionHandled()
            }

            TaskFormTopBarAction.Close -> {
                if (changed) {
                    showDialog = true
                } else {
                    currentOnPopBackStack()
                }
                currentTopBarActionHandled()
            }
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
                currentOnPopBackStack()
            }

            PendingLayout()
        }

        is TaskFormUiState.Success -> {
            LaunchedEffect(state.userMessage) {
                when (state.userMessage) {
                    null -> {}
                    TaskFormUserMessage.FetchParentTaskError -> {
                        currentShowSnackbar(
                            SnackbarState.create(
                                currentResources.getString(R.string.task_form_fetch_parent_task_error)
                            )
                        )
                        taskFormViewModel.userMessageShown()
                    }

                    TaskFormUserMessage.FetchTagsError -> {
                        currentShowSnackbar(
                            SnackbarState.create(
                                currentResources.getString(R.string.task_form_fetch_tags_error)
                            )
                        )
                        taskFormViewModel.userMessageShown()
                    }

                    TaskFormUserMessage.SearchTagsError -> {
                        currentShowSnackbar(
                            SnackbarState.create(
                                currentResources.getString(R.string.search_tag_options_error)
                            )
                        )
                        taskFormViewModel.userMessageShown()
                    }

                    TaskFormUserMessage.CreateError -> {
                        currentShowSnackbar(
                            SnackbarState.create(
                                currentResources.getString(R.string.task_form_save_error_new)
                            )
                        )
                        taskFormViewModel.userMessageShown()
                    }

                    TaskFormUserMessage.EditError -> {
                        currentShowSnackbar(
                            SnackbarState.create(
                                currentResources.getString(R.string.task_form_save_error_edit)
                            )
                        )
                        taskFormViewModel.userMessageShown()
                    }
                }
            }

            LaunchedEffect(state.initialName) {
                if (state.initialName != name.text) {
                    name.setTextAndPlaceCursorAtEnd(state.initialName)
                }
            }

            LaunchedEffect(name) {
                snapshotFlow { name.text.toString() }.collect {
                    taskFormViewModel.updateName(it)
                }
            }

            LaunchedEffect(state.initialNote) {
                if (state.initialNote != note.text) {
                    note.setTextAndPlaceCursorAtEnd(state.initialNote)
                }
            }

            LaunchedEffect(note) {
                snapshotFlow { note.text.toString() }.collect {
                    taskFormViewModel.updateNote(it)
                }
            }

            LaunchedEffect(state.tagFieldState) {
                tagQuery.clearText()
            }

            TaskFormLayout(
                uiState = state,
                name = name,
                note = note,
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
                tagQuery = tagQuery,
                onTagSearch = taskFormViewModel::searchTags,
                onParentTaskChange = taskFormViewModel::updateParent,
                onEditParent = onEditParent,
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
    name: TextFieldState,
    note: TextFieldState,
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
    tagQuery: TextFieldState,
    onTagSearch: (String) -> Unit,
    onParentTaskChange: (ParentTask?) -> Unit,
    onEditParent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialogType by rememberSaveable { mutableStateOf<FieldDialogType?>(null) }
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd),
            verticalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            OutlinedTextField(
                state = name,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name_label)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )

            OutlinedTextField(
                state = note,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.note_label)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                lineLimits = TextFieldLineLimits.MultiLine(2),
            )

            if (uiState.recurrence == null) {
                TextButtonWithIcon(
                    onClick = onEditRecurrence,
                    painter = painterResource(R.drawable.outline_refresh_24),
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
                            modifier = Modifier.weight(1f, fill = false),
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
                                text = uiState.deadlineDate.format(dateFormatter),
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
                                text = uiState.deadlineTime.format(timeFormatter),
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
                                text = uiState.startAfterDate.format(dateFormatter),
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
                                text = uiState.startAfterTime.format(timeFormatter),
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
                                text = uiState.scheduledDate.format(dateFormatter),
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
                                text = uiState.scheduledTime.format(timeFormatter),
                            )
                        }
                    }
                }
            }

            if (uiState.showParentField) {
                if (uiState.parent == null) {
                    TextButtonWithIcon(
                        onClick = { onEditParent("") },
                        painter = painterResource(R.drawable.baseline_add_24),
                        text = stringResource(R.string.add_parent_task_button),
                    )
                } else {
                    Column {
                        Text(
                            text = stringResource(R.string.parent_task_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(
                            onClear = { onParentTaskChange(null) },
                            iconContentDescription = stringResource(R.string.remove_button),
                        ) {
                            EditFieldButton(
                                onClick = { onEditParent(uiState.parent.name) },
                                text = uiState.parent.name,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }

            when (uiState.tagFieldState) {
                TagFieldState.Open -> {
                    Column {
                        if (uiState.tags.isNotEmpty()) {
                            Text(
                                stringResource(R.string.tags_label),
                                style = typography.titleMedium,
                            )

                            TaskFormTagList(tags = uiState.tags, onRemoveTag = onRemoveTag)
                        }

                        AutocompleteField(
                            query = tagQuery,
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
                        if (uiState.tags.isNotEmpty()) {
                            Text(
                                stringResource(R.string.tags_label),
                                style = typography.titleMedium,
                            )

                            TaskFormTagList(tags = uiState.tags, onRemoveTag = onRemoveTag)
                        }

                        if (uiState.tags.size < Task.MAX_TAGS) {
                            TextButtonWithIcon(
                                onClick = startEditTag,
                                painter = painterResource(R.drawable.baseline_add_24),
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

const val TaskFormTagListTestTag = "TaskFormTagListTestTag"

@Composable
fun TaskFormTagList(tags: ImmutableList<Tag>, onRemoveTag: (Tag) -> Unit) {
    FlowRow(
        modifier = Modifier.testTag(TaskFormTagListTestTag),
        horizontalArrangement = Arrangement.spacedBy(SpaceSm),
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = { onRemoveTag(tag) },
                label = { Text(text = tag.name) },
                trailingIcon = {
                    Icon(
                        painterResource(R.drawable.baseline_close_24),
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
        Scaffold(
            topBar = {
                TaskFormTopBar(
                    uiState = TaskFormTopBarState(
                        isNew = true,
                        showSave = true,
                        saveEnabled = false,
                    ),
                    onClose = {},
                    onSave = {},
                )
            },
        ) { innerPadding ->
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    initialName = name,
                    initialNote = note,
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
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = null,
                ),
                name = TextFieldState(initialText = name),
                note = TextFieldState(initialText = note),
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
                tagQuery = TextFieldState(),
                onTagSearch = {},
                onParentTaskChange = {},
                onEditParent = {},
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
    val today = LocalDate.now()
    val currentTime = LocalTime.now()

    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskFormTopBar(
                    uiState = TaskFormTopBarState(
                        isNew = false,
                        showSave = true,
                        saveEnabled = true,
                    ),
                    onClose = {},
                    onSave = {},
                )
            },
        ) { innerPadding ->
            TaskFormLayout(
                uiState = TaskFormUiState.Success(
                    initialName = name,
                    initialNote = note,
                    recurrence = TaskRecurrenceUiState(
                        startDate = today,
                        endDate = today.plusYears(1),
                        type = RecurrenceType.WeekOfMonth,
                        step = 2,
                        weekdays = persistentSetOf(),
                        locale = Locale.getDefault(),
                    ),
                    deadlineDate = null,
                    deadlineTime = currentTime.plusHours(1),
                    startAfterDate = null,
                    startAfterTime = currentTime,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Closed,
                    tags = persistentListOf(
                        Tag(
                            id = 1L,
                            name = "goal",
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        ),
                        Tag(
                            id = 2L,
                            name = "hygiene",
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        ),
                        Tag(
                            id = 3L,
                            name = "low effort",
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        ),
                        Tag(
                            id = 4L,
                            name = "morning routine",
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        ),
                    ),
                    tagOptions = persistentListOf(),
                    showParentField = true,
                    parent = ParentTask(
                        id = 1L,
                        name = "Schedule appointment with primary care physician",
                    ),
                    changed = false,
                    userMessage = null,
                ),
                name = TextFieldState(initialText = name),
                note = TextFieldState(initialText = note),
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
                tagQuery = TextFieldState(),
                onTagSearch = {},
                onParentTaskChange = {},
                onEditParent = {},
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
                    initialName = name,
                    initialNote = note,
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
                    showParentField = true,
                    parent = null,
                    changed = true,
                    userMessage = null,
                ),
                name = TextFieldState(initialText = name),
                note = TextFieldState(initialText = note),
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
                tagQuery = TextFieldState(),
                onTagSearch = {},
                onParentTaskChange = {},
                onEditParent = {},
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
                    initialName = name,
                    initialNote = note,
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
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = null,
                ),
                name = TextFieldState(initialText = name),
                note = TextFieldState(initialText = note),
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
                tagQuery = TextFieldState(),
                onTagSearch = {},
                onParentTaskChange = {},
                onEditParent = {},
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
                    initialName = name,
                    initialNote = note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Open,
                    tags = persistentListOf(
                        Tag(
                            id = 1L,
                            name = "morning routine",
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        )
                    ),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = null,
                ),
                name = TextFieldState(initialText = name),
                note = TextFieldState(initialText = note),
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
                tagQuery = TextFieldState(),
                onTagSearch = {},
                onParentTaskChange = {},
                onEditParent = {},
            )
        }
    }
}
