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
import androidx.compose.ui.platform.LocalContext
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
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import io.github.evaogbe.diswantin.ui.components.AutoFocusTextField
import io.github.evaogbe.diswantin.ui.components.ButtonWithIcon
import io.github.evaogbe.diswantin.ui.components.DiswantinDateRangePickerDialog
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    DeadlineDateRange, ScheduledDateRange
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
    val uiState by taskSearchViewModel.uiState.collectAsStateWithLifecycle()
    var deadlineDateRange by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var scheduledDateRange by rememberSaveable { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var filterDialogType by rememberSaveable { mutableStateOf<FilterDialogType?>(null) }
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(taskSearchViewModel) {
        snapshotFlow { currentQuery }
            .debounce(150.milliseconds)
            .distinctUntilChanged()
            .collectLatest {
                taskSearchViewModel.searchTasks(
                    TaskSearchCriteria(
                        name = it,
                        deadlineDateRange = deadlineDateRange,
                        scheduledDateRange = scheduledDateRange,
                    ),
                )
            }
    }

    LaunchedEffect(topBarAction, taskSearchViewModel) {
        when (topBarAction) {
            null -> {}
            TaskSearchTopBarAction.Search -> {
                taskSearchViewModel.searchTasks(
                    TaskSearchCriteria(
                        name = query,
                        deadlineDateRange = deadlineDateRange,
                        scheduledDateRange = scheduledDateRange,
                    ),
                )
                topBarActionHandled()
            }
        }
    }

    LaunchedEffect(deadlineDateRange, scheduledDateRange) {
        taskSearchViewModel.searchTasks(
            TaskSearchCriteria(
                name = query,
                deadlineDateRange = deadlineDateRange,
                scheduledDateRange = scheduledDateRange,
            ),
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
        scheduledDateRange = scheduledDateRange,
        onScheduledChipClick = {
            if (scheduledDateRange == null) {
                filterDialogType = FilterDialogType.ScheduledDateRange
            } else {
                scheduledDateRange = null
            }
        },
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

        FilterDialogType.ScheduledDateRange -> {
            DiswantinDateRangePickerDialog(
                onDismiss = { filterDialogType = null },
                dateRange = scheduledDateRange,
                onSelectDateRange = {
                    scheduledDateRange = it
                    deadlineDateRange = null
                },
            )
        }
    }
}

@Composable
fun TaskSearchScreen(
    query: String,
    deadlineDateRange: Pair<LocalDate, LocalDate>?,
    onDeadlineChipClick: () -> Unit,
    scheduledDateRange: Pair<LocalDate, LocalDate>?,
    onScheduledChipClick: () -> Unit,
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
        }

        when (uiState) {
            is TaskSearchUiState.Initial -> InitialTaskSearchLayout()

            is TaskSearchUiState.Failure -> {
                LoadFailureLayout(message = stringResource(R.string.task_search_error))
            }

            is TaskSearchUiState.Success -> {
                if (uiState.searchResults.isEmpty()) {
                    EmptyTaskSearchLayout(onAddTask = { onAddTask(query.trim()) })
                } else {
                    TaskSearchLayout(
                        query = query,
                        searchResults = uiState.searchResults,
                        onSelectSearchResult = onSelectSearchResult,
                    )
                }
            }
        }
    }
}

@Composable
fun TaskSearchLayout(
    query: String,
    searchResults: ImmutableList<TaskItemUiState>,
    onSelectSearchResult: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            items(searchResults, key = TaskItemUiState::id) { searchResult ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = buildAnnotatedString {
                                append(searchResult.name)
                                searchResult.name.findOccurrences(query).forEach {
                                    addStyle(
                                        SpanStyle(
                                            color = colorScheme.onTertiary,
                                            background = colorScheme.tertiary
                                        ),
                                        it.first,
                                        it.last + 1
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
                    modifier = Modifier.clickable { onSelectSearchResult(searchResult.id) }
                )
                HorizontalDivider()
            }
        }
    }
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
                scheduledDateRange = null,
                onScheduledChipClick = {},
                uiState = TaskSearchUiState.Success(
                    searchResults = persistentListOf(
                        TaskItemUiState(id = 1L, name = "Brush teeth", isDone = true),
                        TaskItemUiState(id = 2L, name = "Brush hair", isDone = false),
                        TaskItemUiState(id = 3L, name = "Eat brunch", isDone = false),
                    ),
                ),
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
        Scaffold(
            topBar = {
                TaskSearchTopBar(query = "Bru", onQueryChange = {}, onBackClick = {}, onSearch = {})
            },
        ) { innerPadding ->
            TaskSearchScreen(
                query = "Bru",
                deadlineDateRange = null,
                onDeadlineChipClick = {},
                scheduledDateRange = LocalDate.now() to LocalDate.now().plusDays(1),
                onScheduledChipClick = {},
                uiState = TaskSearchUiState.Success(searchResults = persistentListOf()),
                onAddTask = {},
                onSelectSearchResult = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Initial() {
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
                scheduledDateRange = null,
                onScheduledChipClick = {},
                uiState = TaskSearchUiState.Initial,
                onAddTask = {},
                onSelectSearchResult = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
