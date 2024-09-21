package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskCategoryListViewModel @Inject constructor(
    taskCategoryRepository: TaskCategoryRepository,
) : ViewModel() {
    val uiState =
        taskCategoryRepository.categoriesStream
            .map<List<TaskCategory>, TaskCategoryListUiState> {
                TaskCategoryListUiState.Success(categories = it.toImmutableList())
            }
            .catch { e ->
                Timber.e(e, "Failed to fetch task categories")
                emit(TaskCategoryListUiState.Failure)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = TaskCategoryListUiState.Pending,
            )
}
