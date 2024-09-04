package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.EditTaskListForm
import io.github.evaogbe.diswantin.task.data.NewTaskListForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskListRepository
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskListFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskListRepository: TaskListRepository,
    taskRepository: TaskRepository
) : ViewModel() {
    private val taskListId: Long? = savedStateHandle[NavArguments.ID_KEY]

    val isNew = taskListId == null

    var nameInput by mutableStateOf(savedStateHandle[NavArguments.NAME_KEY] ?: "")
        private set

    private val tasks = MutableStateFlow(persistentListOf<Task>())

    private val editingTaskIndex = MutableStateFlow<Int?>(0)

    private val taskQuery = MutableStateFlow("")

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val userMessage = MutableStateFlow<Int?>(null)

    private val existingTaskListWithTasksStream = taskListId?.let { id ->
        taskListRepository.getTaskListWithTasksById(id)
            .map { Result.success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch task list by id: %d", id)
                emit(Result.failure(e))
            }
    } ?: flowOf(Result.success(null))

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        existingTaskListWithTasksStream,
        tasks,
        editingTaskIndex,
        taskQuery,
        taskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(query.trim()).catch { e ->
                    Timber.e(e, "Failed to search for tasks by query: %s", query)
                    emit(emptyList())
                    userMessage.value = R.string.task_list_form_search_tasks_error
                }
            }
        },
        saveResult,
        userMessage,
    ) { args ->
        val existingTaskListWithTasks = args[0] as Result<TaskListWithTasks?>
        val tasks = args[1] as ImmutableList<Task>
        val editingTaskIndex = args[2] as Int?
        val taskQuery = args[3] as String
        val taskSearchResults = args[4] as List<Task>
        val saveResult = args[5] as Result<Unit>?
        val userMessage = args[6] as Int?

        if (saveResult?.isSuccess == true) {
            TaskListFormUiState.Saved
        } else {
            existingTaskListWithTasks.fold(
                onSuccess = { taskListWithTasks ->
                    val editingTask = editingTaskIndex?.let(tasks::getOrNull)
                    val taskOptions = taskSearchResults.filter { option ->
                        (option.listId == null ||
                                option.listId == taskListWithTasks?.taskList?.id) &&
                                option !in tasks
                    }
                    TaskListFormUiState.Success(
                        tasks = tasks,
                        editingTaskIndex = editingTaskIndex,
                        taskOptions = if (
                            taskOptions == listOf(editingTask) && taskQuery == editingTask?.name
                        ) {
                            persistentListOf()
                        } else {
                            taskOptions.toImmutableList()
                        },
                        hasSaveError = saveResult?.isFailure == true,
                        userMessage = userMessage,
                    )
                },
                onFailure = { TaskListFormUiState.Failure },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskListFormUiState.Pending,
    )

    init {
        viewModelScope.launch {
            val existingTaskListWithTasks = existingTaskListWithTasksStream.first().getOrNull()
                ?: return@launch
            nameInput = existingTaskListWithTasks.taskList.name
            tasks.value = existingTaskListWithTasks.tasks.toPersistentList()
            editingTaskIndex.value = null
        }
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun startEditTask(index: Int) {
        editingTaskIndex.value = index
    }

    fun stopEditTask() {
        editingTaskIndex.value = null
        taskQuery.value = ""
    }

    fun setTask(index: Int, task: Task) {
        tasks.update { tasks ->
            if (index < tasks.size) {
                tasks.set(index, task)
            } else {
                tasks.add(task)
            }
        }
        editingTaskIndex.value = null
        taskQuery.value = ""
    }

    fun removeTask(task: Task) {
        tasks.update { it.remove(task) }
    }

    fun searchTasks(query: String) {
        taskQuery.value = query
    }

    fun saveTaskList() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskListFormUiState.Success) ?: return
        if (taskListId == null) {
            val form = NewTaskListForm(name = nameInput, tasks = state.tasks)
            viewModelScope.launch {
                try {
                    taskListRepository.create(form)
                    saveResult.value = Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task list with params: %s", form)
                    saveResult.value = Result.failure(e)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingTaskListWithTasks =
                        checkNotNull(existingTaskListWithTasksStream.first().getOrNull())
                    taskListRepository.update(
                        EditTaskListForm(
                            name = nameInput,
                            tasks = state.tasks,
                            existingTaskListWithTasks = existingTaskListWithTasks,
                        )
                    )
                    saveResult.value = Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task list with id: %s", taskListId)
                    saveResult.value = Result.failure(e)
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
