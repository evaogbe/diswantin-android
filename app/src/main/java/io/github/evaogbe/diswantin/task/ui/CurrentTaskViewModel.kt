package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.CurrentTaskParams
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    private val userMessage = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        currentTaskParams.flatMapLatest { taskRepository.getCurrentTask(it) }
            .map<Task?, Result<Task?>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch current task")
                emit(Result.Failure)
            },
        userMessage,
    ) { currentTask, userMessage ->
        when (currentTask) {
            is Result.Success -> {
                if (currentTask.value == null) {
                    CurrentTaskUiState.Empty
                } else {
                    CurrentTaskUiState.Present(
                        currentTask = currentTask.value,
                        userMessage = userMessage,
                    )
                }
            }

            is Result.Failure -> CurrentTaskUiState.Failure
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CurrentTaskUiState.Pending,
    )

    fun refresh() {
        currentTaskParams.value = CurrentTaskParams(ZonedDateTime.now(clock))
    }

    fun markCurrentTaskDone() {
        val task = (uiState.value as? CurrentTaskUiState.Present)?.currentTask ?: return

        viewModelScope.launch {
            try {
                taskRepository.markDone(task.id)
                currentTaskParams.value = CurrentTaskParams(ZonedDateTime.now(clock))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark task done: %s", task)
                userMessage.value = R.string.current_task_mark_done_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
