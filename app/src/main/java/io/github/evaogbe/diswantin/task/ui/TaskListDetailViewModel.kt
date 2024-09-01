package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskListRepository
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TaskListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskListRepository: TaskListRepository,
    clock: Clock,
) : ViewModel() {
    private val taskListId: Long = checkNotNull(savedStateHandle[Destination.TaskListDetail.ID_KEY])

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<Int?>(null)

    val uiState =
        combine(
            initialized,
            taskListRepository.getTaskListWithTaskItemsById(taskListId),
            userMessage,
        ) { initialized, taskListWithTasks, userMessage ->
            when {
                taskListWithTasks != null -> {
                    val midnight = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.DAYS).toInstant()
                    TaskListDetailUiState.Success(
                        taskList = taskListWithTasks.taskList,
                        tasks = taskListWithTasks.tasks.map { task ->
                            TaskItemState(
                                id = task.id,
                                name = task.name,
                                recurring = task.recurring,
                                isDone = if (task.recurring) {
                                    task.doneAt?.let { it < midnight } == false
                                } else {
                                    task.doneAt != null
                                },
                            )
                        }.toImmutableList(),
                        userMessage = userMessage,
                    )
                }

                initialized -> TaskListDetailUiState.Deleted
                else -> TaskListDetailUiState.Failure
            }
        }.onEach {
            if (it is TaskListDetailUiState.Success) {
                initialized.value = true
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch task list by id: %d", taskListId)
            emit(TaskListDetailUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TaskListDetailUiState.Pending,
        )

    fun deleteTaskList() {
        val taskList = (uiState.value as? TaskListDetailUiState.Success)?.taskList ?: return

        viewModelScope.launch {
            try {
                taskListRepository.delete(taskList)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete taskList: %s", taskList)
                userMessage.value = R.string.task_list_detail_delete_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
