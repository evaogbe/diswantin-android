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
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val clock: Clock,
) : ViewModel() {
    private val taskId: Long? = savedStateHandle[NavArguments.ID_KEY]

    val isNew = taskId == null

    var nameInput by mutableStateOf(savedStateHandle[NavArguments.NAME_KEY] ?: "")
        private set

    private val deadlineDateInput = MutableStateFlow<LocalDate?>(null)

    private val deadlineTimeInput = MutableStateFlow<LocalTime?>(null)

    private val scheduledAtInput = MutableStateFlow<ZonedDateTime?>(null)

    private val recurringInput = MutableStateFlow(false)

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val existingTaskStream = taskId?.let { id ->
        taskRepository.getById(id)
            .map { Result.success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch task by id: %d", id)
                emit(Result.failure(e))
            }
    } ?: flowOf(Result.success(null))

    @Suppress("UNCHECKED_CAST")
    val uiState = combine(
        deadlineDateInput,
        deadlineTimeInput,
        scheduledAtInput,
        recurringInput,
        saveResult,
        existingTaskStream,
    ) { args ->
        val deadlineDateInput = args[0] as LocalDate?
        val deadlineTimeInput = args[1] as LocalTime?
        val scheduledAtInput = args[2] as ZonedDateTime?
        val recurringInput = args[3] as Boolean
        val saveResult = args[4] as Result<Unit>?
        val existingTask = args[5] as Result<Task?>
        when {
            saveResult?.isSuccess == true -> TaskFormUiState.Saved
            existingTask.isFailure -> TaskFormUiState.Failure
            else -> TaskFormUiState.Success(
                deadlineDateInput = deadlineDateInput,
                deadlineTimeInput = deadlineTimeInput,
                scheduledAtInput = scheduledAtInput,
                recurringInput = recurringInput,
                hasSaveError = saveResult?.isFailure == true,
                clock = clock,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskFormUiState.Pending,
    )

    init {
        viewModelScope.launch {
            val existingTask = existingTaskStream.first().getOrNull() ?: return@launch
            nameInput = existingTask.name
            deadlineDateInput.value = existingTask.deadlineDate
            deadlineTimeInput.value = existingTask.deadlineTime
            scheduledAtInput.value = existingTask.scheduledAt?.atZone(clock.zone)
            recurringInput.value = existingTask.recurring
        }
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun updateDeadlineDateInput(value: LocalDate?) {
        deadlineDateInput.value = value
    }

    fun updateDeadlineTimeInput(value: LocalTime?) {
        deadlineTimeInput.value = value
    }

    fun updateScheduledAtInput(value: ZonedDateTime?) {
        scheduledAtInput.value = value
    }

    fun updateRecurringInput(value: Boolean) {
        recurringInput.value = value
    }

    fun saveTask() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskFormUiState.Success) ?: return
        val deadlineDate = if (
            state.deadlineDateInput == null &&
            state.deadlineTimeInput != null &&
            !state.recurringInput
        ) {
            LocalDate.now()
        } else {
            state.deadlineDateInput
        }
        if (taskId == null) {
            val form = NewTaskForm(
                name = nameInput,
                deadlineDate = deadlineDate,
                deadlineTime = state.deadlineTimeInput,
                scheduledAt = state.scheduledAtInput?.toInstant(),
                recurring = state.recurringInput,
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
                    val existingTask = checkNotNull(existingTaskStream.first().getOrNull())
                    taskRepository.update(
                        EditTaskForm(
                            name = nameInput,
                            deadlineDate = deadlineDate,
                            deadlineTime = state.deadlineTimeInput,
                            scheduledAt = state.scheduledAtInput?.toInstant(),
                            recurring = state.recurringInput,
                            existingTask = existingTask,
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
