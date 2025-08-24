package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.button.ButtonWithIcon
import io.github.evaogbe.diswantin.ui.dialog.DiswantinDatePickerDialog
import io.github.evaogbe.diswantin.ui.dialog.DiswantinDateRangePickerDialog
import io.github.evaogbe.diswantin.ui.form.AutoFocusTextField
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.loadstate.pagedListFooter
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            AutoFocusTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.task_search_title)) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_button)
                            )
                        }
                    }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                )
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back_button),
                )
            }
        },
    )
}

enum class FilterDialogType {
    DeadlineDateRange, StartAfterDateRange, ScheduledDateRange, DoneDateRange, RecurrenceDate
}

@OptIn(FlowPreview::class)
@Composable
fun TaskSearchScreen(
    query: String,
    topBarAction: TaskSearchTopBarAction?,
    topBarActionHandled: () -> Unit,
    onAddTask: (String) -> Unit,
    onSelectSearchResult: (Long) -> Unit,
    taskSearchViewModel: TaskSearchViewModel = hiltViewModel(),
) {
    val searchResultPagingItems =
        taskSearchViewModel.searchResultPagingData.collectAsLazyPagingItems()
    val uiState by taskSearchViewModel.uiState.collectAsStateWithLifecycle()
    var deadlineDateRange by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var startAfterDateRange by rememberSaveable {
        mutableStateOf<Pair<LocalDate, LocalDate>?>(null)
    }
    var scheduledDateRange by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var doneDateRange by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var recurrenceDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var filterDialogType by rememberSaveable { mutableStateOf<FilterDialogType?>(null) }
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(Unit) {
        snapshotFlow { currentQuery }.debounce(150.milliseconds).distinctUntilChanged()
            .collectLatest {
                taskSearchViewModel.searchTasks(
                    name = it,
                    deadlineDateRange = deadlineDateRange,
                    startAfterDateRange = startAfterDateRange,
                    scheduledDateRange = scheduledDateRange,
                    doneDateRange = doneDateRange,
                    recurrenceDate = recurrenceDate,
                )
            }
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            TaskSearchTopBarAction.Search -> {
                taskSearchViewModel.searchTasks(
                    name = query,
                    deadlineDateRange = deadlineDateRange,
                    startAfterDateRange = startAfterDateRange,
                    scheduledDateRange = scheduledDateRange,
                    doneDateRange = doneDateRange,
                    recurrenceDate = recurrenceDate,
                )
                topBarActionHandled()
            }
        }
    }

    LaunchedEffect(
        deadlineDateRange,
        startAfterDateRange,
        scheduledDateRange,
        doneDateRange,
        recurrenceDate,
    ) {
        taskSearchViewModel.searchTasks(
            name = query,
            deadlineDateRange = deadlineDateRange,
            startAfterDateRange = startAfterDateRange,
            scheduledDateRange = scheduledDateRange,
            doneDateRange = doneDateRange,
            recurrenceDate = recurrenceDate,
        )
    }

    TaskSearchScreen(
        query = query,
        deadlineDateRange = deadlineDateRange,
        onDeadlineChipClick = {
            if (deadlineDateRange == null) {
                filterDialogType = FilterDialogType.DeadlineDateRange
            } else {
                deadlineDateRange = null
            }
        },
        startAfterDateRange = startAfterDateRange,
        onStartAfterChipClick = {
            if (startAfterDateRange == null) {
                filterDialogType = FilterDialogType.StartAfterDateRange
            } else {
                startAfterDateRange = null
            }
        },
        scheduledDateRange = scheduledDateRange,
        onScheduledChipClick = {
            if (scheduledDateRange == null) {
                filterDialogType = FilterDialogType.ScheduledDateRange
            } else {
                scheduledDateRange = null
            }
        },
        doneDateRange = doneDateRange,
        onDoneChipClick = {
            if (doneDateRange == null) {
                filterDialogType = FilterDialogType.DoneDateRange
            } else {
                doneDateRange = null
            }
        },
        recurrenceDate = recurrenceDate,
        onRecurrenceChipClick = {
            if (recurrenceDate == null) {
                filterDialogType = FilterDialogType.RecurrenceDate
            } else {
                recurrenceDate = null
            }
        },
        searchResultItems = searchResultPagingItems,
        uiState = uiState,
        onAddTask = onAddTask,
        onSelectSearchResult = onSelectSearchResult,
    )

    when (filterDialogType) {
        null -> {}
        FilterDialogType.DeadlineDateRange -> {
            DiswantinDateRangePickerDialog(
                onDismiss = { filterDialogType = null },
                dateRange = deadlineDateRange,
                onSelectDateRange = {
                    deadlineDateRange = it
                    scheduledDateRange = null
                },
            )
        }

        FilterDialogType.StartAfterDateRange -> {
            DiswantinDateRangePickerDialog(
                onDismiss = { filterDialogType = null },
                dateRange = startAfterDateRange,
                onSelectDateRange = {
                    startAfterDateRange = it
                    scheduledDateRange = null
                },
            )
        }

        FilterDialogType.ScheduledDateRange -> {
            DiswantinDateRangePickerDialog(
                onDismiss = { filterDialogType = null },
                dateRange = scheduledDateRange,
                onSelectDateRange = {
                    scheduledDateRange = it
                    deadlineDateRange = null
                    startAfterDateRange = null
                },
            )
        }

        FilterDialogType.DoneDateRange -> {
            DiswantinDateRangePickerDialog(
                onDismiss = { filterDialogType = null },
                dateRange = doneDateRange,
                onSelectDateRange = { doneDateRange = it },
            )
        }

        FilterDialogType.RecurrenceDate -> {
            DiswantinDatePickerDialog(
                onDismiss = { filterDialogType = null },
                date = recurrenceDate,
                onSelectDate = { recurrenceDate = it },
            )
        }
    }
}

@Composable
fun TaskSearchScreen(
    query: String,
    deadlineDateRange: Pair<LocalDate, LocalDate>?,
    onDeadlineChipClick: () -> Unit,
    startAfterDateRange: Pair<LocalDate, LocalDate>?,
    onStartAfterChipClick: () -> Unit,
    scheduledDateRange: Pair<LocalDate, LocalDate>?,
    onScheduledChipClick: () -> Unit,
    doneDateRange: Pair<LocalDate, LocalDate>?,
    onDoneChipClick: () -> Unit,
    recurrenceDate: LocalDate?,
    onRecurrenceChipClick: () -> Unit,
    searchResultItems: LazyPagingItems<TaskSummaryUiState>,
    uiState: TaskSearchUiState,
    onAddTask: (String) -> Unit,
    onSelectSearchResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(horizontal = SpaceMd)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SpaceMd),
        ) {
            FilterChip(
                selected = deadlineDateRange != null,
                onClick = onDeadlineChipClick,
                label = {
                    Text(
                        text = deadlineDateRange?.let {
                            stringResource(
                                R.string.deadline_chip_label,
                                it.first.format(dateFormatter),
                                it.second.format(dateFormatter),
                            )
                        } ?: stringResource(R.string.deadline_label),
                    )
                },
            )
            FilterChip(
                selected = startAfterDateRange != null,
                onClick = onStartAfterChipClick,
                label = {
                    Text(
                        text = startAfterDateRange?.let {
                            stringResource(
                                R.string.start_after_chip_label,
                                it.first.format(dateFormatter),
                                it.second.format(dateFormatter),
                            )
                        } ?: stringResource(R.string.start_after_label),
                    )
                },
            )
            FilterChip(
                selected = scheduledDateRange != null,
                onClick = onScheduledChipClick,
                label = {
                    Text(
                        text = scheduledDateRange?.let {
                            stringResource(
                                R.string.scheduled_chip_label,
                                it.first.format(dateFormatter),
                                it.second.format(dateFormatter),
                            )
                        } ?: stringResource(R.string.scheduled_at_label),
                    )
                },
            )
            FilterChip(
                selected = doneDateRange != null,
                onClick = onDoneChipClick,
                label = {
                    Text(
                        text = doneDateRange?.let {
                            stringResource(
                                R.string.done_chip_label_selected,
                                it.first.format(dateFormatter),
                                it.second.format(dateFormatter),
                            )
                        } ?: stringResource(R.string.done_chip_label_unselected),
                    )
                },
            )
            FilterChip(
                selected = recurrenceDate != null,
                onClick = onRecurrenceChipClick,
                label = {
                    Text(text = recurrenceDate?.let {
                        stringResource(
                            R.string.recurrence_chip_label_selected,
                            it.format(dateFormatter),
                        )
                    } ?: stringResource(R.string.recurrence_chip_label_unselected))
                },
            )
        }

        if (uiState.hasCriteria) {
            when (searchResultItems.loadState.refresh) {
                is LoadState.Loading -> PendingLayout()
                is LoadState.Error -> {
                    LoadFailureLayout(message = stringResource(R.string.task_search_error))
                }

                is LoadState.NotLoading -> {
                    if (searchResultItems.itemCount > 0) {
                        TaskSearchLayout(
                            query = query,
                            searchResultItems = searchResultItems,
                            onSelectSearchResult = onSelectSearchResult,
                        )
                    } else {
                        EmptyTaskSearchLayout(onAddTask = { onAddTask(query.trim()) })
                    }
                }
            }
        } else {
            InitialTaskSearchLayout()
        }
    }
}

@Composable
fun TaskSearchLayout(
    query: String,
    searchResultItems: LazyPagingItems<TaskSummaryUiState>,
    onSelectSearchResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskSearchLayout(
        searchResultItems = {
            items(
                searchResultItems.itemCount,
                key = searchResultItems.itemKey(TaskSummaryUiState::id),
            ) { index ->
                val searchResult = searchResultItems[index]!!
                SearchResultItem(
                    searchResult = searchResult,
                    query = query,
                    onSelectSearchResult = onSelectSearchResult,
                )
                HorizontalDivider()
            }

            pagedListFooter(
                pagingItems = searchResultItems,
                errorMessage = {
                    Text(stringResource(R.string.task_search_error))
                },
            )
        },
        modifier = modifier,
    )
}

@Composable
fun TaskSearchLayout(
    searchResultItems: LazyListScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            searchResultItems()
        }
    }
}

@Composable
private fun SearchResultItem(
    searchResult: TaskSummaryUiState, query: String, onSelectSearchResult: (Long) -> Unit
) {
    val resources = LocalResources.current

    ListItem(
        headlineContent = {
            Text(
                text = buildAnnotatedString {
                    append(searchResult.name)
                    searchResult.name.findOccurrences(query).forEach {
                        addStyle(
                            SpanStyle(
                                color = colorScheme.onTertiary, background = colorScheme.tertiary
                            ),
                            it.first,
                            it.last + 1,
                        )
                    }

                    if (searchResult.isDone) {
                        addStyle(
                            SpanStyle(textDecoration = TextDecoration.LineThrough),
                            0,
                            searchResult.name.length,
                        )
                    }
                },
                modifier = if (searchResult.isDone) {
                    Modifier.semantics {
                        contentDescription = resources.getString(
                            R.string.task_name_done,
                            searchResult.name,
                        )
                    }
                } else {
                    Modifier
                },
            )
        },
        modifier = Modifier.clickable { onSelectSearchResult(searchResult.id) },
    )
}

@Composable
fun EmptyTaskSearchLayout(onAddTask: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .imePadding()
            .fillMaxSize(),
        color = colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SpaceMd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(IconSizeLg),
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.task_search_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge,
            )
            Spacer(Modifier.size(SpaceLg))
            ButtonWithIcon(
                onClick = onAddTask,
                imageVector = Icons.Default.Add,
                text = stringResource(R.string.add_task_button),
            )
        }
    }
}

@Composable
fun InitialTaskSearchLayout(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = colorScheme.surfaceVariant),
    )
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Present() {
    val searchResultItems = flowOf(
        PagingData.from(
            listOf(
                TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = true),
                TaskSummaryUiState(id = 2L, name = "Brush hair", isDone = false),
                TaskSummaryUiState(id = 3L, name = "Eat brunch", isDone = false),
            )
        )
    ).collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskSearchTopBar(query = "Bru", onQueryChange = {}, onBackClick = {}, onSearch = {})
            },
        ) { innerPadding ->
            TaskSearchScreen(
                query = "Bru",
                deadlineDateRange = LocalDate.now() to LocalDate.now().plusDays(1),
                onDeadlineChipClick = {},
                startAfterDateRange = LocalDate.now() to LocalDate.now().plusDays(1),
                onStartAfterChipClick = {},
                scheduledDateRange = null,
                onScheduledChipClick = {},
                doneDateRange = null,
                onDoneChipClick = {},
                recurrenceDate = null,
                onRecurrenceChipClick = {},
                searchResultItems = searchResultItems,
                uiState = TaskSearchUiState(hasCriteria = true),
                onAddTask = {},
                onSelectSearchResult = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Empty() {
    DiswantinTheme {
        EmptyTaskSearchLayout(onAddTask = {})
    }
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Initial() {
    val searchResultItems = emptyFlow<PagingData<TaskSummaryUiState>>().collectAsLazyPagingItems()

    DiswantinTheme {
        Scaffold(
            topBar = {
                TaskSearchTopBar(query = "", onQueryChange = {}, onBackClick = {}, onSearch = {})
            },
        ) { innerPadding ->
            TaskSearchScreen(
                query = "",
                deadlineDateRange = null,
                onDeadlineChipClick = {},
                startAfterDateRange = null,
                onStartAfterChipClick = {},
                scheduledDateRange = null,
                onScheduledChipClick = {},
                doneDateRange = null,
                onDoneChipClick = {},
                recurrenceDate = null,
                onRecurrenceChipClick = {},
                searchResultItems = searchResultItems,
                uiState = TaskSearchUiState(hasCriteria = false),
                onAddTask = {},
                onSelectSearchResult = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskSearchLayoutPreview() {
    val searchResults = listOf(
        TaskSummaryUiState(id = 1L, name = "Brush teeth", isDone = true),
        TaskSummaryUiState(id = 2L, name = "Brush hair", isDone = false),
        TaskSummaryUiState(id = 3L, name = "Eat brunch", isDone = false),
    )

    DiswantinTheme {
        Surface {
            TaskSearchLayout(
                searchResultItems = {
                    items(searchResults, TaskSummaryUiState::id) { searchResult ->
                        SearchResultItem(
                            searchResult = searchResult,
                            query = "Bru",
                            onSelectSearchResult = {},
                        )
                        HorizontalDivider()
                    }
                },
            )
        }
    }
}
