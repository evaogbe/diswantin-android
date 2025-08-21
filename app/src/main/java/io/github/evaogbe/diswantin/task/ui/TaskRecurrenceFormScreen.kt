package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.ui.components.DiswantinDatePickerDialog
import io.github.evaogbe.diswantin.ui.components.EditFieldButton
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
                    imageVector = Icons.Default.Close,
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
    val uiState by taskFormViewModel.recurrenceUiStateOrDefault.collectAsStateWithLifecycle()
    val (start, setStart) = rememberSaveable(uiState) { mutableStateOf(uiState.start) }
    var type by rememberSaveable(uiState) { mutableStateOf(uiState.type) }
    val (step, setStep) = rememberSaveable(uiState) { mutableIntStateOf(uiState.step) }
    var weekdays by rememberSaveable(uiState, saver = persistentSetStateSaver()) {
        mutableStateOf(uiState.weekdays)
    }

    LaunchedEffect(topBarAction, taskFormViewModel) {
        when (topBarAction) {
            null -> {}
            TaskRecurrenceFormTopBarAction.Confirm -> {
                taskFormViewModel.updateRecurrence(
                    TaskRecurrenceUiState(
                        start = start,
                        type = type,
                        step = step,
                        weekdays = if (type == RecurrenceType.Week) weekdays else persistentSetOf(),
                        locale = taskFormViewModel.locale,
                    )
                )
                topBarActionHandled()
            }
        }
    }

    TaskRecurrenceFormScreen(
        start = start,
        onStartChange = setStart,
        type = type,
        onTypeChange = { value ->
            if (value == RecurrenceType.Week && weekdays.isEmpty()) {
                weekdays = persistentSetOf(taskFormViewModel.currentWeekday)
            }

            type = value
        },
        step = step,
        onStepChange = setStep,
        weekdays = weekdays,
        onWeekdaysChange = { weekdays = it },
        locale = taskFormViewModel.locale,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRecurrenceFormScreen(
    start: LocalDate,
    onStartChange: (LocalDate) -> Unit,
    type: RecurrenceType,
    onTypeChange: (RecurrenceType) -> Unit,
    step: Int,
    onStepChange: (Int) -> Unit,
    weekdays: PersistentSet<DayOfWeek>,
    onWeekdaysChange: (PersistentSet<DayOfWeek>) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var typeFieldExpanded by remember { mutableStateOf(false) }
    val typeOptions = listOf(
        R.plurals.recurrence_day to RecurrenceType.Day,
        R.plurals.recurrence_week to RecurrenceType.Week,
        R.plurals.recurrence_month to RecurrenceType.DayOfMonth,
        R.plurals.recurrence_year to RecurrenceType.Year,
    )

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
                onClick = { showDialog = true },
                text = start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            )
            Spacer(Modifier.size(SpaceMd))
            Text(stringResource(R.string.recurrence_step_label), style = typography.bodyLarge)
            Spacer(Modifier.size(SpaceSm))
            Row {
                OutlinedTextField(
                    value = if (step == 0) "" else step.toString(),
                    onValueChange = { onStepChange(it.toIntOrNull() ?: 0) },
                    modifier = Modifier.width(72.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                    ),
                    singleLine = true,
                )
                Spacer(Modifier.size(SpaceMd))
                ExposedDropdownMenuBox(
                    expanded = typeFieldExpanded,
                    onExpandedChange = { typeFieldExpanded = it },
                ) {
                    OutlinedTextField(
                        value = pluralStringResource(
                            when (type) {
                                RecurrenceType.Day -> R.plurals.recurrence_day
                                RecurrenceType.Week -> R.plurals.recurrence_week
                                RecurrenceType.DayOfMonth -> R.plurals.recurrence_month
                                RecurrenceType.WeekOfMonth -> R.plurals.recurrence_month
                                RecurrenceType.Year -> R.plurals.recurrence_year
                            },
                            step,
                        ),
                        onValueChange = {},
                        modifier = Modifier
                            .width(200.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeFieldExpanded)
                        },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = typeFieldExpanded,
                        onDismissRequest = { typeFieldExpanded = false },
                    ) {
                        typeOptions.forEach { (resId, type) ->
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
            Spacer(Modifier.size(SpaceMd))

            when (type) {
                RecurrenceType.Day -> {}
                RecurrenceType.Week -> {
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
                    Text(
                        stringResource(R.string.recurrence_monthly_type_label),
                        style = typography.bodyLarge,
                    )
                    Spacer(Modifier.size(SpaceSm))
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .widthIn(max = 512.dp)
                            .fillMaxWidth()
                            .height(intrinsicSize = IntrinsicSize.Max),
                    ) {
                        SegmentedButton(
                            selected = type == RecurrenceType.DayOfMonth,
                            onClick = { onTypeChange(RecurrenceType.DayOfMonth) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            Text(stringResource(R.string.recurrence_monthly_type_on_day))
                        }
                        SegmentedButton(
                            selected = type == RecurrenceType.WeekOfMonth,
                            onClick = { onTypeChange(RecurrenceType.WeekOfMonth) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.fillMaxHeight(),
                        ) {
                            Text(stringResource(R.string.recurrence_monthly_type_on_week))
                        }
                    }
                }

                RecurrenceType.Year -> {}
            }
        }
    }

    if (showDialog) {
        DiswantinDatePickerDialog(
            onDismiss = { showDialog = false },
            date = start,
            onSelectDate = onStartChange,
        )
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
        Scaffold(topBar = {
            TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
        }) { innerPadding ->
            TaskRecurrenceFormScreen(
                start = LocalDate.now(),
                onStartChange = {},
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
        Scaffold(topBar = {
            TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
        }) { innerPadding ->
            TaskRecurrenceFormScreen(
                start = LocalDate.now(),
                onStartChange = {},
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
        Scaffold(topBar = {
            TaskRecurrenceFormTopBar(onClose = {}, onConfirm = {})
        }) { innerPadding ->
            TaskRecurrenceFormScreen(
                start = LocalDate.now(),
                onStartChange = {},
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
