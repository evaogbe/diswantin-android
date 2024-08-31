package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.ui.components.AutoFocusTextField
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
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
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun TaskSearchScreen(
    onBackClick: () -> Unit,
    onAddTask: (String) -> Unit,
    onSelectSearchResult: (Long) -> Unit,
    taskSearchViewModel: TaskSearchViewModel = hiltViewModel(),
) {
    val uiState by taskSearchViewModel.uiState.collectAsStateWithLifecycle()
    val (query, setQuery) = rememberSaveable { mutableStateOf("") }
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(taskSearchViewModel) {
        snapshotFlow { currentQuery }
            .debounce(150.milliseconds)
            .distinctUntilChanged()
            .collectLatest {
                taskSearchViewModel.searchTasks(it)
            }
    }

    TaskSearchScreen(
        query = query,
        onQueryChange = setQuery,
        onSearch = taskSearchViewModel::searchTasks,
        onBackClick = onBackClick,
        uiState = uiState,
        onAddTask = { onAddTask(query) },
        onSelectSearchResult = { onSelectSearchResult(it.id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBackClick: () -> Unit,
    uiState: TaskSearchUiState,
    onAddTask: () -> Unit,
    onSelectSearchResult: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    AutoFocusTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.task_search_title)) },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is TaskSearchUiState.Initial -> {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(color = colorScheme.surfaceVariant),
                )
            }

            is TaskSearchUiState.Pending -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is TaskSearchUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.task_search_error),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is TaskSearchUiState.Success -> {
                if (uiState.searchResults.isEmpty()) {
                    EmptyTaskSearchLayout(
                        onAddTask = onAddTask,
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    TaskSearchLayout(
                        query = query,
                        searchResults = uiState.searchResults,
                        onSelectSearchResult = onSelectSearchResult,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TaskSearchLayout(
    query: String,
    searchResults: ImmutableList<Task>,
    onSelectSearchResult: (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            items(searchResults, key = Task::id) { searchResult ->
                ListItem(
                    headlineContent = {
                        Text(text = buildAnnotatedString {
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
                        })
                    },
                    modifier = Modifier.clickable { onSelectSearchResult(searchResult) }
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
                .padding(SpaceMd),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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
                style = typography.headlineLarge
            )
            Spacer(Modifier.size(SpaceLg))
            Button(
                onClick = onAddTask,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_task_button))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Present() {
    DiswantinTheme {
        TaskSearchScreen(
            query = "Bru",
            onQueryChange = {},
            onSearch = {},
            onBackClick = {},
            uiState = TaskSearchUiState.Success(
                searchResults = persistentListOf(
                    Task(
                        id = 1L,
                        createdAt = Instant.parse("2024-08-09T08:00:00Z"),
                        name = "Brush teeth",
                    ),
                    Task(
                        id = 2L,
                        createdAt = Instant.parse("2024-08-09T08:05:00Z"),
                        name = "Brush hair",
                    ),
                    Task(
                        id = 3L,
                        createdAt = Instant.parse("2024-08-09T08:10:00Z"),
                        name = "Eat brunch",
                    ),
                )
            ),
            onAddTask = {},
            onSelectSearchResult = {}
        )
    }
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Empty() {
    DiswantinTheme {
        TaskSearchScreen(
            query = "Bru",
            onQueryChange = {},
            onSearch = {},
            onBackClick = {},
            uiState = TaskSearchUiState.Success(searchResults = persistentListOf()),
            onAddTask = {},
            onSelectSearchResult = {}
        )
    }
}

@DevicePreviews
@Composable
private fun TaskSearchScreenPreview_Initial() {
    DiswantinTheme {
        TaskSearchScreen(
            query = "",
            onQueryChange = {},
            onSearch = {},
            onBackClick = {},
            uiState = TaskSearchUiState.Initial,
            onAddTask = {},
            onSelectSearchResult = {}
        )
    }
}
