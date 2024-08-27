package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CurrentTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    private val userMessage = MutableStateFlow<Int?>(null)

    val uiState =
        combine(taskRepository.currentTaskStream, userMessage) { task, userMessage ->
            if (task == null) {
                CurrentTaskUiState.Empty
            } else {
                CurrentTaskUiState.Present(
                    currentTask = task,
                    userMessage = userMessage
                )
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch current task")
            emit(CurrentTaskUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = CurrentTaskUiState.Pending
        )

    fun removeCurrentTask() {
        val task = (uiState.value as? CurrentTaskUiState.Present)?.currentTask ?: return

        viewModelScope.launch {
            try {
                taskRepository.remove(task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove task: %s", task)
                userMessage.value = R.string.current_task_remove_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
