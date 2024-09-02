package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.components.ClearableLayout
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TaskFormScreen(
    onPopBackStack: () -> Unit,
    taskFormViewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by taskFormViewModel.uiState.collectAsStateWithLifecycle()

    if (uiState is TaskFormUiState.Saved) {
        LaunchedEffect(onPopBackStack) {
            onPopBackStack()
        }
    }

    TaskFormScreen(
        isNew = taskFormViewModel.isNew,
        onClose = onPopBackStack,
        name = taskFormViewModel.nameInput,
        onNameChange = taskFormViewModel::updateNameInput,
        onDeadlineDateChange = taskFormViewModel::updateDeadlineDateInput,
        onDeadlineTimeChange = taskFormViewModel::updateDeadlineTimeInput,
        onScheduleAtChange = taskFormViewModel::updateScheduledAtInput,
        onRecurringChange = taskFormViewModel::updateRecurringInput,
        onSave = taskFormViewModel::saveTask,
        uiState = uiState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    isNew: Boolean,
    onClose: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    onDeadlineDateChange: (LocalDate?) -> Unit,
    onDeadlineTimeChange: (LocalTime?) -> Unit,
    onScheduleAtChange: (ZonedDateTime?) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    uiState: TaskFormUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) {
                            stringResource(R.string.task_form_title_new)
                        } else {
                            stringResource(R.string.task_form_title_edit)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_button)
                        )
                    }
                },
                actions = {
                    if (uiState is TaskFormUiState.Success) {
                        Button(
                            onClick = onSave,
                            enabled = name.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        ) {
                            Text(stringResource(R.string.save_button))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is TaskFormUiState.Pending,
            is TaskFormUiState.Saved -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is TaskFormUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.task_form_fetch_error),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is TaskFormUiState.Success -> {
                TaskFormLayout(
                    name = name,
                    onNameChange = onNameChange,
                    onDeadlineDateChange = onDeadlineDateChange,
                    onDeadlineTimeChange = onDeadlineTimeChange,
                    onScheduleAtChange = onScheduleAtChange,
                    onRecurringChange = onRecurringChange,
                    uiState = uiState,
                    formError = when {
                        !uiState.hasSaveError -> null
                        isNew -> stringResource(R.string.task_form_save_error_new)
                        else -> stringResource(R.string.task_form_save_error_edit)
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

enum class DateTimeConstraintField {
    DeadlineDate, DeadlineTime, ScheduledDate, ScheduledTime,
}

@Composable
fun TaskFormLayout(
    name: String,
    onNameChange: (String) -> Unit,
    onDeadlineDateChange: (LocalDate?) -> Unit,
    onDeadlineTimeChange: (LocalTime?) -> Unit,
    onScheduleAtChange: (ZonedDateTime?) -> Unit,
    onRecurringChange: (Boolean) -> Unit,
    uiState: TaskFormUiState.Success,
    formError: String?,
    modifier: Modifier = Modifier,
) {
    var dateTimePickerType by rememberSaveable {
        mutableStateOf<DateTimeConstraintField?>(null)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd),
            verticalArrangement = Arrangement.spacedBy(SpaceMd)
        ) {
            if (formError != null) {
                SelectionContainer {
                    Text(
                        text = formError,
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

            when {
                uiState.deadlineDateInput != null || uiState.deadlineTimeInput != null -> {
                    if (uiState.deadlineDateInput == null) {
                        AddDateTimeButton(
                            onClick = { dateTimePickerType = DateTimeConstraintField.DeadlineDate },
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
                                EditDateTimeButton(
                                    onClick = {
                                        dateTimePickerType = DateTimeConstraintField.DeadlineDate
                                    },
                                    dateTime = uiState.deadlineDateInput.format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                    ),
                                )
                            }
                        }
                    }

                    if (uiState.deadlineTimeInput == null) {
                        AddDateTimeButton(
                            onClick = { dateTimePickerType = DateTimeConstraintField.DeadlineTime },
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
                                EditDateTimeButton(
                                    onClick = {
                                        dateTimePickerType = DateTimeConstraintField.DeadlineTime
                                    },
                                    dateTime = uiState.deadlineTimeInput.format(
                                        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                    ),
                                )
                            }
                        }
                    }
                }

                uiState.scheduledAtInput != null -> {
                    Column {
                        Text(
                            text = stringResource(R.string.scheduled_at_label),
                            style = typography.bodyLarge,
                        )
                        Spacer(Modifier.size(SpaceSm))
                        ClearableLayout(onClear = { onScheduleAtChange(null) }, invert = false) {
                            EditDateTimeButton(
                                onClick = {
                                    dateTimePickerType = DateTimeConstraintField.ScheduledDate
                                },
                                dateTime = uiState.scheduledAtInput.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                ),
                            )
                            EditDateTimeButton(
                                onClick = {
                                    dateTimePickerType = DateTimeConstraintField.ScheduledTime
                                },
                                dateTime = uiState.scheduledAtInput.format(
                                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                                ),
                            )
                        }
                    }
                }

                else -> {
                    AddDateTimeButton(
                        onClick = { dateTimePickerType = DateTimeConstraintField.DeadlineDate },
                        text = stringResource(R.string.add_deadline_date_button),
                    )
                    AddDateTimeButton(
                        onClick = { dateTimePickerType = DateTimeConstraintField.DeadlineTime },
                        text = stringResource(R.string.add_deadline_time_button),
                    )
                    AddDateTimeButton(
                        onClick = { dateTimePickerType = DateTimeConstraintField.ScheduledDate },
                        text = stringResource(R.string.add_scheduled_at_button),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.recurringInput, onCheckedChange = onRecurringChange)
                Text(stringResource(R.string.recurring_label))
            }
        }
    }

    when (dateTimePickerType) {
        null -> {}
        DateTimeConstraintField.DeadlineDate -> {
            TaskFormDatePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                dateTime = uiState.deadlineDateAsZonedDateTime,
                onSelectDateTime = {
                    if (it != null) {
                        onDeadlineDateChange(it.toLocalDate())
                    }
                    dateTimePickerType = null
                },
            )
        }

        DateTimeConstraintField.DeadlineTime -> {
            TaskFormTimePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                time = uiState.deadlineTimeInput,
                onSelectTime = {
                    onDeadlineTimeChange(it)
                    dateTimePickerType = null
                },
            )
        }

        DateTimeConstraintField.ScheduledDate -> {
            TaskFormDatePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                dateTime = uiState.scheduledAtInput,
                onSelectDateTime = {
                    if (it != null) {
                        onScheduleAtChange(it)
                    }
                    dateTimePickerType = null
                },
            )
        }

        DateTimeConstraintField.ScheduledTime -> {
            TaskFormTimePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                time = uiState.scheduledAtInput?.toLocalTime(),
                onSelectTime = {
                    if (uiState.scheduledAtInput != null) {
                        onScheduleAtChange(
                            uiState.scheduledAtInput.withHour(it.hour).withMinute(it.minute),
                        )
                    }
                    dateTimePickerType = null
                },
            )
        }
    }
}

@Composable
fun EditDateTimeButton(onClick: () -> Unit, dateTime: String, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = shapes.extraSmall,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = dateTime)
    }
}

@Composable
fun AddDateTimeButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_schedule_24),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormDatePickerDialog(
    onDismissRequest: () -> Unit,
    dateTime: ZonedDateTime?,
    onSelectDateTime: (ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateTime?.toInstant()?.toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onSelectDateTime(datePickerState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it)
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameLocal(dateTime?.zone ?: ZoneId.systemDefault())
                        .withHour(dateTime?.hour ?: 0)
                        .withMinute(dateTime?.minute ?: 0)
                })
            }) {
                Text(stringResource(R.string.ok_button))
            }
        },
        modifier = modifier.verticalScroll(rememberScrollState()),
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormTimePickerDialog(
    onDismissRequest: () -> Unit,
    time: LocalTime?,
    onSelectTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = time?.hour ?: 0,
        initialMinute = time?.minute ?: 0
    )
    var showDial by rememberSaveable { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(shape = shapes.extraLarge, color = colorScheme.surface),
            shape = shapes.extraLarge,
            color = colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = stringResource(R.string.time_picker_dialog_title),
                    style = typography.labelMedium,
                )

                if (showDial) {
                    TimePicker(state = timePickerState)
                } else {
                    TimeInput(state = timePickerState)
                }

                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                ) {
                    if (showDial) {
                        IconButton(onClick = { showDial = false }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_keyboard_24),
                                contentDescription = stringResource(
                                    R.string.time_picker_switch_to_input_mode
                                ),
                            )
                        }
                    } else {
                        IconButton(onClick = { showDial = true }) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_schedule_24),
                                contentDescription = stringResource(
                                    R.string.time_picker_switch_to_dial_mode
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel_button))
                    }
                    TextButton(onClick = {
                        onSelectTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    }) {
                        Text(stringResource(R.string.ok_button))
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormScreenPreview_New() {
    DiswantinTheme {
        TaskFormScreen(
            isNew = true,
            onClose = {},
            name = "",
            onNameChange = {},
            onDeadlineDateChange = {},
            onDeadlineTimeChange = {},
            onScheduleAtChange = {},
            onRecurringChange = {},
            onSave = {},
            uiState = TaskFormUiState.Success(
                deadlineDateInput = null,
                deadlineTimeInput = null,
                scheduledAtInput = null,
                recurringInput = false,
                hasSaveError = false,
                clock = Clock.systemDefaultZone(),
            )
        )
    }
}

@DevicePreviews
@Composable
private fun TaskFormScreenPreview_Edit() {
    DiswantinTheme {
        TaskFormScreen(
            isNew = false,
            onClose = {},
            name = "Shower",
            onNameChange = {},
            onDeadlineDateChange = {},
            onDeadlineTimeChange = {},
            onScheduleAtChange = {},
            onRecurringChange = {},
            onSave = {},
            uiState = TaskFormUiState.Success(
                deadlineDateInput = null,
                deadlineTimeInput = null,
                scheduledAtInput = ZonedDateTime.now(),
                recurringInput = true,
                hasSaveError = true,
                clock = Clock.systemDefaultZone(),
            )
        )
    }
}

@DevicePreviews
@Composable
private fun TaskFormLayoutPreview_Deadline() {
    DiswantinTheme {
        Surface {
            TaskFormLayout(
                name = "",
                onNameChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onScheduleAtChange = {},
                onRecurringChange = {},
                uiState = TaskFormUiState.Success(
                    deadlineDateInput = LocalDate.now(),
                    deadlineTimeInput = LocalTime.now(),
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                    clock = Clock.systemDefaultZone()
                ),
                formError = null,
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskFormLayoutPreview_DeadlineDate() {
    DiswantinTheme {
        Surface {
            TaskFormLayout(
                name = "",
                onNameChange = {},
                onDeadlineDateChange = {},
                onDeadlineTimeChange = {},
                onScheduleAtChange = {},
                onRecurringChange = {},
                uiState = TaskFormUiState.Success(
                    deadlineDateInput = LocalDate.now(),
                    deadlineTimeInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                    clock = Clock.systemDefaultZone()
                ),
                formError = null,
            )
        }
    }
}
