package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskListsViewModel @Inject constructor(taskListRepository: TaskListRepository) : ViewModel() {
    val uiState = taskListRepository.taskListsStream.map<List<TaskList>, TaskListsUiState> {
        TaskListsUiState.Success(taskLists = it.toImmutableList())
    }.catch { e ->
        Timber.e(e, "Failed to fetch task lists")
        emit(TaskListsUiState.Failure)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskListsUiState.Pending
    )
}
