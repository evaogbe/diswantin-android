package io.github.evaogbe.diswantin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiswantinDatePickerDialog(
    onDismissRequest: () -> Unit,
    date: LocalDate?,
    onSelectDate: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onSelectDate(datePickerState.selectedDateMillis?.let {
                    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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
fun DiswantinTimePickerDialog(
    onDismissRequest: () -> Unit,
    time: LocalTime?,
    onSelectTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = time?.hour ?: 0,
        initialMinute = time?.minute ?: 0,
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
