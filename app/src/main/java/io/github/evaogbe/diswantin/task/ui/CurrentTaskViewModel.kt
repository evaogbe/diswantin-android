package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.CurrentTaskParams
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class CurrentTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val currentTaskParams = MutableStateFlow(CurrentTaskParams(ZonedDateTime.now(clock)))

    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing = _isRefreshing.asStateFlow()

    private val _userMessage = MutableStateFlow<UserMessage?>(null)

    val userMessage = _userMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = currentTaskParams.flatMapLatest {
        _isRefreshing.value = true
        taskRepository.getCurrentTask(it)
            .onEach { _isRefreshing.value = false }
            .map<Task?, Result<Task?>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch current task")
                emit(Result.Failure(e))
            }
    }
        .flatMapLatest { taskResult ->
            taskResult.fold(
                onSuccess = { task ->
                    task?.let { t ->
                        taskRepository.getTaskRecurrencesByTaskId(t.id)
                            .map<List<TaskRecurrence>, CurrentTaskUiState> {
                                CurrentTaskUiState.Present(
                                    currentTask = t,
                                    canSkip = it.isNotEmpty(),
                                )
                            }
                            .catch { e ->
                                Timber.e(e, "Failed to fetch current task recurrences")
                                _userMessage.value = UserMessage.String(
                                    R.string.current_task_fetch_recurrences_error,
                                )
                                emit(CurrentTaskUiState.Present(currentTask = t, canSkip = false))
                            }
                    } ?: flowOf(CurrentTaskUiState.Empty)
                },
                onFailure = { flowOf(CurrentTaskUiState.Failure(it)) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CurrentTaskUiState.Pending,
        )

    fun refresh() {
        currentTaskParams.value = CurrentTaskParams(ZonedDateTime.now(clock))
    }

    fun skipCurrentTask() {
        val task = (uiState.value as? CurrentTaskUiState.Present)?.currentTask ?: return

        viewModelScope.launch {
            try {
                taskRepository.skip(task.id)
                currentTaskParams.value = CurrentTaskParams(ZonedDateTime.now(clock))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to skip task: %s", task)
                _userMessage.value = UserMessage.String(R.string.current_task_skip_error)
            }
        }
    }

    fun markCurrentTaskDone() {
        val task = (uiState.value as? CurrentTaskUiState.Present)?.currentTask ?: return

        viewModelScope.launch {
            var markedDone = false

            try {
                taskRepository.markDone(task.id)
                markedDone = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark task done: %s", task)
                _userMessage.value = UserMessage.String(R.string.current_task_mark_done_error)
            }

            if (markedDone) {
                currentTaskParams.value = CurrentTaskParams(ZonedDateTime.now(clock))

                try {
                    val completionCount = taskRepository.getCompletionCount().first().toInt()
                    if (completionCount % 50 == 0) {
                        _userMessage.value = UserMessage.Plural(
                            R.plurals.completed_tasks_celebration_message,
                            completionCount,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch completion count")
                    _userMessage.value =
                        UserMessage.String(R.string.current_task_fetch_completion_error)
                }
            }
        }
    }

    fun userMessageShown() {
        _userMessage.value = null
    }
}
