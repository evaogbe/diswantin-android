package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.data.ClockMonitor
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.CurrentTask
import io.github.evaogbe.diswantin.task.data.CurrentTaskParams
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSkip
import io.github.evaogbe.diswantin.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class CurrentTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    clockMonitor: ClockMonitor,
) : BaseViewModel(clockMonitor) {
    private val refreshCount = MutableStateFlow(0)

    private val currentTaskParams = combine(clockMonitor.clock, refreshCount) { clock, _ ->
        CurrentTaskParams(ZonedDateTime.now(clock))
    }

    private val isRefreshing = MutableStateFlow(false)

    private val _userMessage = MutableStateFlow<CurrentTaskUserMessage?>(null)

    val userMessage = _userMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        currentTaskParams.flatMapLatest { params ->
            isRefreshing.value = true
            taskRepository.getCurrentTask(params)
                .map<CurrentTask?, Result<CurrentTask?>> { Result.Success(it) }.catch { e ->
                    Timber.e(e, "Failed to fetch current task")
                    emit(Result.Failure(e))
                }.onEach { isRefreshing.value = false }
        },
        isRefreshing,
    ) { taskResult, isRefreshing ->
        taskResult.fold(
            onSuccess = { task ->
                if (task == null) {
                    CurrentTaskUiState.Empty(isRefreshing = isRefreshing)
                } else {
                    CurrentTaskUiState.Present(
                        id = task.id,
                        name = task.name,
                        note = task.note,
                        isRefreshing = isRefreshing,
                        canSkip = task.recurring,
                    )
                }
            },
            onFailure = CurrentTaskUiState::Failure,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = CurrentTaskUiState.Pending,
    )

    fun refresh() {
        ++refreshCount.value
    }

    fun skipCurrentTask() {
        val taskId = (uiState.value as? CurrentTaskUiState.Present)?.id ?: return

        viewModelScope.launch {
            var skipped = false

            try {
                taskRepository.skip(TaskSkip(taskId = taskId, skippedAt = now().toInstant()))
                skipped = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to skip task: %s", taskId)
                _userMessage.value = CurrentTaskUserMessage.SkipError
            }

            if (skipped) {
                refresh()
            }
        }
    }

    fun markCurrentTaskDone() {
        val taskId = (uiState.value as? CurrentTaskUiState.Present)?.id ?: return

        viewModelScope.launch {
            var markedDone = false

            try {
                taskRepository.markDone(TaskCompletion(taskId = taskId, doneAt = now().toInstant()))
                markedDone = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark task done: %s", taskId)
                _userMessage.value = CurrentTaskUserMessage.MarkDoneError
            }

            if (markedDone) {
                refresh()
            }
        }
    }

    fun userMessageShown() {
        _userMessage.value = null
    }
}
