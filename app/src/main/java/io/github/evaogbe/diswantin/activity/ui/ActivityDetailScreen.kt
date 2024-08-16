package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Instant

@Composable
fun ActivityDetailScreen(
    popBackStack: () -> Unit,
    activityDetailViewModel: ActivityDetailViewModel = hiltViewModel()
) {
    val uiState by activityDetailViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val snackbarHostState = remember { SnackbarHostState() }
    val activityFormDialogState = rememberActivityFormDialogState()

    LaunchedEffect(uiState, activityDetailViewModel) {
        if (uiState is ActivityDetailUiState.Removed) {
            activityDetailViewModel.userMessageShown()
            popBackStack()
        }
    }

    (uiState as? ActivityDetailUiState.Success)?.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(resources.getString(message))
            activityDetailViewModel.userMessageShown()
        }
    }

    if ((uiState as? ActivityDetailUiState.Success)?.saveResult?.isSuccess == true) {
        LaunchedEffect(activityFormDialogState) {
            activityFormDialogState.closeDialog()
            activityDetailViewModel.saveHandled()
        }
    }

    ActivityDetailScreen(
        onClose = {
            activityDetailViewModel.userMessageShown()
            popBackStack()
        },
        startEditActivity = { activityFormDialogState.openDialog(it.name) },
        removeActivity = activityDetailViewModel::removeActivity,
        snackbarHostState = snackbarHostState,
        uiState = uiState
    )

    if (activityFormDialogState.mode == ActivityFormDialogState.Mode.OpenForEdit) {
        ActivityFormDialog(
            dismiss = {
                activityFormDialogState.closeDialog()
                activityDetailViewModel.saveHandled()
            },
            saveActivity = {
                activityDetailViewModel.updateActivity(activityFormDialogState.nameInput)
            },
            title = stringResource(R.string.activity_form_heading_edit),
            name = activityFormDialogState.nameInput,
            onNameChange = activityFormDialogState::updateNameInput,
            formError = if (
                (uiState as? ActivityDetailUiState.Success)?.saveResult?.isFailure == true
            ) {
                stringResource(R.string.activity_form_save_error_edit)
            } else {
                null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    onClose: () -> Unit,
    startEditActivity: (Activity) -> Unit,
    removeActivity: () -> Unit,
    snackbarHostState: SnackbarHostState,
    uiState: ActivityDetailUiState
) {
    Scaffold(
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }

            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_button)
                        )
                    }
                },
                actions = {
                    if (uiState is ActivityDetailUiState.Success) {
                        IconButton(onClick = { startEditActivity(uiState.activity) }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_button)
                            )
                        }

                        IconButton(onClick = { menuExpanded = !menuExpanded }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_actions_button)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.delete_button)) },
                                onClick = removeActivity,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                },
                            )
                        }
                    }
                })
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
                    activity = uiState.activity,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun ActivityDetailLayout(activity: Activity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(SpaceMd),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxWidth()
        ) {
            SelectionContainer(modifier = Modifier.padding(SpaceMd)) {
                Text(text = activity.name, style = typography.displaySmall)
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
            startEditActivity = {},
            removeActivity = {},
            snackbarHostState = SnackbarHostState(),
            uiState = ActivityDetailUiState.Success(
                activity = Activity(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth"
                )
            )
        )
    }
}
