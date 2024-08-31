package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.TaskListRepository
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    taskListRepository: TaskListRepository
) : ViewModel() {
    private val taskListId: Long = checkNotNull(savedStateHandle[Destination.TaskListDetail.ID_KEY])

    val uiState =
        taskListRepository.getById(taskListId).map<TaskListWithTasks, TaskListDetailUiState> {
            TaskListDetailUiState.Success(
                taskList = it.taskList,
                tasks = it.tasks.toImmutableList(),
            )
        }.catch { e ->
            Timber.e(e, "Failed to fetch task list by id: %d", taskListId)
            emit(TaskListDetailUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TaskListDetailUiState.Pending,
        )
}
