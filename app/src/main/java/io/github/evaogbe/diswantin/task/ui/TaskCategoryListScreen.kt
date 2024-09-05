package io.github.evaogbe.diswantin.task.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.ui.components.LoadFailureLayout
import io.github.evaogbe.diswantin.ui.components.PendingLayout
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.evaogbe.diswantin.ui.theme.IconSizeLg
import io.github.evaogbe.diswantin.ui.theme.ScreenLg
import io.github.evaogbe.diswantin.ui.theme.SpaceLg
import io.github.evaogbe.diswantin.ui.theme.SpaceXl
import io.github.evaogbe.diswantin.ui.tooling.DevicePreviews
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCategoryListTopBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        title = {},
        modifier = modifier,
        actions = {
            IconButton(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_tasks_button),
                )
            }
        },
    )
}

@Composable
fun TaskCategoryListScreen(
    onAddCategory: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    taskCategoryListViewModel: TaskCategoryListViewModel = hiltViewModel(),
) {
    val uiState by taskCategoryListViewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is TaskCategoryListUiState.Pending -> PendingLayout()
        is TaskCategoryListUiState.Failure -> {
            LoadFailureLayout(message = stringResource(R.string.task_category_list_fetch_error))
        }

        is TaskCategoryListUiState.Success -> {
            if (state.categories.isEmpty()) {
                EmptyTaskCategoryListLayout(onAddCategory = onAddCategory)
            } else {
                TaskCategoryListLayout(
                    categories = state.categories,
                    onSelectCategory = { onSelectCategory(it.id) },
                )
            }
        }
    }
}

@Composable
fun TaskCategoryListLayout(
    categories: ImmutableList<TaskCategory>,
    onSelectCategory: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = ScreenLg)
                .fillMaxSize(),
        ) {
            items(categories, key = TaskCategory::id) { category ->
                ListItem(
                    headlineContent = { Text(text = category.name) },
                    modifier = Modifier.clickable { onSelectCategory(category) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EmptyTaskCategoryListLayout(onAddCategory: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = colorScheme.surfaceVariant) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.list_alt_add_24px),
                contentDescription = null,
                modifier = Modifier.size(IconSizeLg),
            )
            Spacer(Modifier.size(SpaceXl))
            Text(
                stringResource(R.string.task_category_list_empty),
                textAlign = TextAlign.Center,
                style = typography.headlineLarge,
            )
            Spacer(Modifier.size(SpaceLg))
            Button(
                onClick = onAddCategory,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_task_category_button))
            }
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryListScreenPreview_Present() {
    DiswantinTheme {
        Scaffold(topBar = { TaskCategoryListTopBar(onSearch = {}) }) { innerPadding ->
            TaskCategoryListLayout(
                categories = persistentListOf(
                    TaskCategory(id = 1L, name = "Morning routine"),
                    TaskCategory(id = 2L, name = "Work"),
                    TaskCategory(id = 3L, name = "Bedtime routine")
                ),
                onSelectCategory = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@DevicePreviews
@Composable
private fun TaskCategoryListScreenPreview_Empty() {
    DiswantinTheme {
        Scaffold(topBar = { TaskCategoryListTopBar(onSearch = {}) }) { innerPadding ->
            EmptyTaskCategoryListLayout(
                onAddCategory = {},
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
