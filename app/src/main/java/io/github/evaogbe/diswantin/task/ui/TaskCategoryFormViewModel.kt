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
import io.github.evaogbe.diswantin.task.data.EditTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.NewTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
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
class TaskCategoryFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskCategoryRepository: TaskCategoryRepository,
    taskRepository: TaskRepository
) : ViewModel() {
    private val categoryId: Long? = savedStateHandle[NavArguments.ID_KEY]

    val isNew = categoryId == null

    var nameInput by mutableStateOf(savedStateHandle[NavArguments.NAME_KEY] ?: "")
        private set

    private val tasks = MutableStateFlow(persistentListOf<Task>())

    private val editingTaskIndex = MutableStateFlow<Int?>(0)

    private val taskQuery = MutableStateFlow("")

    private val saveResult = MutableStateFlow<Result<Unit>?>(null)

    private val userMessage = MutableStateFlow<Int?>(null)

    private val existingCategoryWithTasksStream = categoryId?.let { id ->
        taskCategoryRepository.getCategoryWithTasksById(id)
            .map<TaskCategoryWithTasks, Result<TaskCategoryWithTasks>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch task category by id: %d", id)
                emit(Result.Failure)
            }
    } ?: flowOf(Result.Success(null))

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        existingCategoryWithTasksStream,
        tasks,
        editingTaskIndex,
        taskQuery,
        taskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(query.trim()).catch { e ->
                    Timber.e(e, "Failed to search for tasks by query: %s", query)
                    userMessage.value = R.string.search_task_options_error
                }
            }
        },
        saveResult,
        userMessage,
    ) { args ->
        val existingCategoryWithTasks = args[0] as Result<TaskCategoryWithTasks?>
        val tasks = args[1] as ImmutableList<Task>
        val editingTaskIndex = args[2] as Int?
        val taskQuery = args[3] as String
        val taskSearchResults = args[4] as List<Task>
        val saveResult = args[5] as Result<Unit>?
        val userMessage = args[6] as Int?

        when {
            saveResult is Result.Success -> TaskCategoryFormUiState.Saved
            existingCategoryWithTasks is Result.Success -> {
                val taskOptions = taskSearchResults.filter { option ->
                    (option.categoryId == null ||
                            option.categoryId == existingCategoryWithTasks.value?.category?.id) &&
                            option !in tasks
                }
                TaskCategoryFormUiState.Success(
                    tasks = tasks,
                    editingTaskIndex = editingTaskIndex,
                    taskOptions = if (taskOptions.singleOrNull()?.name == taskQuery.trim()) {
                        persistentListOf()
                    } else {
                        taskOptions.toImmutableList()
                    },
                    hasSaveError = saveResult is Result.Failure,
                    userMessage = userMessage,
                )
            }

            else -> TaskCategoryFormUiState.Failure
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskCategoryFormUiState.Pending,
    )

    init {
        viewModelScope.launch {
            val existingCategoryWithTasks = existingCategoryWithTasksStream.first().getOrNull()
                ?: return@launch
            nameInput = existingCategoryWithTasks.category.name
            tasks.value = existingCategoryWithTasks.tasks.toPersistentList()
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

    fun saveCategory() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskCategoryFormUiState.Success) ?: return

        if (categoryId == null) {
            val form = NewTaskCategoryForm(name = nameInput, tasks = state.tasks)
            viewModelScope.launch {
                try {
                    taskCategoryRepository.create(form)
                    saveResult.value = Result.Success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task category with form: %s", form)
                    saveResult.value = Result.Failure
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingCategoryWithTasks =
                        checkNotNull(existingCategoryWithTasksStream.first().getOrNull())
                    taskCategoryRepository.update(
                        EditTaskCategoryForm(
                            name = nameInput,
                            tasks = state.tasks,
                            existingCategoryWithTasks = existingCategoryWithTasks,
                        )
                    )
                    saveResult.value = Result.Success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task category with id: %s", categoryId)
                    saveResult.value = Result.Failure
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
