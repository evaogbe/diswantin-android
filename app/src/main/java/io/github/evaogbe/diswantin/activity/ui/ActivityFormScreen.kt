package io.github.evaogbe.diswantin.activity.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.components.DateTimePickerDialog
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ActivityFormScreen(
    onPopBackStack: () -> Unit,
    activityFormViewModel: ActivityFormViewModel = hiltViewModel()
) {
    val uiState = activityFormViewModel.uiState

    LaunchedEffect(uiState, activityFormViewModel, onPopBackStack) {
        when (uiState) {
            is ActivityFormUiState.Pending -> {
                activityFormViewModel.initialize()
            }

            is ActivityFormUiState.Saved -> {
                onPopBackStack()
            }

            else -> {}
        }
    }

    ActivityFormScreen(
        isNew = activityFormViewModel.isNew,
        onClose = onPopBackStack,
        name = activityFormViewModel.nameInput,
        onNameChange = activityFormViewModel::updateNameInput,
        dueAt = activityFormViewModel.dueAtInput,
        onDueAtChange = activityFormViewModel::updateDueAtInput,
        scheduledAt = activityFormViewModel.scheduledAtInput,
        onScheduleAtChange = activityFormViewModel::updateScheduledAtInput,
        onSave = activityFormViewModel::saveActivity,
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFormScreen(
    isNew: Boolean,
    onClose: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    dueAt: ZonedDateTime?,
    onDueAtChange: (ZonedDateTime?) -> Unit,
    scheduledAt: ZonedDateTime?,
    onScheduleAtChange: (ZonedDateTime?) -> Unit,
    onSave: () -> Unit,
    uiState: ActivityFormUiState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) {
                            stringResource(R.string.activity_form_title_new)
                        } else {
                            stringResource(R.string.activity_form_title_edit)
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
                    if (uiState is ActivityFormUiState.Success) {
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
            is ActivityFormUiState.Pending,
            is ActivityFormUiState.Saved -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is ActivityFormUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.activity_form_fetch_error),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is ActivityFormUiState.Success -> {
                ActivityFormLayout(
                    name = name,
                    onNameChange = onNameChange,
                    dueAt = dueAt,
                    onDueAtChange = onDueAtChange,
                    scheduledAt = scheduledAt,
                    onScheduleAtChange = onScheduleAtChange,
                    formError = when {
                        !uiState.hasSaveError -> null
                        isNew -> stringResource(R.string.activity_form_save_error_new)
                        else -> stringResource(R.string.activity_form_save_error_edit)
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
fun ActivityFormLayout(
    name: String,
    onNameChange: (String) -> Unit,
    dueAt: ZonedDateTime?,
    onDueAtChange: (ZonedDateTime?) -> Unit,
    scheduledAt: ZonedDateTime?,
    onScheduleAtChange: (ZonedDateTime?) -> Unit,
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
                singleLine = true
            )

            if (scheduledAt == null) {
                DateTimeTextField(
                    onClick = { dateTimePickerType = DateTimeConstraintField.DueAt },
                    dateTime = dueAt,
                    onDateTimeChange = onDueAtChange,
                    label = { Text(stringResource(R.string.due_at_label)) },
                )
            }

            if (dueAt == null) {
                DateTimeTextField(
                    onClick = { dateTimePickerType = DateTimeConstraintField.ScheduledAt },
                    dateTime = scheduledAt,
                    onDateTimeChange = onScheduleAtChange,
                    label = { Text(stringResource(R.string.scheduled_at_label)) },
                )
            }
        }
    }

    when (dateTimePickerType) {
        null -> {}
        DateTimeConstraintField.DueAt -> {
            DateTimePickerDialog(
                onDismissRequest = { dateTimePickerType = null },
                dateTime = dueAt,
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
                dateTime = scheduledAt,
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
            interactionSource = interactionSource
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
fun ActivityFormScreenPreview_New() {
    DiswantinTheme {
        ActivityFormScreen(
            isNew = true,
            onClose = {},
            name = "",
            onNameChange = {},
            dueAt = null,
            onDueAtChange = {},
            scheduledAt = null,
            onScheduleAtChange = {},
            onSave = {},
            uiState = ActivityFormUiState.Success(hasSaveError = false)
        )
    }
}

@DevicePreviews
@Composable
fun ActivityFormScreenPreview_Edit() {
    DiswantinTheme {
        ActivityFormScreen(
            isNew = false,
            onClose = {},
            name = "Brush teeth",
            onNameChange = {},
            dueAt = ZonedDateTime.now(),
            onDueAtChange = {},
            scheduledAt = null,
            onScheduleAtChange = {},
            onSave = {},
            uiState = ActivityFormUiState.Success(hasSaveError = true)
        )
    }
}
