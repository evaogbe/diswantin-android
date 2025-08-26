package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.data.getOrDefault
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.NewTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    tagRepository: TagRepository,
    private val clock: Clock,
    val locale: Locale,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<TaskFormRoute.Main>()

    private val taskId = route.id

    val isNew = taskId == null

    private val initialName = MutableStateFlow(route.name.orEmpty())

    private val initialNote = MutableStateFlow("")

    private val name = MutableStateFlow("")

    private val note = MutableStateFlow("")

    private val recurrenceUiState = MutableStateFlow<TaskRecurrenceUiState?>(null)

    private val deadlineDate = MutableStateFlow<LocalDate?>(null)

    private val deadlineTime = MutableStateFlow<LocalTime?>(null)

    private val startAfterDate = MutableStateFlow<LocalDate?>(null)

    private val startAfterTime = MutableStateFlow<LocalTime?>(null)

    private val scheduledDate = MutableStateFlow<LocalDate?>(null)

    private val scheduledTime = MutableStateFlow<LocalTime?>(null)

    private val parentTask = MutableStateFlow<Task?>(null)

    private val parentTaskQuery = MutableStateFlow("")

    private val tagFieldState = MutableStateFlow(TagFieldState.Closed)

    private val tags = MutableStateFlow(persistentListOf<Tag>())

    private val tagQuery = MutableStateFlow("")

    private val isSaved = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<UserMessage?>(null)

    private val taskCountStream =
        taskRepository.getCount().map<Long, Result<Long>> { Result.Success(it) }.catch { e ->
            Timber.e(e, "Failed to fetch task count")
            userMessage.value = UserMessage.String(R.string.task_form_fetch_parent_task_error)
            emit(Result.Failure(e))
        }

    private val existingTaskStream = taskId?.let { id ->
        taskRepository.getById(id).map<Task, Result<Task>> { Result.Success(it) }.catch { e ->
            Timber.e(e, "Failed to fetch task by id: %d", id)
            emit(Result.Failure(e))
        }
    } ?: flowOf(Result.Success(null))

    private val existingRecurrencesStream = taskId?.let { id ->
        taskRepository.getTaskRecurrencesByTaskId(id)
            .map<List<TaskRecurrence>, Result<List<TaskRecurrence>>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch task recurrences by task id: %d", id)
                emit(Result.Failure(e))
            }
    } ?: flowOf(Result.Success(emptyList()))

    private val existingParentTaskStream = taskId?.let { id ->
        taskRepository.getParent(id).map<Task?, Result<Task?>> { Result.Success(it) }.catch { e ->
            Timber.e(e, "Failed to fetch parent task by child id: %d", id)
            userMessage.value = UserMessage.String(R.string.task_form_fetch_parent_task_error)
            emit(Result.Failure(e))
        }
    } ?: flowOf(Result.Success(null))

    private val existingTagsStream = taskId?.let { id ->
        tagRepository.getTagsByTaskId(id).map<List<Tag>, Result<List<Tag>>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch tags by task id: %d", id)
                userMessage.value = UserMessage.String(R.string.task_form_fetch_tags_error)
                tagFieldState.value = TagFieldState.Hidden
                emit(Result.Failure(e))
            }
    } ?: flowOf(Result.Success(emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    val uiState = combine(
        initialName,
        initialNote,
        name,
        note,
        recurrenceUiState,
        deadlineDate,
        deadlineTime,
        startAfterDate,
        startAfterTime,
        scheduledDate,
        scheduledTime,
        taskCountStream,
        parentTask,
        parentTaskQuery,
        parentTaskQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                taskRepository.search(query.trim(), size = 20).catch { e ->
                    Timber.e(e, "Failed to search for task by query: %s", query)
                    userMessage.value = UserMessage.String(R.string.search_task_options_error)
                }
            }
        },
        tagFieldState,
        tags,
        tagQuery,
        tagQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                tagRepository.search(query.trim(), size = 40).catch { e ->
                    Timber.e(e, "Failed to search for tag by query: %s", query)
                    userMessage.value = UserMessage.String(R.string.search_tag_options_error)
                }
            }
        },
        isSaved,
        userMessage,
        existingTaskStream,
        existingRecurrencesStream,
        existingTagsStream,
        existingParentTaskStream,
    ) { args ->
        val initialName = args[0] as String
        val initialNote = args[1] as String
        val name = args[2] as String
        val note = args[3] as String
        val recurrenceUiState = args[4] as TaskRecurrenceUiState?
        val deadlineDate = args[5] as LocalDate?
        val deadlineTime = args[6] as LocalTime?
        val startAfterDate = args[7] as LocalDate?
        val startAfterTime = args[8] as LocalTime?
        val scheduledDate = args[9] as LocalDate?
        val scheduledTime = args[10] as LocalTime?
        val taskCountResult = args[11] as Result<Long>
        val parentTask = args[12] as Task?
        val parentTaskQuery = (args[13] as String).trim()
        val parentTaskOptions = args[14] as List<Task>
        val tagFieldState = args[15] as TagFieldState
        val tags = args[16] as ImmutableList<Tag>
        val tagQuery = (args[17] as String).trim()
        val tagOptions = args[18] as List<Tag>
        val isSaved = args[19] as Boolean
        val userMessage = args[20] as UserMessage?
        val existingTaskResult = args[21] as Result<Task?>
        val existingRecurrencesResult = args[22] as Result<List<TaskRecurrence>>
        val existingTagsResult = args[23] as Result<List<Tag>>
        val existingParentTaskResult = args[24] as Result<Task?>

        if (isSaved) {
            TaskFormUiState.Saved
        } else {
            existingTaskResult.zip(existingRecurrencesResult).fold(
                onSuccess = { (existingTask, existingRecurrences) ->
                    val singleTagOption = tagOptions.singleOrNull()?.name
                    val hasTagOptions =
                        tags.none { it.name == tagQuery } || tagQuery != singleTagOption
                    val showParentTaskField =
                        existingParentTaskResult.isSuccess && taskCountResult.getOrDefault(
                            0L
                        ) > if (taskId == null) 0L else 1L
                    val singleParentTaskOption = parentTaskOptions.singleOrNull()?.name
                    val hasParentTaskOptions =
                        parentTaskQuery != parentTask?.name || parentTaskQuery != singleParentTaskOption
                    val existingTags =
                        existingTagsResult.getOrNull()?.toPersistentList() ?: persistentListOf()
                    val existingRecurrenceUiState =
                        TaskRecurrenceUiState.tryFromEntities(existingRecurrences, locale)
                    TaskFormUiState.Success(
                        initialName = initialName,
                        initialNote = initialNote,
                        recurrence = recurrenceUiState,
                        deadlineDate = deadlineDate,
                        deadlineTime = deadlineTime,
                        startAfterDate = startAfterDate,
                        startAfterTime = startAfterTime,
                        scheduledDate = scheduledDate,
                        scheduledTime = scheduledTime,
                        tagFieldState = tagFieldState,
                        tags = tags,
                        tagOptions = if (hasTagOptions) {
                            tagOptions.filter { it !in tags }.take(20).toImmutableList()
                        } else {
                            persistentListOf()
                        },
                        showParentTaskField = showParentTaskField,
                        parentTask = parentTask,
                        parentTaskOptions = if (hasParentTaskOptions) {
                            parentTaskOptions.filter { it.id != taskId }.toImmutableList()
                        } else {
                            persistentListOf()
                        },
                        changed = listOf(
                            name == existingTask?.name.orEmpty(),
                            note == existingTask?.note.orEmpty(),
                            deadlineDate == existingTask?.deadlineDate,
                            deadlineTime == existingTask?.deadlineTime,
                            startAfterDate == existingTask?.startAfterDate,
                            startAfterTime == existingTask?.startAfterTime,
                            scheduledDate == existingTask?.scheduledDate,
                            scheduledTime == existingTask?.scheduledTime,
                            tags == existingTags,
                            recurrenceUiState == existingRecurrenceUiState,
                            parentTask == existingParentTaskResult.getOrNull(),
                        ).contains(false),
                        userMessage = userMessage,
                    )
                },
                onFailure = TaskFormUiState::Failure,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskFormUiState.Pending,
    )

    val recurrenceUiStateOrDefault = recurrenceUiState.map {
        it ?: TaskRecurrenceUiState(
            start = startAfterDate.value ?: LocalDate.now(clock),
            type = RecurrenceType.Day,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TaskRecurrenceUiState(
            start = startAfterDate.value ?: LocalDate.now(clock),
            type = RecurrenceType.Day,
            step = 1,
            weekdays = persistentSetOf(),
            locale = locale,
        ),
    )

    val currentWeekday: DayOfWeek = LocalDate.now(clock).dayOfWeek

    init {
        tagRepository.hasTagsStream.onEach { hasTags ->
            if (!hasTags) {
                tagFieldState.value = TagFieldState.Hidden
            }
        }.catch { e ->
            Timber.e(e, "Failed to query has tags")
            userMessage.value = UserMessage.String(R.string.task_form_fetch_tags_error)
            tagFieldState.value = TagFieldState.Hidden
        }.launchIn(viewModelScope)
        viewModelScope.launch {
            val existingTask = existingTaskStream.first().getOrNull() ?: return@launch
            initialName.value = existingTask.name
            initialNote.value = existingTask.note
            name.value = existingTask.name
            note.value = existingTask.note
            deadlineDate.value = existingTask.deadlineDate
            deadlineTime.value = existingTask.deadlineTime
            startAfterDate.value = existingTask.startAfterDate
            startAfterTime.value = existingTask.startAfterTime
            scheduledDate.value = existingTask.scheduledDate
            scheduledTime.value = existingTask.scheduledTime
            recurrenceUiState.value = existingRecurrencesStream.first().getOrNull()?.let {
                TaskRecurrenceUiState.tryFromEntities(it, locale)
            }
            parentTask.value = existingParentTaskStream.first().getOrNull()
            tags.value =
                existingTagsStream.first().getOrNull()?.toPersistentList() ?: persistentListOf()
        }
    }

    fun updateName(value: String) {
        name.value = value
    }

    fun updateNote(value: String) {
        note.value = value
    }

    fun updateDeadlineDate(value: LocalDate?) {
        deadlineDate.value = value
    }

    fun updateDeadlineTime(value: LocalTime?) {
        deadlineTime.value = value
    }

    fun updateStartAfterDate(value: LocalDate?) {
        startAfterDate.value = value
    }

    fun updateStartAfterTime(value: LocalTime?) {
        startAfterTime.value = value
    }

    fun updateScheduledDate(value: LocalDate?) {
        scheduledDate.value = value
        if (value == null) {
            scheduledTime.value = null
        }
    }

    fun updateScheduledTime(value: LocalTime?) {
        scheduledTime.value = value
    }

    fun updateParentTask(value: Task?) {
        parentTask.value = value
        if (value == null) {
            parentTaskQuery.value = ""
        }
    }

    fun startEditTag() {
        tagFieldState.value = TagFieldState.Open
    }

    fun addTag(tag: Tag) {
        tags.update { it.add(tag) }
        tagQuery.value = ""
        tagFieldState.value = TagFieldState.Closed
    }

    fun removeTag(tag: Tag) {
        tags.update { it.remove(tag) }
    }

    fun updateRecurrence(value: TaskRecurrenceUiState?) {
        recurrenceUiState.value =
            if (value?.type == RecurrenceType.Week && value.weekdays.isEmpty()) {
                value.copy(weekdays = persistentSetOf(value.start.dayOfWeek))
            } else {
                value
            }

        if (value == null) {
            scheduledTime.value = null
        } else {
            deadlineDate.value = null
            startAfterDate.value = null
            scheduledDate.value = null
        }
    }

    fun searchParentTasks(query: String) {
        parentTaskQuery.value = query
    }

    fun searchTags(query: String) {
        tagQuery.value = query
    }

    fun saveTask() {
        val state = (uiState.value as? TaskFormUiState.Success) ?: return
        if (name.value.isBlank()) return
        val nonRecurringHasScheduledTime =
            state.scheduledDate == null && state.scheduledTime != null && state.recurrence == null
        val scheduledDate = if (nonRecurringHasScheduledTime) {
            LocalDate.now()
        } else {
            state.scheduledDate
        }
        val recurrences = when (state.recurrence?.type) {
            null -> emptyList()
            RecurrenceType.Week -> {
                state.recurrence.weekdays.map {
                    val start = state.recurrence.start.plusDays(
                        (7 + it.value - state.recurrence.start.dayOfWeek.value) % 7L
                    )
                    TaskRecurrence(
                        taskId = taskId ?: 0L,
                        start = start,
                        type = state.recurrence.type,
                        step = state.recurrence.step,
                    )
                }
            }

            else -> {
                listOf(
                    TaskRecurrence(
                        taskId = taskId ?: 0L,
                        start = state.recurrence.start,
                        type = state.recurrence.type,
                        step = state.recurrence.step,
                    )
                )
            }
        }

        if (taskId == null) {
            val form = NewTaskForm(
                name = name.value,
                note = note.value,
                deadlineDate = state.deadlineDate,
                deadlineTime = state.deadlineTime,
                startAfterDate = state.startAfterDate,
                startAfterTime = state.startAfterTime,
                scheduledDate = scheduledDate,
                scheduledTime = state.scheduledTime,
                tagIds = state.tags.map { it.id }.toSet(),
                recurrences = recurrences,
                parentTaskId = state.parentTask?.id,
                clock = clock,
            )
            viewModelScope.launch {
                try {
                    taskRepository.create(form)
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task with form: %s", form)
                    userMessage.value = UserMessage.String(R.string.task_form_save_error_new)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingTask = checkNotNull(existingTaskStream.first().getOrNull())
                    val existingTags = existingTagsStream.first()
                    val existingRecurrences =
                        existingRecurrencesStream.first().getOrDefault(emptyList())
                    val existingParent = existingParentTaskStream.first()
                    val hasTags = tagFieldState.value != TagFieldState.Hidden
                    val taskCount = taskCountStream.first()
                    val existingTagIds = if (hasTags) {
                        existingTags.fold(
                            onSuccess = { tags ->
                                tags.map { it.id }.toSet()
                            },
                            onFailure = { emptySet() },
                        )
                    } else {
                        emptySet()
                    }
                    taskRepository.update(
                        EditTaskForm(
                            name = name.value,
                            note = note.value,
                            deadlineDate = state.deadlineDate,
                            deadlineTime = state.deadlineTime,
                            startAfterDate = state.startAfterDate,
                            startAfterTime = state.startAfterTime,
                            scheduledDate = scheduledDate,
                            scheduledTime = state.scheduledTime,
                            tagIds = if (hasTags) {
                                state.tags.map { it.id }.toSet()
                            } else {
                                existingTagIds
                            },
                            recurrences = recurrences,
                            parentUpdateType = when {
                                taskCount.isFailure -> PathUpdateType.Keep
                                existingParent.fold(
                                    onSuccess = { it?.id == state.parentTask?.id },
                                    onFailure = { true },
                                ) -> PathUpdateType.Keep

                                state.parentTask == null -> PathUpdateType.Remove
                                else -> PathUpdateType.Replace(state.parentTask.id)
                            },
                            existingTask = existingTask,
                            existingTagIds = existingTagIds,
                            existingRecurrences = existingRecurrences,
                        )
                    )
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task with id: %d", taskId)
                    userMessage.value = UserMessage.String(R.string.task_form_save_error_edit)
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }
}
