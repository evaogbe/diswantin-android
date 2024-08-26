package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXs
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Clock
import java.time.Instant

@Composable
fun ActivityDetailScreen(
    onPopBackStack: () -> Unit,
    onEditActivity: (Long) -> Unit,
    onSelectChainItem: (Long) -> Unit,
    activityDetailViewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val uiState by activityDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is ActivityDetailUiState.Removed) {
            onPopBackStack()
        }
    }

    (uiState as? ActivityDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, snackbarHostState) {
            snackbarHostState.showSnackbar(resources.getString(message))
            activityDetailViewModel.userMessageShown()
        }
    }

    ActivityDetailScreen(
        onClose = onPopBackStack,
        onEditActivity = { onEditActivity(it.id) },
        onRemoveActivity = activityDetailViewModel::removeActivity,
        snackbarHostState = snackbarHostState,
        uiState = uiState,
        onSelectChainItem = { onSelectChainItem(it.id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onClose: () -> Unit,
    onEditActivity: (Activity) -> Unit,
    onRemoveActivity: () -> Unit,
    snackbarHostState: SnackbarHostState,
    uiState: ActivityDetailUiState,
    onSelectChainItem: (Activity) -> Unit,
) {
    Scaffold(
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }

            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_button)
                        )
                    }
                },
                actions = {
                    if (uiState is ActivityDetailUiState.Success) {
                        IconButton(onClick = { onEditActivity(uiState.activity) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_button)
                            )
                        }

                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_actions_button)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.delete_button)) },
                                onClick = onRemoveActivity,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                },
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when (uiState) {
            is ActivityDetailUiState.Pending,
            is ActivityDetailUiState.Removed -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is ActivityDetailUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.activity_detail_fetch_error),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is ActivityDetailUiState.Success -> {
                ActivityDetailLayout(
                    uiState = uiState,
                    onSelectChainItem = onSelectChainItem,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun ActivityDetailLayout(
    uiState: ActivityDetailUiState.Success,
    onSelectChainItem: (Activity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
                .padding(vertical = SpaceMd)
        ) {
            item {
                SelectionContainer {
                    Text(
                        text = uiState.activity.name,
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        style = typography.displaySmall
                    )
                }
            }

            if (uiState.formattedDueAt != null) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.due_at_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                }

                item {
                    Spacer(Modifier.size(SpaceXs))
                    SelectionContainer {
                        Text(
                            text = uiState.formattedDueAt,
                            modifier = Modifier.padding(horizontal = SpaceMd),
                            style = typography.bodyLarge
                        )
                    }
                }
            }

            if (uiState.formattedScheduledAt != null) {
                item {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.scheduled_at_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                }

                item {
                    Spacer(Modifier.size(SpaceXs))
                    SelectionContainer {
                        Text(
                            text = uiState.formattedScheduledAt,
                            modifier = Modifier.padding(horizontal = SpaceMd),
                            style = typography.bodyLarge
                        )
                    }
                }
            }

            if (uiState.activityChain.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(SpaceLg))
                    Text(
                        stringResource(R.string.activity_chain_label),
                        modifier = Modifier.padding(horizontal = SpaceMd),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                }

                items(uiState.activityChain, key = Activity::id) { activity ->
                    ListItem(
                        headlineContent = { Text(text = activity.name) },
                        modifier = Modifier.clickable { onSelectChainItem(activity) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@DevicePreviews
@Composable
fun ActivityDetailScreenPreview() {
    DiswantinTheme {
        ActivityDetailScreen(
            onClose = {},
            onEditActivity = {},
            onRemoveActivity = {},
            snackbarHostState = SnackbarHostState(),
            uiState = ActivityDetailUiState.Success(
                activity = Activity(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth",
                ),
                activityChain = emptyList(),
                userMessage = null,
                clock = Clock.systemDefaultZone()
            ),
            onSelectChainItem = {},
        )
    }
}

@DevicePreviews
@Composable
fun ActivityDetailLayoutPreview_withActivityChain() {
    DiswantinTheme {
        Surface {
            ActivityDetailLayout(
                uiState = ActivityDetailUiState.Success(
                    activity = Activity(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                    ),
                    activityChain = listOf(
                        Activity(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                            dueAt = Instant.now(),
                        ),
                        Activity(
                            id = 2L,
                            createdAt = Instant.now(),
                            name = "Shower",
                        ),
                        Activity(
                            id = 3L,
                            createdAt = Instant.now(),
                            name = "Eat breakfast",
                        ),
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone()
                ),
                onSelectChainItem = {},
            )
        }
    }
}


@DevicePreviews
@Composable
fun ActivityDetailLayoutPreview_withDueAtAndActivityChain() {
    DiswantinTheme {
        Surface {
            ActivityDetailLayout(
                uiState = ActivityDetailUiState.Success(
                    activity = Activity(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                        dueAt = Instant.now(),
                    ),
                    activityChain = listOf(
                        Activity(
                            id = 1L,
                            createdAt = Instant.now(),
                            name = "Brush teeth",
                            dueAt = Instant.now(),
                        ),
                        Activity(
                            id = 2L,
                            createdAt = Instant.now(),
                            name = "Shower",
                        ),
                        Activity(
                            id = 3L,
                            createdAt = Instant.now(),
                            name = "Eat breakfast",
                        ),
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone()
                ),
                onSelectChainItem = {},
            )
        }
    }
}
