package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class CurrentTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val now = MutableStateFlow(ZonedDateTime.now(clock))

    private val userMessage = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        now.flatMapLatest {
            taskRepository.getCurrentTask(
                scheduledBefore = it.plusHours(1).toInstant(),
                doneBefore = it.truncatedTo(ChronoUnit.DAYS).toInstant(),
            )
        },
        userMessage
    ) { task, userMessage ->
        if (task == null) {
            CurrentTaskUiState.Empty
        } else {
            CurrentTaskUiState.Present(currentTask = task, userMessage = userMessage)
        }
    }.catch { e ->
        Timber.e(e, "Failed to fetch current task")
        emit(CurrentTaskUiState.Failure)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CurrentTaskUiState.Pending
    )

    fun initialize() {
        now.value = ZonedDateTime.now(clock)
    }

    fun markCurrentTaskDone() {
        val task = (uiState.value as? CurrentTaskUiState.Present)?.currentTask ?: return

        viewModelScope.launch {
            try {
                taskRepository.markDone(task.id)
                now.value = ZonedDateTime.now(clock)
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
