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
import io.github.evaogbe.diswantin.ui.navigation.Destination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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

    private val dueAtInput = MutableStateFlow<ZonedDateTime?>(null)

    private val scheduledAtInput = MutableStateFlow<ZonedDateTime?>(null)

    private val prevTask = MutableStateFlow<Task?>(null)

    private val prevTaskQuery = MutableStateFlow("")

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val taskStream = taskId?.let { id ->
        taskRepository.getById(id).catch { e ->
            Timber.e(e, "Failed to fetch task by id: %d", id)
            emit(null)
        }
    } ?: flowOf(null)

    val uiState = initUiState()

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun initUiState(): StateFlow<TaskFormUiState> {
        viewModelScope.launch {
            val task = taskStream.first() ?: return@launch
            nameInput = task.name
            dueAtInput.value = task.dueAt?.atZone(clock.zone)
            scheduledAtInput.value = task.scheduledAt?.atZone(clock.zone)
            prevTask.value = taskRepository.getParent(task.id).first()
        }
        val prevTaskOptionsStream = prevTaskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(
                    query = query.trim(),
                    tailsOnly = true,
                    excludeChainFor = taskId
                ).catch { e ->
                    Timber.e(
                        e,
                        "Failed to search for tasks by query: %s, id: %d",
                        query,
                        taskId
                    )
                    emit(emptyList())
                }
            }
        }
        val hasTasksOutsideChainStream =
            taskRepository.hasTasks(excludeChainFor = taskId).catch { e ->
                Timber.e(e, "Failed to fetch tasks outside chain of: %d", taskId)
                emit(false)
            }
        return combine(
            dueAtInput,
            scheduledAtInput,
            prevTask,
            saveResult,
            taskStream,
            prevTaskOptionsStream,
            hasTasksOutsideChainStream,
        ) { args ->
            val dueAtInput = args[0] as ZonedDateTime?
            val scheduledAtInput = args[1] as ZonedDateTime?
            val prevTask = args[2] as Task?
            val saveResult = args[3] as Result<Unit>?
            val task = args[4]
            val prevTaskOptions = args[5] as List<Task>
            val hasTasksOutsideChain = args[6] as Boolean
            when {
                saveResult?.isSuccess == true -> TaskFormUiState.Saved
                taskId != null && task == null -> TaskFormUiState.Failure
                else -> TaskFormUiState.Success(
                    dueAtInput = dueAtInput,
                    scheduledAtInput = scheduledAtInput,
                    canUpdatePrevTask = hasTasksOutsideChain || prevTask != null,
                    prevTask = prevTask,
                    prevTaskOptions = prevTaskOptions,
                    hasSaveError = saveResult?.isFailure == true,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = TaskFormUiState.Pending,
        )
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun updateDueAtInput(value: ZonedDateTime?) {
        dueAtInput.value = value
    }

    fun updateScheduledAtInput(value: ZonedDateTime?) {
        scheduledAtInput.value = value
    }

    fun updatePrevTask(value: Task?) {
        prevTask.value = value
    }

    fun searchPrevTask(query: String) {
        prevTaskQuery.value = query
    }

    fun saveTask() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskFormUiState.Success) ?: return
        if (taskId == null) {
            val form = NewTaskForm(
                name = nameInput,
                dueAt = state.dueAtInput?.toInstant(),
                scheduledAt = state.scheduledAtInput?.toInstant(),
                prevTaskId = state.prevTask?.id,
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
                    val existingParent = taskRepository.getParent(taskId).first()
                    taskRepository.update(
                        EditTaskForm(
                            name = nameInput,
                            dueAt = state.dueAtInput?.toInstant(),
                            scheduledAt = state.scheduledAtInput?.toInstant(),
                            oldParentId = existingParent?.id,
                            parentId = state.prevTask?.id,
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
