package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.EditTagForm
import io.github.evaogbe.diswantin.task.data.NewTagForm
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.TaggedTask
import io.github.evaogbe.diswantin.task.data.TaskRepository
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
class TagFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tagRepository: TagRepository,
    taskRepository: TaskRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<TagFormRoute>()

    private val tagId = route.id

    val isNew = tagId == null

    private val name = MutableStateFlow(route.name.orEmpty())

    private val removedTaskIds = MutableStateFlow(emptySet<Long>())

    private val newTasks = MutableStateFlow(persistentListOf<TaggedTask>())

    private val isEditing = MutableStateFlow(true)

    private val taskQuery = MutableStateFlow("")

    private val isSaved = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    private val existingTagStream = tagId?.let { id ->
        tagRepository.getById(id).map<Tag?, Result<Tag?>> { tag ->
            if (tag == null) {
                Timber.e("Failed to fetch tag by id: %d", id)
                Result.Failure(NullPointerException("Tag not found: $id"))
            } else {
                Result.Success(tag)
            }
        }.catch { e ->
            Timber.e(e, "Failed to fetch tag by id: %d", id)
            emit(Result.Failure(e))
        }
    } ?: flowOf(Result.Success(null))

    val existingTaskPagingData = tagId?.let { id ->
        combine(
            taskRepository.getTaggedTasksByTagId(id).cachedIn(viewModelScope),
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
        existingTagStream,
        name,
        newTasks,
        removedTaskIds,
        isEditing,
        taskQuery,
        taskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.searchTaggedTasks(query.trim(), tagId, size = 40).catch { e ->
                    Timber.e(e, "Failed to search for tasks by query: %s", query)
                    userMessage.value = UserMessage.String(R.string.search_task_options_error)
                }
            }
        },
        isSaved,
        userMessage,
    ) { args ->
        val existingTagResult = args[0] as Result<Tag?>
        val name = args[1] as String
        val newTasks = args[2] as ImmutableList<TaggedTask>
        val removedTaskIds = args[3] as Set<Long>
        val isEditing = args[4] as Boolean
        val taskQuery = (args[5] as String).trim()
        val taskSearchResults = args[6] as List<TaggedTask>
        val isSaved = args[7] as Boolean
        val userMessage = args[8] as UserMessage?

        if (isSaved) {
            TagFormUiState.Saved
        } else {
            existingTagResult.fold(
                onSuccess = { existingTag ->
                    val hasTaskOptions = taskQuery != taskSearchResults.singleOrNull()?.name
                    TagFormUiState.Success(
                        name = name,
                        newTasks = newTasks,
                        isEditing = isEditing,
                        taskOptions = if (hasTaskOptions) {
                            taskSearchResults.filter { option ->
                                val hasTag = option.isTagged && option.id !in removedTaskIds
                                !hasTag && option !in newTasks
                            }.take(20).toImmutableList()
                        } else {
                            persistentListOf()
                        },
                        changed = listOf(
                            name == existingTag?.name.orEmpty(),
                            removedTaskIds.isEmpty(),
                            newTasks.isEmpty()
                        ).contains(false),
                        userMessage = userMessage,
                    )
                },
                onFailure = TagFormUiState::Failure,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TagFormUiState.Pending,
    )

    init {
        viewModelScope.launch {
            val existingTag = existingTagStream.first().getOrNull() ?: return@launch
            name.value = existingTag.name
            isEditing.value = false
        }
    }

    fun updateName(value: String) {
        name.value = value
    }

    fun startEditTask() {
        isEditing.value = true
    }

    fun addTask(task: TaggedTask) {
        newTasks.update { it.add(task) }
        isEditing.value = false
        taskQuery.value = ""
    }

    fun removeTask(task: TaggedTask) {
        removedTaskIds.update { it + task.id }
        newTasks.update { it.remove(task) }
    }

    fun searchTasks(query: String) {
        taskQuery.value = query
    }

    fun saveTag() {
        val state = (uiState.value as? TagFormUiState.Success) ?: return
        if (state.name.isBlank()) return

        val newTaskIds = state.newTasks.take(20).map { it.id }.toSet()

        if (tagId == null) {
            val form = NewTagForm(
                name = state.name,
                newTaskIds = newTaskIds,
            )
            viewModelScope.launch {
                try {
                    tagRepository.create(form)
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create tag with form: %s", form)
                    userMessage.value = UserMessage.String(R.string.tag_form_save_error_new)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingTag = checkNotNull(existingTagStream.first().getOrNull())
                    tagRepository.update(
                        EditTagForm(
                            name = state.name,
                            taskIdsToInsert = newTaskIds,
                            taskIdsToRemove = removedTaskIds.value,
                            existingTag = existingTag,
                        )
                    )
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update tag with id: %s", tagId)
                    userMessage.value = UserMessage.String(R.string.tag_form_save_error_edit)
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
