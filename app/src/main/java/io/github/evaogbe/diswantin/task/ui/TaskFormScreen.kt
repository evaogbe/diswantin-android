package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.components.ClearableLayout
import io.github.evaogbe.diswantin.ui.components.DateTimePickerDialog
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
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
        onDeadlineChange = taskFormViewModel::updateDeadlineInput,
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
    onDeadlineChange: (ZonedDateTime?) -> Unit,
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
                    onDeadlineChange = onDeadlineChange,
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
    Deadline, ScheduledAt
}

@Composable
fun TaskFormLayout(
    name: String,
    onNameChange: (String) -> Unit,
    onDeadlineChange: (ZonedDateTime?) -> Unit,
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
                        style = typography.titleSmall
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

            if (uiState.scheduledAtInput == null) {
                DateTimeTextField(
                    onClick = { dateTimePickerType = DateTimeConstraintField.Deadline },
                    dateTime = uiState.deadlineInput,
                    onDateTimeChange = onDeadlineChange,
                    label = { Text(stringResource(R.string.deadline_label)) },
                )
            }

            if (uiState.deadlineInput == null) {
                DateTimeTextField(
                    onClick = { dateTimePickerType = DateTimeConstraintField.ScheduledAt },
                    dateTime = uiState.scheduledAtInput,
                    onDateTimeChange = onScheduleAtChange,
                    label = { Text(stringResource(R.string.scheduled_at_label)) },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.recurringInput, onCheckedChange = onRecurringChange)
                Text(stringResource(R.string.recurring_label))
            }
        }
    }

    when (dateTimePickerType) {
        null -> {}
        DateTimeConstraintField.Deadline -> {
            DateTimePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                dateTime = uiState.deadlineInput,
                onSelectDateTime = { selectedDateTime ->
                    if (selectedDateTime != null) {
                        onDeadlineChange(selectedDateTime)
                    }
                    dateTimePickerType = null
                }
            )
        }

        DateTimeConstraintField.ScheduledAt -> {
            DateTimePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                dateTime = uiState.scheduledAtInput,
                onSelectDateTime = { selectedDateTime ->
                    if (selectedDateTime != null) {
                        onScheduleAtChange(selectedDateTime)
                    }
                    dateTimePickerType = null
                }
            )
        }
    }
}

@Composable
fun DateTimeTextField(
    onClick: () -> Unit,
    dateTime: ZonedDateTime?,
    onDateTimeChange: (ZonedDateTime?) -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onClick()
        }
    }

    ClearableLayout(
        canClear = dateTime != null,
        onClear = { onDateTimeChange(null) },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = dateTime?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
                ?: "",
            onValueChange = {},
            label = label,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null
                )
            },
            singleLine = true,
            interactionSource = interactionSource,
        )
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
            onDeadlineChange = {},
            onScheduleAtChange = {},
            onRecurringChange = {},
            onSave = {},
            uiState = TaskFormUiState.Success(
                deadlineInput = null,
                scheduledAtInput = null,
                recurringInput = false,
                hasSaveError = false,
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
            onDeadlineChange = {},
            onScheduleAtChange = {},
            onRecurringChange = {},
            onSave = {},
            uiState = TaskFormUiState.Success(
                deadlineInput = ZonedDateTime.now(),
                scheduledAtInput = null,
                recurringInput = true,
                hasSaveError = true,
            )
        )
    }
}
