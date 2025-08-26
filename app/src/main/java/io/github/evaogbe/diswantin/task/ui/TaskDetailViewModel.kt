package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
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
    tagRepository: TagRepository,
    clock: Clock,
    locale: Locale,
) : ViewModel() {
    private val taskId = savedStateHandle.toRoute<TaskDetailRoute>().id

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    val childTaskPagingData = taskRepository.getChildren(taskId).map { pagingData ->
        val doneBefore = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
        pagingData.map { TaskSummaryUiState.fromTaskSummary(it, doneBefore) }
    }.cachedIn(viewModelScope)

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
        tagRepository.getTagsByTaskId(taskId, size = Task.MAX_TAGS)
            .map<List<Tag>, Result<List<Tag>>> { Result.Success(it) }.catch { e ->
                Timber.e(e, "Failed to fetch tags by task id: %d", taskId)
                emit(Result.Failure(e))
            },
        taskRepository.getTaskRecurrencesByTaskId(taskId)
            .map<List<TaskRecurrence>, Result<TaskRecurrenceUiState?>> {
                Result.Success(TaskRecurrenceUiState.tryFromEntities(it, locale))
            }.catch { e ->
                Timber.e(e, "Failed to fetch task recurrences by task id: %d", taskId)
                emit(Result.Failure(e))
            },
        userMessage,
    ) { initialized, taskResult, tagsResult, recurrenceResult, userMessage ->
        taskResult.andThen { task ->
            when {
                task != null -> {
                    tagsResult.zipWith(recurrenceResult) { tags, recurrence ->
                        val doneBefore = ZonedDateTime.now(clock).with(LocalTime.MIN).toInstant()
                        TaskDetailUiState.success(
                            task = task,
                            tags = tags,
                            recurrence = recurrence,
                            userMessage = userMessage,
                            doneBefore = doneBefore,
                        )
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
        viewModelScope.launch {
            try {
                taskRepository.markDone(taskId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark task done: %s", taskId)
                userMessage.value = UserMessage.String(R.string.task_detail_mark_done_error)
            }
        }
    }

    fun unmarkTaskDone() {
        viewModelScope.launch {
            try {
                taskRepository.unmarkDone(taskId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to unmark task done: %s", taskId)
                userMessage.value = UserMessage.String(R.string.task_detail_unmark_done_error)
            }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            try {
                taskRepository.delete(taskId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete task: %s", taskId)
                userMessage.value = UserMessage.String(R.string.task_detail_delete_error)
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
