package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
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

    private val deadlineDate = MutableStateFlow<LocalDate?>(null)

    private val deadlineTime = MutableStateFlow<LocalTime?>(null)

    private val scheduledDate = MutableStateFlow<LocalDate?>(null)

    private val scheduledTime = MutableStateFlow<LocalTime?>(null)

    private val recurring = MutableStateFlow(false)

    private val parentTask = MutableStateFlow<Task?>(null)

    private val parentTaskQuery = MutableStateFlow("")

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val userMessage = MutableStateFlow<Int?>(null)

    private val existingTaskStream = taskId?.let { id ->
        taskRepository.getById(id)
            .map<Task, Result<Task>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch task by id: %d", id)
                emit(Result.Failure)
            }
    } ?: flowOf(Result.Success(null))

    private val existingParentTaskStream = taskId?.let { id ->
        taskRepository.getParent(id)
            .map<Task?, Result<Task?>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch parent task by child id: %d", id)
                emit(Result.Failure)
            }
    } ?: flowOf(Result.Success(null))

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    val uiState = combine(
        deadlineDate,
        deadlineTime,
        scheduledDate,
        scheduledTime,
        recurring,
        taskRepository.getCount().catch { e ->
            Timber.e(e, "Failed to fetch task count")
            emit(0L)
        },
        parentTask,
        parentTaskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(query.trim()).catch { e ->
                    Timber.e(e, "Failed to search for task by query: %s", query)
                    userMessage.value = R.string.search_task_options_error
                }
            }
        },
        saveResult,
        userMessage,
        existingTaskStream,
        existingParentTaskStream,
    ) { args ->
        val deadlineDate = args[0] as LocalDate?
        val deadlineTime = args[1] as LocalTime?
        val scheduledDate = args[2] as LocalDate?
        val scheduledTime = args[3] as LocalTime?
        val recurring = args[4] as Boolean
        val taskCount = args[5] as Long
        val parentTask = args[6] as Task?
        val parentTaskOptions = args[7] as List<Task>
        val saveResult = args[8] as Result<Unit>?
        val userMessage = args[9] as Int?
        val existingTask = args[10] as Result<Task?>
        val existingParentTask = args[11] as Result<Task?>

        when {
            saveResult is Result.Success -> TaskFormUiState.Saved
            existingTask is Result.Success && existingParentTask is Result.Success -> {
                TaskFormUiState.Success(
                    deadlineDate = deadlineDate,
                    deadlineTime = deadlineTime,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    recurring = recurring,
                    showParentTaskField = taskCount > if (taskId == null) 0L else 1L,
                    parentTask = parentTask,
                    parentTaskOptions = parentTaskOptions.filterNot { it == existingTask.value }
                        .toPersistentList(),
                    hasSaveError = saveResult is Result.Failure,
                    userMessage = userMessage,
                    clock = clock,
                )
            }

            else -> TaskFormUiState.Failure
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
            deadlineDate.value = existingTask.deadlineDate
            deadlineTime.value = existingTask.deadlineTime
            scheduledDate.value = existingTask.scheduledDate
            scheduledTime.value = existingTask.scheduledTime
            recurring.value = existingTask.recurring
            parentTask.value = existingParentTaskStream.first().getOrNull()
        }
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun updateDeadlineDate(value: LocalDate?) {
        deadlineDate.value = value
    }

    fun updateDeadlineTime(value: LocalTime?) {
        deadlineTime.value = value
    }

    fun updateScheduledDate(value: LocalDate?) {
        scheduledDate.value = value
        if (scheduledTime.value == null) {
            scheduledTime.value = LocalTime.of(9, 0)
        }
    }

    fun updateScheduledTime(value: LocalTime?) {
        scheduledTime.value = value
        if (value == null) {
            scheduledDate.value = null
        }
    }

    fun updateRecurring(value: Boolean) {
        recurring.value = value
    }

    fun updateParentTask(value: Task?) {
        parentTask.value = value
    }

    fun searchTasks(query: String) {
        parentTaskQuery.value = query
    }

    fun saveTask() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskFormUiState.Success) ?: return
        val deadlineDate = if (
            state.deadlineDate == null &&
            state.deadlineTime != null &&
            !state.recurring
        ) {
            LocalDate.now()
        } else {
            state.deadlineDate
        }
        val scheduledDate = if (
            state.scheduledDate == null &&
            state.scheduledTime != null &&
            !state.recurring
        ) {
            LocalDate.now()
        } else {
            state.scheduledDate
        }
        if (taskId == null) {
            val form = NewTaskForm(
                name = nameInput,
                deadlineDate = deadlineDate,
                deadlineTime = state.deadlineTime,
                scheduledDate = scheduledDate,
                scheduledTime = state.scheduledTime,
                recurring = state.recurring,
                parentTaskId = state.parentTask?.id,
                clock = clock,
            )
            viewModelScope.launch {
                try {
                    taskRepository.create(form)
                    saveResult.value = Result.Success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task with form: %s", form)
                    saveResult.value = Result.Failure
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingTask = checkNotNull(existingTaskStream.first().getOrNull())
                    val existingParent = existingParentTaskStream.first().getOrNull()
                    taskRepository.update(
                        EditTaskForm(
                            name = nameInput,
                            deadlineDate = deadlineDate,
                            deadlineTime = state.deadlineTime,
                            scheduledDate = scheduledDate,
                            scheduledTime = state.scheduledTime,
                            recurring = state.recurring,
                            parentUpdateType = when (state.parentTask) {
                                existingParent -> PathUpdateType.Keep
                                null -> PathUpdateType.Remove
                                else -> PathUpdateType.Replace(state.parentTask.id)
                            },
                            existingTask = existingTask,
                        )
                    )
                    saveResult.value = Result.Success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task with id: %d", taskId)
                    saveResult.value = Result.Failure
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
