package io.github.evaogbe.diswantin.task.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.ui.button.TextButtonWithIcon
import io.github.evaogbe.diswantin.ui.dialog.DiswantinDatePickerDialog
import io.github.evaogbe.diswantin.ui.dialog.SelectableDatesWithMax
import io.github.evaogbe.diswantin.ui.dialog.SelectableDatesWithMin
import io.github.evaogbe.diswantin.ui.form.ClearableLayout
import io.github.evaogbe.diswantin.ui.form.EditFieldButton
import io.github.evaogbe.diswantin.ui.form.OutlinedIntegerField
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRecurrenceFormTopBar(
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.task_recurrence_form_title)) },
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
            TextButton(
                onClick = onConfirm,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.done_button))
            }
        }
    )
}

@Composable
fun TaskRecurrentFormScreen(
    topBarAction: TaskRecurrenceFormTopBarAction?,
    topBarActionHandled: () -> Unit,
    taskFormViewModel: TaskFormViewModel = hiltViewModel(),
) {
    val uiState by taskFormViewModel.recurrenceUiState.collectAsStateWithLifecycle()
    var dirty by rememberSaveable { mutableStateOf(false) }
    var startDate by rememberSaveable { mutableStateOf(uiState?.startDate) }
    var endDate by rememberSaveable { mutableStateOf(uiState?.endDate) }
    var type by rememberSaveable { mutableStateOf(uiState?.type) }
    var step by rememberSaveable { mutableIntStateOf(uiState?.step ?: 1) }
    var weekdays by rememberSaveable(saver = persistentSetStateSaver()) {
        mutableStateOf(uiState?.weekdays ?: persistentSetOf())
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TaskRecurrenceFormTopBarAction.Confirm -> {
                taskFormViewModel.updateRecurrence(
                    TaskRecurrenceUiState(
                        startDate = startDate ?: taskFormViewModel.today,
                        endDate = endDate,
                        type = type ?: RecurrenceType.Day,
                        step = step,
                        weekdays = if (type == RecurrenceType.Week) weekdays else persistentSetOf(),
                        locale = taskFormViewModel.locale,
                    )
                )
                topBarActionHandled()
            }
        }
    }

    LaunchedEffect(uiState) {
        if (!dirty) {
            startDate = uiState?.startDate
            endDate = uiState?.endDate
            type = uiState?.type
            step = uiState?.step ?: 1
            weekdays = uiState?.weekdays ?: persistentSetOf()
        }
    }

    TaskRecurrenceFormScreen(
        startDate = startDate ?: taskFormViewModel.today,
        onStartDateChange = { value ->
            startDate = value

            if (value != uiState?.startDate) {
                dirty = true
            }
        },
        endDate = endDate,
        onEndDateChange = { value ->
            endDate = value

            if (value != uiState?.endDate) {
                dirty = true
            }
        },
        type = type ?: RecurrenceType.Day,
        onTypeChange = { value ->
            if (value == RecurrenceType.Week && weekdays.isEmpty()) {
                weekdays = persistentSetOf(taskFormViewModel.today.dayOfWeek)
            }

            type = value

            if (value != uiState?.type) {
                dirty = true
            }
        },
        step = step,
        onStepChange = { value ->
            step = value

            if (value != uiState?.step) {
                dirty = true
            }
        },
        weekdays = weekdays,
        onWeekdaysChange = { value ->
            weekdays = value

            if (value != uiState?.weekdays) {
                dirty = true
            }
        },
        locale = taskFormViewModel.locale,
    )
}

private val TypeOptions = listOf(
    R.plurals.recurrence_day to RecurrenceType.Day,
    R.plurals.recurrence_week to RecurrenceType.Week,
    R.plurals.recurrence_month to RecurrenceType.DayOfMonth,
    R.plurals.recurrence_year to RecurrenceType.Year,
)

private val MonthOptions = listOf(RecurrenceType.DayOfMonth, RecurrenceType.WeekOfMonth)

enum class RecurrenceDialogType {
    Start, End
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRecurrenceFormScreen(
    startDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate?) -> Unit,
    type: RecurrenceType,
    onTypeChange: (RecurrenceType) -> Unit,
    step: Int,
    onStepChange: (Int) -> Unit,
    weekdays: PersistentSet<DayOfWeek>,
    onWeekdaysChange: (PersistentSet<DayOfWeek>) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    var dialogType by rememberSaveable { mutableStateOf<RecurrenceDialogType?>(null) }
    val typeInput = rememberTextFieldState(
        initialText = pluralStringResource(
            when (type) {
                RecurrenceType.Day -> R.plurals.recurrence_day
                RecurrenceType.Week -> R.plurals.recurrence_week
                RecurrenceType.DayOfMonth -> R.plurals.recurrence_month
                RecurrenceType.WeekOfMonth -> R.plurals.recurrence_month
                RecurrenceType.Year -> R.plurals.recurrence_year
            },
            step,
        )
    )
    var typeFieldExpanded by remember { mutableStateOf(false) }
    val monthInput = rememberTextFieldState(
        initialText = if (type in MonthOptions) {
            taskRecurrenceText(
                TaskRecurrenceUiState(
                    startDate = startDate,
                    endDate = null,
                    type = type,
                    step = 1,
                    weekdays = persistentSetOf(),
                    locale = locale,
                )
            )
        } else {
            ""
        }
    )
    var monthFieldExpanded by remember { mutableStateOf(false) }
    val resources = LocalResources.current
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    LaunchedEffect(resources, type, step) {
        typeInput.setTextAndPlaceCursorAtEnd(
            resources.getQuantityString(
                when (type) {
                    RecurrenceType.Day -> R.plurals.recurrence_day
                    RecurrenceType.Week -> R.plurals.recurrence_week
                    RecurrenceType.DayOfMonth -> R.plurals.recurrence_month
                    RecurrenceType.WeekOfMonth -> R.plurals.recurrence_month
                    RecurrenceType.Year -> R.plurals.recurrence_year
                },
                step,
            )
        )
    }

    LaunchedEffect(resources, locale, type, startDate) {
        if (type in MonthOptions) {
            monthInput.setTextAndPlaceCursorAtEnd(
                resources.getTaskRecurrenceText(
                    TaskRecurrenceUiState(
                        startDate = startDate,
                        endDate = null,
                        type = type,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = locale,
                    )
                )
            )
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd),
        ) {
            Text(stringResource(R.string.start_date_label), style = typography.bodyLarge)
            Spacer(Modifier.size(SpaceSm))
            EditFieldButton(
                onClick = { dialogType = RecurrenceDialogType.Start },
                text = startDate.format(dateFormatter),
            )

            Spacer(Modifier.size(SpaceMd))
            Text(stringResource(R.string.recurrence_step_label), style = typography.bodyLarge)
            Spacer(Modifier.size(SpaceSm))
            Row {
                OutlinedIntegerField(
                    value = step,
                    onValueChange = onStepChange,
                    modifier = Modifier.width(72.dp),
                )
                Spacer(Modifier.size(SpaceMd))
                ExposedDropdownMenuBox(
                    expanded = typeFieldExpanded,
                    onExpandedChange = { typeFieldExpanded = it },
                ) {
                    OutlinedTextField(
                        state = typeInput,
                        modifier = Modifier
                            .width(200.dp)
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeFieldExpanded)
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                    ExposedDropdownMenu(
                        expanded = typeFieldExpanded,
                        onDismissRequest = { typeFieldExpanded = false },
                    ) {
                        TypeOptions.forEach { (resId, type) ->
                            DropdownMenuItem(
                                text = { Text(text = pluralStringResource(resId, step)) },
                                onClick = {
                                    onTypeChange(type)
                                    typeFieldExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            when (type) {
                RecurrenceType.Day -> {}
                RecurrenceType.Week -> {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.recurrence_weekday_label),
                        style = typography.bodyLarge,
                    )
                    Spacer(Modifier.size(SpaceSm))
                    Row(horizontalArrangement = Arrangement.spacedBy(SpaceSm)) {
                        DayOfWeek.entries
                            .partition { it == WeekFields.of(locale).firstDayOfWeek }
                            .let { it.first + it.second }
                            .forEach { weekday ->
                                if (weekday in weekdays) {
                                    Button(
                                        onClick = { onWeekdaysChange(weekdays.remove(weekday)) },
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        elevation = null,
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Text(
                                            text = weekday.getDisplayName(
                                                TextStyle.NARROW_STANDALONE,
                                                locale
                                            ),
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onWeekdaysChange(weekdays.add(weekday)) },
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Text(
                                            text = weekday.getDisplayName(
                                                TextStyle.NARROW_STANDALONE,
                                                locale
                                            ),
                                        )
                                    }
                                }
                            }
                    }
                }

                RecurrenceType.DayOfMonth,
                RecurrenceType.WeekOfMonth -> {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.recurrence_monthly_type_label),
                        style = typography.bodyLarge,
                    )
                    Spacer(Modifier.size(SpaceSm))
                    ExposedDropdownMenuBox(
                        expanded = monthFieldExpanded,
                        onExpandedChange = { monthFieldExpanded = it },
                    ) {
                        OutlinedTextField(
                            state = monthInput,
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = monthFieldExpanded
                                )
                            },
                            lineLimits = TextFieldLineLimits.SingleLine,
                        )
                        ExposedDropdownMenu(
                            expanded = monthFieldExpanded,
                            onDismissRequest = { monthFieldExpanded = false },
                        ) {
                            MonthOptions.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = taskRecurrenceText(
                                                TaskRecurrenceUiState(
                                                    startDate = startDate,
                                                    endDate = null,
                                                    type = type,
                                                    step = 1,
                                                    weekdays = persistentSetOf(),
                                                    locale = locale,
                                                )
                                            )
                                        )
                                    },
                                    onClick = {
                                        onTypeChange(type)
                                        monthFieldExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                RecurrenceType.Year -> {}
            }

            Spacer(Modifier.size(SpaceMd))
            if (endDate == null) {
                TextButtonWithIcon(
                    onClick = { dialogType = RecurrenceDialogType.End },
                    painter = painterResource(R.drawable.baseline_schedule_24),
                    text = stringResource(R.string.add_end_date_button),
                )
            } else {
                Text(text = stringResource(R.string.end_date_label), style = typography.bodyLarge)
                Spacer(Modifier.size(SpaceSm))
                ClearableLayout(
                    onClear = { onEndDateChange(null) },
                    iconContentDescription = stringResource(R.string.remove_button),
                ) {
                    EditFieldButton(
                        onClick = { dialogType = RecurrenceDialogType.End },
                        text = endDate.format(dateFormatter),
                    )
                }
            }
        }
    }

    when (dialogType) {
        null -> {}
        RecurrenceDialogType.Start -> {
            DiswantinDatePickerDialog(
                onDismiss = { dialogType = null },
                date = startDate,
                onSelectDate = onStartDateChange,
                selectableDates = if (endDate == null) {
                    DatePickerDefaults.AllDates
                } else {
                    SelectableDatesWithMax(endDate)
                },
            )
        }

        RecurrenceDialogType.End -> {
            DiswantinDatePickerDialog(
                onDismiss = { dialogType = null },
                date = endDate,
                onSelectDate = onEndDateChange,
                selectableDates = SelectableDatesWithMin(startDate)
            )
        }
    }
}

private fun <T> persistentSetStateSaver() = listSaver<MutableState<PersistentSet<T>>, T>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toPersistentSet()) },
)

@DevicePreviews
@Composable
private fun TaskRecurrenceFormScreenPreview_Daily() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
            },
        ) { innerPadding ->
            TaskRecurrenceFormScreen(
                startDate = LocalDate.now(),
                onStartDateChange = {},
                endDate = null,
                onEndDateChange = {},
                type = RecurrenceType.Day,
                onTypeChange = {},
                step = 1,
                onStepChange = {},
                weekdays = persistentSetOf(),
                onWeekdaysChange = {},
                locale = Locale.getDefault(),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskRecurrenceFormScreenPreview_Weekly() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
            },
        ) { innerPadding ->
            TaskRecurrenceFormScreen(
                startDate = LocalDate.now(),
                onStartDateChange = {},
                endDate = null,
                onEndDateChange = {},
                type = RecurrenceType.Week,
                onTypeChange = {},
                step = 10,
                onStepChange = {},
                weekdays = persistentSetOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY),
                onWeekdaysChange = {},
                locale = Locale.getDefault(),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskRecurrenceFormScreenPreview_MonthlyOnDay() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
            },
        ) { innerPadding ->
            TaskRecurrenceFormScreen(
                startDate = LocalDate.now(),
                onStartDateChange = {},
                endDate = null,
                onEndDateChange = {},
                type = RecurrenceType.DayOfMonth,
                onTypeChange = {},
                step = 2,
                onStepChange = {},
                weekdays = persistentSetOf(),
                onWeekdaysChange = {},
                locale = Locale.getDefault(),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}


@DevicePreviews
@Composable
private fun TaskRecurrenceFormScreenPreview_MonthlyOnWeek() {
    val startDate = LocalDate.now()

    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
            },
        ) { innerPadding ->
            TaskRecurrenceFormScreen(
                startDate = startDate,
                onStartDateChange = {},
                endDate = startDate.plusYears(1),
                onEndDateChange = {},
                type = RecurrenceType.WeekOfMonth,
                onTypeChange = {},
                step = 1,
                onStepChange = {},
                weekdays = persistentSetOf(),
                onWeekdaysChange = {},
                locale = Locale.getDefault(),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
