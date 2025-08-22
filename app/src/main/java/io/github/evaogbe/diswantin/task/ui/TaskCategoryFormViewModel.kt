package io.github.evaogbe.diswantin.task.ui

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

    private val name = MutableStateFlow(savedStateHandle[NavArguments.NAME_KEY] ?: "")

    private val removedTaskIds = MutableStateFlow(emptySet<Long>())

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

    val existingTaskPagingData = categoryId?.let { id ->
        combine(
            taskRepository.getTasksByCategoryId(id).cachedIn(viewModelScope),
            removedTaskIds,
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

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        existingCategoryStream,
        name,
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
        val name = args[1] as String
        val newTasks = args[2] as ImmutableList<Task>
        val removedTaskIds = args[3] as Set<Long>
        val isEditing = args[4] as Boolean
        val taskQuery = (args[5] as String).trim()
        val taskSearchResults = args[6] as List<Task>
        val isSaved = args[7] as Boolean
        val userMessage = args[8] as UserMessage?

        if (isSaved) {
            TaskCategoryFormUiState.Saved
        } else {
            existingCategoryResult.fold(
                onSuccess = { existingCategory ->
                    val hasTaskOptions = taskQuery != taskSearchResults.singleOrNull()?.name
                    TaskCategoryFormUiState.Success(
                        name = name,
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
                        changed = listOf(
                            name == existingCategory?.name.orEmpty(),
                            removedTaskIds.isEmpty(),
                            newTasks.isEmpty()
                        ).contains(false),
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
            name.value = existingCategory.name
            isEditing.value = false
        }
    }

    fun updateName(value: String) {
        name.value = value
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
        val state = (uiState.value as? TaskCategoryFormUiState.Success) ?: return
        if (state.name.isBlank()) return

        if (categoryId == null) {
            val form = NewTaskCategoryForm(
                name = state.name,
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
                            name = state.name,
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
