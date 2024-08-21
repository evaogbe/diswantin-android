package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceMd
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import java.time.Instant

@Composable
fun CurrentActivityScreen(
    navigateToActivitySearch: () -> Unit,
    navigateToNewActivityForm: () -> Unit,
    navigateToEditActivityForm: (Long) -> Unit,
    currentActivityViewModel: CurrentActivityViewModel = hiltViewModel()
) {
    val uiState by currentActivityViewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val snackbarHostState = remember { SnackbarHostState() }

    currentActivityViewModel.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(resources.getString(message))
            currentActivityViewModel.userMessageShown()
        }
    }

    CurrentActivityScreen(
        onSearch = {
            currentActivityViewModel.userMessageShown()
            navigateToActivitySearch()
        },
        onAddActivity = navigateToNewActivityForm,
        onEditActivity = { navigateToEditActivityForm(it.id) },
        snackbarHostState = snackbarHostState,
        uiState = uiState,
        skipActivity = currentActivityViewModel::skipCurrentActivity,
        removeActivity = currentActivityViewModel::removeCurrentActivity
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentActivityScreen(
    onSearch: () -> Unit,
    onAddActivity: () -> Unit,
    onEditActivity: (Activity) -> Unit,
    snackbarHostState: SnackbarHostState,
    uiState: CurrentActivityUiState,
    skipActivity: () -> Unit,
    removeActivity: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.current_activity_title)) },
                actions = {
                    val activity = (uiState as? CurrentActivityUiState.Present)?.currentActivity

                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_activities_button)
                        )
                    }

                    if (activity != null) {
                        IconButton(onClick = { onEditActivity(activity) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_button)
                            )
                        }
                    }
                })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivity) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_activity_button)
                )
            }
        }
    ) { innerPadding ->
        when (uiState) {
            is CurrentActivityUiState.Pending -> {
                PendingLayout(modifier = Modifier.padding(innerPadding))
            }

            is CurrentActivityUiState.Failure -> {
                LoadFailureLayout(
                    message = stringResource(R.string.current_activity_fetch_error),
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is CurrentActivityUiState.Empty -> {
                EmptyCurrentActivityLayout(
                    onAddActivity = onAddActivity,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is CurrentActivityUiState.Present -> {
                CurrentActivityLayout(
                    activity = uiState.currentActivity,
                    skipActivity = skipActivity,
                    removeActivity = removeActivity,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun CurrentActivityLayout(
    activity: Activity,
    skipActivity: () -> Unit,
    removeActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SpaceMd),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SelectionContainer(Modifier.widthIn(max = ScreenLg)) {
            Text(text = activity.name, style = typography.displaySmall)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            OutlinedButton(onClick = skipActivity) {
                Text(stringResource(R.string.skip_button))
            }
            OutlinedButton(onClick = removeActivity) {
                Text(stringResource(R.string.remove_button))
            }
        }
    }
}

@Composable
fun EmptyCurrentActivityLayout(onAddActivity: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpaceMd),
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
                stringResource(R.string.current_activity_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge
            )
            Spacer(Modifier.size(SpaceLg))
            Button(onClick = onAddActivity) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_activity_button))
            }
        }
    }
}

@DevicePreviews
@Composable
fun CurrentActivityScreenPreview() {
    DiswantinTheme {
        CurrentActivityScreen(
            onSearch = {},
            onAddActivity = {},
            onEditActivity = {},
            snackbarHostState = SnackbarHostState(),
            uiState = CurrentActivityUiState.Present(
                Activity(
                    id = 1L,
                    createdAt = Instant.now(),
                    name = "Brush teeth"
                )
            ),
            skipActivity = {},
            removeActivity = {}
        )
    }
}

@DevicePreviews
@Composable
fun EmptyCurrentActivityLayoutPreview() {
    DiswantinTheme {
        EmptyCurrentActivityLayout(onAddActivity = {})
    }
}
