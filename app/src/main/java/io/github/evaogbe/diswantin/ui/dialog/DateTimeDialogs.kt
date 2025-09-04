package io.github.evaogbe.diswantin.ui.dialog

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.getSelectedEndDate
import androidx.compose.material3.getSelectedStartDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.evaogbe.diswantin.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class SelectableDatesWithMax(private val maxDate: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) =
        utcTimeMillis <= maxDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli()

    override fun isSelectableYear(year: Int) = year <= maxDate.year
}

class SelectableDatesWithMin(private val minDate: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long) =
        utcTimeMillis >= minDate.atTime(LocalTime.MIN).toInstant(ZoneOffset.UTC).toEpochMilli()

    override fun isSelectableYear(year: Int) = year >= minDate.year
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiswantinDatePickerDialog(
    onDismiss: () -> Unit,
    date: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDate = date,
        selectableDates = selectableDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.getSelectedDate()?.let(onSelectDate)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.ok_button))
            }
        },
        modifier = modifier.verticalScroll(rememberScrollState()),
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiswantinDateRangePickerDialog(
    onDismiss: () -> Unit,
    dateRange: Pair<LocalDate, LocalDate>?,
    onSelectDateRange: (Pair<LocalDate, LocalDate>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDate = dateRange?.first,
        initialSelectedEndDate = dateRange?.second,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.getSelectedStartDate()
                    val end = dateRangePickerState.getSelectedEndDate()

                    if (start != null && end != null) {
                        onSelectDateRange(start to end)
                    }

                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.ok_button))
            }
        },
        modifier = modifier.verticalScroll(rememberScrollState()),
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
    ) {
        DateRangePicker(state = dateRangePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiswantinTimePickerDialog(
    onDismiss: () -> Unit,
    time: LocalTime?,
    onSelectTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = time?.hour ?: 0,
        initialMinute = time?.minute ?: 0,
    )
    var showDial by rememberSaveable { mutableStateOf(true) }

    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSelectTime(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.ok_button))
            }
        },
        title = { Text(stringResource(R.string.time_picker_dialog_title)) },
        modifier = modifier,
        modeToggleButton = {
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
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        },
    ) {
        if (showDial) {
            TimePicker(state = timePickerState)
        } else {
            TimeInput(state = timePickerState)
        }
    }
}
