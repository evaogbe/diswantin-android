package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.CurrentTaskParams
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class CurrentTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val currentTaskParams = MutableStateFlow(CurrentTaskParams(ZonedDateTime.now(clock)))

    private val isRefreshing = MutableStateFlow(false)

    private val _userMessage = MutableStateFlow<CurrentTaskUserMessage?>(null)

    val userMessage = _userMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = currentTaskParams.flatMapLatest { params ->
        isRefreshing.value = true
        taskRepository.getCurrentTask(params).onEach { isRefreshing.value = false }
            .map<Task?, Result<Task?>> { Result.Success(it) }.catch { e ->
                Timber.e(e, "Failed to fetch current task")
                emit(Result.Failure(e))
            }
    }.flatMapLatest { taskResult ->
        taskResult.fold(
            onSuccess = { task ->
                val canSkip = task?.let { t ->
                    taskRepository.getTaskRecurrencesByTaskId(t.id).map { it.isNotEmpty() }
                        .catch { e ->
                            Timber.e(e, "Failed to fetch current task recurrences")
                            _userMessage.value = CurrentTaskUserMessage.FetchRecurrencesError
                            emit(false)
                        }
                } ?: flowOf(false)
                combine(isRefreshing, canSkip) { isRefreshing, canSkip ->
                    if (task == null) {
                        CurrentTaskUiState.Empty(isRefreshing = isRefreshing)
                    } else {
                        CurrentTaskUiState.Present(
                            currentTask = task,
                            isRefreshing = isRefreshing,
                            canSkip = canSkip,
                        )
                    }
                }
            },
            onFailure = { flowOf(CurrentTaskUiState.Failure(it)) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
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
                _userMessage.value = CurrentTaskUserMessage.SkipError
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
                _userMessage.value = CurrentTaskUserMessage.MarkDoneError
            }

            if (markedDone) {
                currentTaskParams.value = CurrentTaskParams(ZonedDateTime.now(clock))
            }
        }
    }

    fun userMessageShown() {
        _userMessage.value = null
    }
}
