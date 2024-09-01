package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.Destination
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
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val taskId: Long = checkNotNull(savedStateHandle[Destination.TaskDetail.ID_KEY])

    private val initialized = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<Int?>(null)

    val uiState =
        combine(
            initialized,
            taskRepository.getTaskDetailById(taskId),
            userMessage,
        ) { initialized, task, userMessage ->
            when {
                task != null -> {
                    TaskDetailUiState.Success(task = task, userMessage = userMessage, clock = clock)
                }

                initialized -> TaskDetailUiState.Deleted
                else -> TaskDetailUiState.Failure
            }
        }.onEach {
            if (it is TaskDetailUiState.Success) {
                initialized.value = true
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch task by id: %d", taskId)
            emit(TaskDetailUiState.Failure)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TaskDetailUiState.Pending
        )

    fun deleteTask() {
        val task = (uiState.value as? TaskDetailUiState.Success)?.task ?: return

        viewModelScope.launch {
            try {
                taskRepository.delete(task.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete task: %s", task)
                userMessage.value = R.string.task_detail_delete_error
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
