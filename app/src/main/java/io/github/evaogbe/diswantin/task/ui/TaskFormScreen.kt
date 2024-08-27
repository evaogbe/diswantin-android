package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.AutocompleteField
import io.github.evaogbe.diswantin.ui.components.DateTimePickerDialog
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TaskFormScreen(
    onPopBackStack: () -> Unit,
    taskFormViewModel: TaskFormViewModel = hiltViewModel()
) {
    val uiState by taskFormViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState, taskFormViewModel, onPopBackStack) {
        if (uiState is TaskFormUiState.Saved) {
            onPopBackStack()
        }
    }

    TaskFormScreen(
        isNew = taskFormViewModel.isNew,
        onClose = onPopBackStack,
        name = taskFormViewModel.nameInput,
        onNameChange = taskFormViewModel::updateNameInput,
        onDueAtChange = taskFormViewModel::updateDueAtInput,
        onScheduleAtChange = taskFormViewModel::updateScheduledAtInput,
        onPrevTaskSearch = taskFormViewModel::searchPrevTask,
        onSelectPrevTask = taskFormViewModel::updatePrevTask,
        onSave = taskFormViewModel::saveTask,
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormScreen(
    isNew: Boolean,
    onClose: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    onDueAtChange: (ZonedDateTime?) -> Unit,
    onScheduleAtChange: (ZonedDateTime?) -> Unit,
    onPrevTaskSearch: (String) -> Unit,
    onSelectPrevTask: (Task?) -> Unit,
    onSave: () -> Unit,
    uiState: TaskFormUiState
) {
    Scaffold(
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
                }, actions = {
                    if (uiState is TaskFormUiState.Success) {
                        Button(
                            onClick = onSave,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.save_button))
                        }
                    }
                })
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
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TaskFormUiState.Success -> {
                TaskFormLayout(
                    name = name,
                    onNameChange = onNameChange,
                    onDueAtChange = onDueAtChange,
                    onScheduleAtChange = onScheduleAtChange,
                    onPrevTaskSearch = onPrevTaskSearch,
                    onSelectPrevTask = onSelectPrevTask,
                    uiState = uiState,
                    formError = when {
                        !uiState.hasSaveError -> null
                        isNew -> stringResource(R.string.task_form_save_error_new)
                        else -> stringResource(R.string.task_form_save_error_edit)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

enum class DateTimeConstraintField {
    DueAt, ScheduledAt
}

@Composable
fun TaskFormLayout(
    name: String,
    onNameChange: (String) -> Unit,
    onDueAtChange: (ZonedDateTime?) -> Unit,
    onScheduleAtChange: (ZonedDateTime?) -> Unit,
    onPrevTaskSearch: (String) -> Unit,
    onSelectPrevTask: (Task?) -> Unit,
    uiState: TaskFormUiState.Success,
    formError: String?,
    modifier: Modifier = Modifier
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
                        style = typography.titleMedium
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
                    onClick = { dateTimePickerType = DateTimeConstraintField.DueAt },
                    dateTime = uiState.dueAtInput,
                    onDateTimeChange = onDueAtChange,
                    label = { Text(stringResource(R.string.due_at_label)) },
                )
            }

            if (uiState.dueAtInput == null) {
                DateTimeTextField(
                    onClick = { dateTimePickerType = DateTimeConstraintField.ScheduledAt },
                    dateTime = uiState.scheduledAtInput,
                    onDateTimeChange = onScheduleAtChange,
                    label = { Text(stringResource(R.string.scheduled_at_label)) },
                )
            }

            if (uiState.canUpdatePrevTask) {
                AutocompleteField(
                    label = { Text(stringResource(R.string.prev_task_label)) },
                    onSearch = onPrevTaskSearch,
                    selectedOption = uiState.prevTask,
                    options = uiState.prevTaskOptions,
                    formatOption = Task::name,
                    onSelectOption = onSelectPrevTask,
                )
            }
        }
    }

    when (dateTimePickerType) {
        null -> {}
        DateTimeConstraintField.DueAt -> {
            DateTimePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                dateTime = uiState.dueAtInput,
                onSelectDateTime = { selectedDateTime ->
                    if (selectedDateTime != null) {
                        onDueAtChange(selectedDateTime)
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
    label: @Composable (() -> Unit)
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(isPressed) {
            if (isPressed) {
                onClick()
            }
        }

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

        if (dateTime != null) {
            Spacer(Modifier.size(SpaceSm))
            IconButton(
                onClick = { onDateTimeChange(null) },
                modifier = Modifier.padding(top = 4.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.clear_button)
                )
            }
        }
    }
}

@DevicePreviews
@Composable
fun TaskFormScreenPreview_New() {
    DiswantinTheme {
        TaskFormScreen(
            isNew = true,
            onClose = {},
            name = "",
            onNameChange = {},
            onDueAtChange = {},
            onScheduleAtChange = {},
            onPrevTaskSearch = {},
            onSelectPrevTask = {},
            onSave = {},
            uiState = TaskFormUiState.Success(
                dueAtInput = null,
                scheduledAtInput = null,
                canUpdatePrevTask = false,
                prevTask = null,
                prevTaskOptions = emptyList(),
                hasSaveError = false,
            )
        )
    }
}

@DevicePreviews
@Composable
fun TaskFormScreenPreview_Edit() {
    DiswantinTheme {
        TaskFormScreen(
            isNew = false,
            onClose = {},
            name = "Shower",
            onNameChange = {},
            onDueAtChange = {},
            onScheduleAtChange = {},
            onPrevTaskSearch = {},
            onSelectPrevTask = {},
            onSave = {},
            uiState = TaskFormUiState.Success(
                dueAtInput = ZonedDateTime.now(),
                scheduledAtInput = null,
                canUpdatePrevTask = true,
                prevTask = Task(id = 1L, createdAt = Instant.now(), name = "Brush teeth"),
                prevTaskOptions = emptyList(),
                hasSaveError = true,
            )
        )
    }
}
