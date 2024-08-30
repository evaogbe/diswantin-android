package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.task.data.NewTaskListForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskListRepository
import io.github.evaogbe.diswantin.task.data.TaskRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TaskListFormViewModel @Inject constructor(
    private val taskListRepository: TaskListRepository,
    taskRepository: TaskRepository
) : ViewModel() {
    var nameInput by mutableStateOf("")
        private set

    private val tasks = MutableStateFlow(persistentListOf<Task>())

    private val editingTaskIndex = MutableStateFlow<Int?>(0)

    private val taskQuery = MutableStateFlow("")

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        tasks,
        editingTaskIndex,
        taskQuery,
        taskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(query = query, singletonsOnly = true).catch { e ->
                    Timber.e(e, "Failed to search for tasks by query: %s", query)
                    emit(emptyList())
                }
            }
        },
        saveResult,
    ) { tasks, editingTaskIndex, taskQuery, taskSearchResults, saveResult ->
        if (saveResult?.isSuccess == true) {
            TaskListFormUiState.Saved
        } else {
            val editingTask = editingTaskIndex?.let(tasks::getOrNull)
            val taskOptions = taskSearchResults.filter { option ->
                option !in tasks || editingTask == option
            }
            TaskListFormUiState.Editing(
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
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskListFormUiState.Editing(
            tasks = persistentListOf(),
            editingTaskIndex = 0,
            taskOptions = persistentListOf(),
            hasSaveError = false,
        )
    )

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
        val state = (uiState.value as? TaskListFormUiState.Editing) ?: return
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
    }
}
