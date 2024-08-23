package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXs
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Clock
import java.time.Instant

@Composable
fun ActivityDetailScreen(
    popBackStack: () -> Unit,
    navigateToEditActivityForm: (Long) -> Unit,
    activityDetailViewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val uiState by activityDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is ActivityDetailUiState.Removed) {
            popBackStack()
        }
    }

    (uiState as? ActivityDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message, snackbarHostState) {
            snackbarHostState.showSnackbar(resources.getString(message))
            activityDetailViewModel.userMessageShown()
        }
    }

    ActivityDetailScreen(
        onClose = popBackStack,
        editActivity = { navigateToEditActivityForm(it.id) },
        removeActivity = activityDetailViewModel::removeActivity,
        snackbarHostState = snackbarHostState,
        uiState = uiState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onClose: () -> Unit,
    editActivity: (Activity) -> Unit,
    removeActivity: () -> Unit,
    snackbarHostState: SnackbarHostState,
    uiState: ActivityDetailUiState,
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
                        IconButton(onClick = { editActivity(uiState.activity) }) {
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
                                onClick = removeActivity,
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
                ActivityDetailLayout(uiState = uiState, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun ActivityDetailLayout(uiState: ActivityDetailUiState.Success, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .widthIn(max = ScreenLg)
                    .fillMaxWidth()
                    .padding(SpaceMd)
            ) {
                Text(text = uiState.activity.name, style = typography.displaySmall)

                if (uiState.formattedDueAt != null) {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.due_at_label),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                    Spacer(Modifier.size(SpaceXs))
                    Text(text = uiState.formattedDueAt, style = typography.bodyLarge)
                }

                if (uiState.formattedScheduledAt != null) {
                    Spacer(Modifier.size(SpaceMd))
                    Text(
                        stringResource(R.string.scheduled_at_label),
                        color = colorScheme.onSurfaceVariant,
                        style = typography.labelSmall
                    )
                    Spacer(Modifier.size(SpaceXs))
                    Text(text = uiState.formattedScheduledAt, style = typography.bodyLarge)
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
            editActivity = {},
            removeActivity = {},
            snackbarHostState = SnackbarHostState(),
            uiState = ActivityDetailUiState.Success(
                activity = Activity(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth",
                ),
                userMessage = null,
                clock = Clock.systemDefaultZone()
            )
        )
    }
}

@DevicePreviews
@Composable
fun ActivityDetailLayoutPreview() {
    DiswantinTheme {
        Surface {
            ActivityDetailLayout(
                uiState = ActivityDetailUiState.Success(
                    activity = Activity(
                        id = 1L,
                        createdAt = Instant.now(),
                        name = "Brush teeth",
                        dueAt = Instant.now()
                    ),
                    userMessage = null,
                    clock = Clock.systemDefaultZone()
                )
            )
        }
    }
}
