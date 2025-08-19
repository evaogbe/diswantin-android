package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskItem
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
    private val locale: Locale,
) : ViewModel() {
    private val taskId: Long = checkNotNull(savedStateHandle[NavArguments.ID_KEY])

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    val uiState = combine(
        initialized,
        taskRepository.getTaskDetailById(taskId).onEach {
            if (it != null) {
                initialized.value = true
            }
        }.map<TaskDetail?, Result<TaskDetail?>> { Result.Success(it) }.catch { e ->
            Timber.e(e, "Failed to fetch task by id: %d", taskId)
            emit(Result.Failure(e))
        },
        taskRepository.getTaskRecurrencesByTaskId(taskId)
            .map<List<TaskRecurrence>, Result<TaskRecurrenceUiState?>> {
                Result.Success(TaskRecurrenceUiState.tryFromEntities(it, locale))
            }.catch { e ->
                Timber.e(e, "Failed to fetch task recurrences by task id: %d", taskId)
                emit(Result.Failure(e))
            },
        taskRepository.getChildren(taskId)
            .map<List<TaskItem>, Result<List<TaskItem>>> { Result.Success(it) }.catch { e ->
                Timber.e(e, "Failed to fetch task children by id: %d", taskId)
                emit(Result.Failure(e))
            },
        userMessage,
    ) { initialized, taskResult, recurrenceResult, childTasksResult, userMessage ->
        taskResult.andThen { task ->
            when {
                task != null -> {
                    recurrenceResult.andThen { recurrence ->
                        childTasksResult.map { childTasks ->
                            val doneBefore =
                                ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
                            TaskDetailUiState.Success(
                                task = task,
                                recurrence = recurrence,
                                childTasks = childTasks.map {
                                    TaskItemUiState.fromTaskItem(it, doneBefore)
                                }.toImmutableList(),
                                userMessage = userMessage,
                                clock = clock,
                            )
                        }
                    }
                }

                initialized -> Result.Success(TaskDetailUiState.Deleted)
                else -> Result.Failure(NullPointerException("Task with id $taskId not found"))
            }
        }.fold(onSuccess = { it }, onFailure = TaskDetailUiState::Failure)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskDetailUiState.Pending
    )

    fun markTaskDone() {
        val task = (uiState.value as? TaskDetailUiState.Success)?.task ?: return

        viewModelScope.launch {
            try {
                taskRepository.markDone(task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark task done: %s", task)
                userMessage.value = UserMessage.String(R.string.task_detail_mark_done_error)
            }
        }
    }

    fun unmarkTaskDone() {
        val task = (uiState.value as? TaskDetailUiState.Success)?.task ?: return

        viewModelScope.launch {
            try {
                taskRepository.unmarkDone(task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to unmark task done: %s", task)
                userMessage.value = UserMessage.String(R.string.task_detail_unmark_done_error)
            }
        }
    }

    fun deleteTask() {
        val task = (uiState.value as? TaskDetailUiState.Success)?.task ?: return

        viewModelScope.launch {
            try {
                taskRepository.delete(task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete task: %s", task)
                userMessage.value = UserMessage.String(R.string.task_detail_delete_error)
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
