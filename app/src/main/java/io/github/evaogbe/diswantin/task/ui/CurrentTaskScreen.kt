package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.ui.button.ButtonWithIcon
import io.github.evaogbe.diswantin.ui.button.FilledTonalButtonWithIcon
import io.github.evaogbe.diswantin.ui.button.OutlinedButtonWithIcon
import io.github.evaogbe.diswantin.ui.loadstate.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayout
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarHandler
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceSm
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentTaskTopBar(
    uiState: CurrentTaskTopBarState,
    onRefresh: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {},
        modifier = modifier,
        actions = {
            IconButton(onClick = { menuExpanded = !menuExpanded }) {
                Icon(
                    painterResource(R.drawable.outline_more_vert_24),
                    contentDescription = stringResource(R.string.more_actions_button),
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.refresh_button)) },
                    onClick = {
                        onRefresh()
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.outline_refresh_24),
                            contentDescription = null,
                        )
                    },
                )

                if (uiState.canSkip) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.skip_button)) },
                        onClick = {
                            onSkip()
                            menuExpanded = false
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.baseline_skip_next_24),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        },
    )
}

enum class DismissSkipSheetReason {
    ClickOutside, Skip, ShowAdvice
}

@Composable
fun CurrentTaskScreen(
    setTopBarState: (CurrentTaskTopBarState) -> Unit,
    topBarAction: CurrentTaskTopBarAction?,
    topBarActionHandled: () -> Unit,
    showSnackbar: SnackbarHandler,
    onNavigateToAdvice: () -> Unit,
    onAddTask: () -> Unit,
    onNavigateToTask: (Long) -> Unit,
    currentTaskViewModel: CurrentTaskViewModel = hiltViewModel(),
) {
    val currentTopBarActionHandled by rememberUpdatedState(topBarActionHandled)
    val currentShowSnackbar by rememberUpdatedState(showSnackbar)
    val uiState by currentTaskViewModel.uiState.collectAsStateWithLifecycle()
    val canSkip by remember {
        derivedStateOf {
            (uiState as? CurrentTaskUiState.Present)?.canSkip == true
        }
    }
    val userMessage by currentTaskViewModel.userMessage.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        currentTaskViewModel.refresh()
    }

    LaunchedEffect(lifecycleOwner) {
        flow {
            while (true) {
                delay(1.hours)
                emit(Unit)
            }
        }.flowWithLifecycle(lifecycleOwner.lifecycle).collectLatest {
            currentTaskViewModel.refresh()
        }
    }

    LaunchedEffect(canSkip, setTopBarState) {
        setTopBarState(CurrentTaskTopBarState(canSkip = canSkip))
    }

    LaunchedEffect(topBarAction) {
        when (topBarAction) {
            null -> {}
            CurrentTaskTopBarAction.Refresh -> {
                currentTaskViewModel.refresh()
                currentTopBarActionHandled()
            }

            CurrentTaskTopBarAction.Skip -> {
                showBottomSheet = true
                currentTopBarActionHandled()
            }
        }
    }

    LaunchedEffect(userMessage) {
        when (userMessage) {
            null -> {}

            CurrentTaskUserMessage.SkipError -> {
                currentShowSnackbar(
                    SnackbarState.create(
                        currentResources.getString(R.string.current_task_skip_error)
                    )
                )
                currentTaskViewModel.userMessageShown()
            }

            CurrentTaskUserMessage.MarkDoneError -> {
                currentShowSnackbar(
                    SnackbarState.create(
                        currentResources.getString(R.string.current_task_mark_done_error)
                    )
                )
                currentTaskViewModel.userMessageShown()
            }
        }
    }

    when (val state = uiState) {
        is CurrentTaskUiState.Pending -> PendingLayout()
        is CurrentTaskUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.current_task_fetch_error))
        }

        is CurrentTaskUiState.Empty -> {
            EmptyCurrentTaskLayout(
                isRefreshing = state.isRefreshing,
                onRefresh = currentTaskViewModel::refresh,
                onAddTask = onAddTask,
            )
        }

        is CurrentTaskUiState.Present -> {
            CurrentTaskLayout(
                uiState = state,
                onRefresh = currentTaskViewModel::refresh,
                onNavigateToTask = onNavigateToTask,
                onMarkTaskDone = currentTaskViewModel::markCurrentTaskDone,
            )
        }
    }

    if (showBottomSheet) {
        SkipSheet(
            onDismiss = { reason ->
                showBottomSheet = false
                when (reason) {
                    DismissSkipSheetReason.Skip -> currentTaskViewModel.skipCurrentTask()
                    DismissSkipSheetReason.ShowAdvice -> onNavigateToAdvice()
                    DismissSkipSheetReason.ClickOutside -> {}
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SkipSheet(
    onDismiss: (DismissSkipSheetReason) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onDismiss(DismissSkipSheetReason.ClickOutside) },
        sheetState = sheetState
    ) {
        SkipSheetLayout(
            onDismiss = { reason ->
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss(reason)
                }
            },
        )
    }
}

@Composable
private fun SkipSheetLayout(onDismiss: (DismissSkipSheetReason) -> Unit) {
    Column(
        modifier = Modifier.padding(SpaceMd),
    ) {
        Text(stringResource(R.string.skip_sheet_title), style = typography.titleLarge)
        Spacer(Modifier.size(SpaceSm))
        Text(stringResource(R.string.skip_sheet_text))
        Spacer(Modifier.size(SpaceMd))
        HorizontalDivider()
        Spacer(Modifier.size(SpaceMd))
        TextButton(
            onClick = { onDismiss(DismissSkipSheetReason.ShowAdvice) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.skip_sheet_dismiss_button))
        }
        Spacer(Modifier.size(SpaceSm))
        TextButton(
            onClick = { onDismiss(DismissSkipSheetReason.Skip) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.skip_sheet_confirm_button))
        }
    }
}

@Composable
fun CurrentTaskLayout(
    uiState: CurrentTaskUiState.Present,
    onRefresh: () -> Unit,
    onNavigateToTask: (Long) -> Unit,
    onMarkTaskDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh) {
            Column(
                modifier = Modifier
                    .padding(horizontal = SpaceLg, vertical = SpaceMd)
                    .widthIn(max = ScreenLg)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceAround,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SelectionContainer {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.name,
                            textAlign = TextAlign.Center,
                            style = typography.displaySmall
                        )
                        Spacer(Modifier.size(SpaceMd))

                        if (uiState.note.isNotEmpty()) {
                            Text(
                                text = uiState.note,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                style = typography.titleLarge,
                            )
                            Spacer(Modifier.size(SpaceMd))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    OutlinedButtonWithIcon(
                        onClick = { onNavigateToTask(uiState.id) },
                        painter = painterResource(R.drawable.baseline_receipt_24),
                        text = stringResource(R.string.current_task_view_details_button),
                    )
                    FilledTonalButtonWithIcon(
                        onClick = onMarkTaskDone,
                        painter = painterResource(R.drawable.baseline_done_24),
                        text = stringResource(R.string.current_task_mark_done_button),
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCurrentTaskLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SpaceMd)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_add_task_24),
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeLg),
                )
                Spacer(Modifier.size(SpaceXl))
                Text(
                    stringResource(R.string.current_task_empty),
                    textAlign = TextAlign.Center,
                    style = typography.headlineLarge
                )
                Spacer(Modifier.size(SpaceLg))
                ButtonWithIcon(
                    onClick = onAddTask,
                    painter = painterResource(R.drawable.baseline_add_24),
                    text = stringResource(R.string.add_task_button),
                )
            }
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskScreenPreview_Present() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                CurrentTaskTopBar(
                    uiState = CurrentTaskTopBarState(canSkip = true),
                    onRefresh = {},
                    onSkip = {},
                )
            },
        ) { innerPadding ->
            CurrentTaskLayout(
                uiState = CurrentTaskUiState.Present(
                    id = 1L,
                    name = "Brush teeth",
                    note = "",
                    isRefreshing = false,
                    canSkip = true,
                ),
                onRefresh = {},
                onNavigateToTask = {},
                onMarkTaskDone = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskScreenPreview_withNote() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                CurrentTaskTopBar(
                    uiState = CurrentTaskTopBarState(canSkip = false),
                    onRefresh = {},
                    onSkip = {},
                )
            },
        ) { innerPadding ->
            CurrentTaskLayout(
                uiState = CurrentTaskUiState.Present(
                    id = 1L,
                    name = "Brush teeth",
                    note = "Don't forget to floss and rinse with mouthwash",
                    isRefreshing = false,
                    canSkip = false,
                ),
                onRefresh = {},
                onNavigateToTask = {},
                onMarkTaskDone = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun CurrentTaskScreenPreview_Empty() {
    DiswantinTheme {
        Scaffold(
            topBar = {
                CurrentTaskTopBar(
                    uiState = CurrentTaskTopBarState(canSkip = false),
                    onRefresh = {},
                    onSkip = {},
                )
            },
        ) { innerPadding ->
            EmptyCurrentTaskLayout(
                isRefreshing = false,
                onRefresh = {},
                onAddTask = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SkipSheetLayoutPreview() {
    DiswantinTheme {
        Surface {
            SkipSheetLayout(onDismiss = {})
        }
    }
}
