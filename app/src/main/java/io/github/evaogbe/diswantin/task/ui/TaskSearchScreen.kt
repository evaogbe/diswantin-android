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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.rememberUpdatedState
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
import io.github.evaogbe.diswantin.ui.components.AutoFocusTextField
import io.github.evaogbe.diswantin.ui.components.ButtonWithIcon
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
    val currentQuery by rememberUpdatedState(query)

    LaunchedEffect(taskSearchViewModel) {
        snapshotFlow { currentQuery }
            .debounce(150.milliseconds)
            .distinctUntilChanged()
            .collectLatest {
                taskSearchViewModel.searchTasks(it)
            }
    }

    LaunchedEffect(topBarAction, taskSearchViewModel) {
        when (topBarAction) {
            null -> {}
            TaskSearchTopBarAction.Search -> {
                taskSearchViewModel.searchTasks(query)
                topBarActionHandled()
            }
        }
    }

    when (val state = uiState) {
        is TaskSearchUiState.Initial -> InitialTaskSearchLayout()

        is TaskSearchUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_search_error))
        }

        is TaskSearchUiState.Success -> {
            if (state.searchResults.isEmpty()) {
                EmptyTaskSearchLayout(onAddTask = { onAddTask(query.trim()) })
            } else {
                TaskSearchLayout(
                    query = query,
                    searchResults = state.searchResults,
                    onSelectSearchResult = onSelectSearchResult,
                )
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
            TaskSearchLayout(
                query = "Bru",
                searchResults = persistentListOf(
                    TaskItemUiState(id = 1L, name = "Brush teeth", isDone = true),
                    TaskItemUiState(id = 2L, name = "Brush hair", isDone = false),
                    TaskItemUiState(id = 3L, name = "Eat brunch", isDone = false),
                ),
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
            EmptyTaskSearchLayout(onAddTask = {}, modifier = Modifier.padding(innerPadding))
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
            InitialTaskSearchLayout(modifier = Modifier.padding(innerPadding))
        }
    }
}
