package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val taskId: Long? = savedStateHandle[Destination.EditTaskForm.ID_KEY]

    val isNew = taskId == null

    var nameInput by mutableStateOf("")
        private set

    private val deadlineInput = MutableStateFlow<ZonedDateTime?>(null)

    private val scheduledAtInput = MutableStateFlow<ZonedDateTime?>(null)

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val taskStream = taskId?.let { id ->
        taskRepository.getById(id).catch { e ->
            Timber.e(e, "Failed to fetch task by id: %d", id)
            emit(null)
        }
    } ?: flowOf(null)

    val uiState = combine(
        deadlineInput,
        scheduledAtInput,
        saveResult,
        taskStream,
    ) { deadlineInput, scheduledAtInput, saveResult, task ->
        when {
            saveResult?.isSuccess == true -> TaskFormUiState.Saved
            taskId != null && task == null -> TaskFormUiState.Failure
            else -> TaskFormUiState.Success(
                deadlineInput = deadlineInput,
                scheduledAtInput = scheduledAtInput,
                hasSaveError = saveResult?.isFailure == true,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskFormUiState.Pending,
    )

    init {
        viewModelScope.launch {
            val task = taskStream.first() ?: return@launch
            nameInput = task.name
            deadlineInput.value = task.deadline?.atZone(clock.zone)
            scheduledAtInput.value = task.scheduledAt?.atZone(clock.zone)
        }
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun updateDeadlineInput(value: ZonedDateTime?) {
        deadlineInput.value = value
    }

    fun updateScheduledAtInput(value: ZonedDateTime?) {
        scheduledAtInput.value = value
    }

    fun saveTask() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskFormUiState.Success) ?: return
        if (taskId == null) {
            val form = NewTaskForm(
                name = nameInput,
                deadline = state.deadlineInput?.toInstant(),
                scheduledAt = state.scheduledAtInput?.toInstant(),
                clock = clock,
            )
            viewModelScope.launch {
                try {
                    taskRepository.create(form)
                    saveResult.value = Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task with form: %s", form)
                    saveResult.value = Result.failure(e)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val task = checkNotNull(taskStream.first())
                    taskRepository.update(
                        EditTaskForm(
                            name = nameInput,
                            deadline = state.deadlineInput?.toInstant(),
                            scheduledAt = state.scheduledAtInput?.toInstant(),
                            task = task,
                        )
                    )
                    saveResult.value = Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task with id: %d", taskId)
                    saveResult.value = Result.failure(e)
                }
            }
        }
    }
}
