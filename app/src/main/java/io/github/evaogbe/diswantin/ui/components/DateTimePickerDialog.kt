package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.evaogbe.diswantin.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    onDismissRequest: () -> Unit,
    dateTime: ZonedDateTime?,
    onSelectDateTime: (ZonedDateTime?) -> Unit,
) {
    var showDate by rememberSaveable { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateTime?.toInstant()?.toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = dateTime?.hour ?: 0,
        initialMinute = dateTime?.minute ?: 0
    )

    if (showDate) {
        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = { showDate = false }) {
                    Text(stringResource(R.string.ok_button))
                }
            },
            modifier = Modifier.verticalScroll(rememberScrollState()),
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        var showClock by rememberSaveable { mutableStateOf(true) }

        TimePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = {
                    onSelectDateTime(
                        datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it)
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameLocal(dateTime?.zone ?: ZoneId.systemDefault())
                                .withHour(timePickerState.hour)
                                .withMinute(timePickerState.minute)
                        }
                    )
                }) {
                    Text(stringResource(R.string.ok_button))
                }
            },
            modifier = Modifier.verticalScroll(rememberScrollState()),
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
            toggle = {
                if (showClock) {
                    IconButton(onClick = { showClock = false }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_keyboard_24),
                            contentDescription = stringResource(
                                R.string.time_picker_switch_to_input_mode
                            ),
                        )
                    }
                } else {
                    IconButton(onClick = { showClock = true }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_schedule_24),
                            contentDescription = stringResource(
                                R.string.time_picker_switch_to_clock_mode
                            ),
                        )
                    }
                }
            }
        ) {
            if (showClock) {
                TimePicker(state = timePickerState)
            } else {
                TimeInput(state = timePickerState)
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    toggle: (@Composable () -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = shapes.extraLarge,
                    color = colorScheme.surface
                ),
            shape = shapes.extraLarge,
            color = colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = stringResource(R.string.time_picker_dialog_title),
                    style = typography.labelMedium
                )

                content()

                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle?.invoke()

                    Spacer(modifier = Modifier.weight(1f))
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}
