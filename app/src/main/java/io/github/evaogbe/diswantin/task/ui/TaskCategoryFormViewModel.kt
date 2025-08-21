package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.EditTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.NewTaskCategoryForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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

    private val removedTaskIds = MutableStateFlow(emptySet<Long>())

    val existingTaskPagingData = categoryId?.let { id ->
        combine(
            taskRepository.getTasksByCategoryId(id).cachedIn(viewModelScope),
            removedTaskIds
        ) { all, removed ->
            all.filter { it.id !in removed }
        }
    } ?: flowOf(
        PagingData.empty(
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            )
        )
    )

    private val newTasks = MutableStateFlow(persistentListOf<Task>())

    private val isEditing = MutableStateFlow(true)

    private val taskQuery = MutableStateFlow("")

    private val isSaved = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    private val existingCategoryStream = categoryId?.let { id ->
        taskCategoryRepository.getById(id)
            .map<TaskCategory?, Result<TaskCategory?>> { Result.Success(it) }.catch { e ->
                Timber.e(e, "Failed to fetch task category by id: %d", id)
                emit(Result.Failure(e))
            }
    } ?: flowOf(Result.Success(null))

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        existingCategoryStream,
        newTasks,
        removedTaskIds,
        isEditing,
        taskQuery,
        taskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(query.trim()).catch { e ->
                    Timber.e(e, "Failed to search for tasks by query: %s", query)
                    userMessage.value = UserMessage.String(R.string.search_task_options_error)
                }
            }
        },
        isSaved,
        userMessage,
    ) { args ->
        val existingCategoryResult = args[0] as Result<TaskCategory?>
        val newTasks = args[1] as ImmutableList<Task>
        val removedTaskIds = args[2] as Set<Long>
        val isEditing = args[3] as Boolean
        val taskQuery = (args[4] as String).trim()
        val taskSearchResults = args[5] as List<Task>
        val isSaved = args[6] as Boolean
        val userMessage = args[7] as UserMessage?

        if (isSaved) {
            TaskCategoryFormUiState.Saved
        } else {
            existingCategoryResult.fold(
                onSuccess = { existingCategory ->
                    val hasTaskOptions = taskQuery != taskSearchResults.singleOrNull()?.name
                    TaskCategoryFormUiState.Success(
                        newTasks = newTasks,
                        isEditing = isEditing,
                        taskOptions = if (hasTaskOptions) {
                            taskSearchResults.filter<Task> { option ->
                                val hasCategory =
                                    option.categoryId != null && option.id !in removedTaskIds
                                !hasCategory && option !in newTasks
                            }.toImmutableList<Task>()
                        } else {
                            persistentListOf()
                        },
                        userMessage = userMessage,
                    )
                },
                onFailure = TaskCategoryFormUiState::Failure,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskCategoryFormUiState.Pending,
    )

    init {
        viewModelScope.launch {
            val existingCategory = existingCategoryStream.first().getOrNull() ?: return@launch
            nameInput = existingCategory.name
            isEditing.value = false
        }
    }

    fun updateNameInput(value: String) {
        nameInput = value
    }

    fun startEditTask() {
        isEditing.value = true
    }

    fun addTask(task: Task) {
        newTasks.update { it.add(task) }
        isEditing.value = false
        taskQuery.value = ""
    }

    fun removeTask(task: Task) {
        removedTaskIds.update { it + task.id }
        newTasks.update { it.remove(task) }
    }

    fun searchTasks(query: String) {
        taskQuery.value = query
    }

    fun saveCategory() {
        if (nameInput.isBlank()) return
        val state = (uiState.value as? TaskCategoryFormUiState.Success) ?: return

        if (categoryId == null) {
            val form = NewTaskCategoryForm(
                name = nameInput,
                newTasks = state.newTasks.take(20),
            )
            viewModelScope.launch {
                try {
                    taskCategoryRepository.create(form)
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task category with form: %s", form)
                    userMessage.value =
                        UserMessage.String(R.string.task_category_form_save_error_new)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingCategory = checkNotNull(existingCategoryStream.first().getOrNull())
                    taskCategoryRepository.update(
                        EditTaskCategoryForm(
                            name = nameInput,
                            newTasks = state.newTasks.take(20),
                            taskIdsToRemove = removedTaskIds.value,
                            existingCategory = existingCategory,
                        )
                    )
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task category with id: %s", categoryId)
                    userMessage.value =
                        UserMessage.String(R.string.task_category_form_save_error_edit)
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
