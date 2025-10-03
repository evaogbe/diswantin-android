package io.github.evaogbe.diswantin.task.ui

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.evaogbe.diswantin.data.ClockMonitor
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
import io.github.evaogbe.diswantin.ui.viewmodel.BaseViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    clockMonitor: ClockMonitor,
) : BaseViewModel(clockMonitor) {
    private val route = savedStateHandle.toRoute<TaskFormRoute.Main>()

    private val taskId = route.id

    val isNew = taskId == null

    private val initialName =
        savedStateHandle.getMutableStateFlow(INITIAL_NAME_KEY, route.name.orEmpty())

    private val initialNote = savedStateHandle.getMutableStateFlow(INITIAL_NOTE_KEY, "")

    private val name = MutableStateFlow("")

    private val note = MutableStateFlow("")

    private val _recurrenceUiState = MutableStateFlow<TaskRecurrenceUiState?>(null)

    val recurrenceUiState = _recurrenceUiState.asStateFlow()

    private val deadlineDate =
        savedStateHandle.getMutableStateFlow<LocalDate?>(DEADLINE_DATE_KEY, null)

    private val deadlineTime =
        savedStateHandle.getMutableStateFlow<LocalTime?>(DEADLINE_TIME_KEY, null)

    private val startAfterDate =
        savedStateHandle.getMutableStateFlow<LocalDate?>(START_AFTER_DATE_KEY, null)

    private val startAfterTime =
        savedStateHandle.getMutableStateFlow<LocalTime?>(START_AFTER_TIME_KEY, null)

    private val scheduledDate =
        savedStateHandle.getMutableStateFlow<LocalDate?>(SCHEDULED_DATE_KEY, null)

    private val scheduledTime =
        savedStateHandle.getMutableStateFlow<LocalTime?>(SCHEDULE_TIME_KEY, null)

    private val parent = savedStateHandle.getMutableStateFlow<ParentTask?>(PARENT_KEY, null)

    private val tagFieldState =
        savedStateHandle.getMutableStateFlow(TAG_FIELD_STATE_KEY, TagFieldState.Closed)

    private val tags = MutableStateFlow(persistentListOf<Tag>())

    private val tagQuery = savedStateHandle.getMutableStateFlow(TAG_QUERY_KEY, "")

    private val isSaved = MutableStateFlow(false)

    private val userMessage = MutableStateFlow<TaskFormUserMessage?>(null)

    private val taskCountStream =
        taskRepository.getTaskCount().map<Long, Result<Long>> { Result.Success(it) }
            .catch { e ->
                Timber.e(e, "Failed to fetch task count")
                userMessage.value = TaskFormUserMessage.FetchParentTaskError
                emit(Result.Failure(e))
            }

    private val existingTaskStream = taskId?.let { id ->
        taskRepository.getTaskById(id).map<Task, Result<Task>> { Result.Success(it) }
            .catch { e ->
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

    private val existingParentStream = taskId?.let { id ->
        taskRepository.getParent(id).map<Task?, Result<ParentTask?>> { task ->
            Result.Success(task?.let(ParentTask::fromTask))
        }.catch { e ->
            Timber.e(e, "Failed to fetch parent task by child id: %d", id)
            userMessage.value = TaskFormUserMessage.FetchParentTaskError
            emit(Result.Failure(e))
        }
    } ?: flowOf(Result.Success(null))

    private val existingTagsStream = taskId?.let { id ->
        tagRepository.getTagsByTaskId(id, size = Task.MAX_TAGS)
            .map<List<Tag>, Result<List<Tag>>> { Result.Success(it) }.catch { e ->
                Timber.e(e, "Failed to fetch tags by task id: %d", id)
                userMessage.value = TaskFormUserMessage.FetchTagsError
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
        _recurrenceUiState,
        deadlineDate,
        deadlineTime,
        startAfterDate,
        startAfterTime,
        scheduledDate,
        scheduledTime,
        taskCountStream,
        parent,
        tagFieldState,
        tags,
        tagQuery,
        tagQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                tagRepository.search(query.trim(), size = Task.MAX_TAGS * 2).catch { e ->
                    Timber.e(e, "Failed to search for tag by query: %s", query)
                    userMessage.value = TaskFormUserMessage.SearchTagsError
                }
            }
        },
        isSaved,
        userMessage,
        existingTaskStream,
        existingRecurrencesStream,
        existingTagsStream,
        existingParentStream,
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
        val parent = args[12] as ParentTask?
        val tagFieldState = args[13] as TagFieldState
        val tags = args[14] as ImmutableList<Tag>
        val tagQuery = (args[15] as String).trim()
        val tagOptions = args[16] as List<Tag>
        val isSaved = args[17] as Boolean
        val userMessage = args[18] as TaskFormUserMessage?
        val existingTaskResult = args[19] as Result<Task?>
        val existingRecurrencesResult = args[20] as Result<List<TaskRecurrence>>
        val existingTagsResult = args[21] as Result<List<Tag>>
        val existingParentResult = args[22] as Result<ParentTask?>

        if (isSaved) {
            TaskFormUiState.Saved
        } else {
            existingTaskResult.zip(existingRecurrencesResult).fold(
                onSuccess = { (existingTask, existingRecurrences) ->
                    val singleTagOption = tagOptions.singleOrNull()?.name
                    val hasTagOptions =
                        tags.none { it.name == tagQuery } || tagQuery != singleTagOption
                    val taskCount = taskCountResult.getOrDefault(0L)
                    val hasOtherTasks = taskCount > if (isNew) 0L else 1L
                    val showParentField = existingParentResult.isSuccess && hasOtherTasks
                    val existingTags =
                        existingTagsResult.getOrNull()?.toPersistentList() ?: persistentListOf()
                    val existingRecurrenceUiState =
                        TaskRecurrenceUiState.tryFromEntities(existingRecurrences)
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
                            tagOptions.filter { it !in tags }.take(Task.MAX_TAGS).toImmutableList()
                        } else {
                            persistentListOf()
                        },
                        showParentField = showParentField,
                        parent = parent,
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
                            parent == existingParentResult.getOrNull(),
                        ).contains(false),
                        userMessage = userMessage,
                    )
                },
                onFailure = TaskFormUiState::Failure,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = TaskFormUiState.Pending,
    )

    init {
        val recurrencesBundle = savedStateHandle.get<Bundle>(RECURRENCES_KEY)
        if (recurrencesBundle != null) {
            _recurrenceUiState.value = recurrencesBundle.restoreTaskRecurrenceUiState()
        }
        savedStateHandle.setSavedStateProvider(RECURRENCES_KEY) {
            _recurrenceUiState.value?.bundle ?: Bundle()
        }

        val tagsBundle = savedStateHandle.get<Bundle>(TAGS_KEY)
        if (tagsBundle != null) {
            tags.value = tagsBundle.restoreTags()
        }
        savedStateHandle.setSavedStateProvider(TAGS_KEY) {
            bundleOf(TAGS_KEY to tags.value.toTypedArray())
        }
    }

    private var initializeCalled = false

    @MainThread
    fun initialize() {
        if (initializeCalled) return
        initializeCalled = true

        viewModelScope.launch {
            val hasTags = try {
                tagRepository.hasTags()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to query has tags")
                userMessage.value = TaskFormUserMessage.FetchTagsError
                false
            }
            if (!hasTags) {
                tagFieldState.value = TagFieldState.Hidden
            }
        }

        if (!isNew && savedStateHandle.get<Boolean>(INITIALIZED_KEY) != true) {
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
                _recurrenceUiState.value = existingRecurrencesStream.first().getOrNull()?.let {
                    TaskRecurrenceUiState.tryFromEntities(it)
                }
                parent.value = existingParentStream.first().getOrNull()
                tags.value =
                    existingTagsStream.first().getOrNull()?.toPersistentList() ?: persistentListOf()
                savedStateHandle[INITIALIZED_KEY] = true
            }
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

    fun updateParent(value: ParentTask?) {
        parent.value = value
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
        _recurrenceUiState.value =
            if (value?.type == RecurrenceType.Week && value.weekdays.isEmpty()) {
                value.copy(weekdays = persistentSetOf(value.startDate.dayOfWeek))
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

    fun searchTags(query: String) {
        tagQuery.value = query
    }

    fun commitInputs() {
        initialName.value = name.value
        initialNote.value = note.value
    }

    fun saveTask() {
        val state = (uiState.value as? TaskFormUiState.Success) ?: return
        if (name.value.isBlank()) return
        val now = ZonedDateTime.now(clock.value)
        val nonRecurringHasScheduledTime =
            state.scheduledDate == null && state.scheduledTime != null && state.recurrence == null
        val scheduledDate = if (nonRecurringHasScheduledTime) {
            now.toLocalDate()
        } else {
            state.scheduledDate
        }
        val recurrences = when (state.recurrence?.type) {
            null -> emptyList()
            RecurrenceType.Week -> {
                state.recurrence.weekdays.map {
                    val startDate = state.recurrence.startDate.plusDays(
                        (7 + it.value - state.recurrence.startDate.dayOfWeek.value) % 7L
                    )
                    TaskRecurrence(
                        taskId = taskId ?: 0L,
                        startDate = startDate,
                        endDate = state.recurrence.endDate,
                        type = state.recurrence.type,
                        step = state.recurrence.step,
                    )
                }
            }

            else -> {
                listOf(
                    TaskRecurrence(
                        taskId = taskId ?: 0L,
                        startDate = state.recurrence.startDate,
                        endDate = state.recurrence.endDate,
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
                parentTaskId = state.parent?.id,
                now = now.toInstant(),
            )
            viewModelScope.launch {
                try {
                    taskRepository.create(form)
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create task with form: %s", form)
                    userMessage.value = TaskFormUserMessage.CreateError
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val existingTagsResult = existingTagsStream.first()
                    val existingParentResult = existingParentStream.first()
                    val hasTags = tagFieldState.value != TagFieldState.Hidden
                    val taskCountResult = taskCountStream.first()
                    val existingTagIds = if (hasTags) {
                        existingTagsResult.fold(
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
                                taskCountResult.isFailure -> PathUpdateType.Keep
                                existingParentResult.fold(
                                    onSuccess = { it?.id == state.parent?.id },
                                    onFailure = { true },
                                ) -> PathUpdateType.Keep

                                state.parent == null -> PathUpdateType.Remove
                                else -> PathUpdateType.Replace(state.parent.id)
                            },
                            now = now.toInstant(),
                            existingId = taskId,
                            existingTagIds = existingTagIds,
                        )
                    )
                    isSaved.value = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update task with id: %d", taskId)
                    userMessage.value = TaskFormUserMessage.EditError
                }
            }
        }
    }

    fun userMessageShown() {
        userMessage.value = null
    }

    private val TaskRecurrenceUiState.bundle
        get() = bundleOf(
            RECURRENCE_START_DATE_KEY to startDate,
            RECURRENCE_END_DATE_KEY to endDate,
            RECURRENCE_TYPE_KEY to type,
            RECURRENCE_STEP_KEY to step,
            RECURRENCE_WEEKDAYS_KEY to weekdays.map { it.value }.toIntArray(),
        )

    private fun Bundle.restoreTaskRecurrenceUiState(): TaskRecurrenceUiState? {
        val startDate =
            BundleCompat.getSerializable(this, RECURRENCE_START_DATE_KEY, LocalDate::class.java)
        val endDate =
            BundleCompat.getSerializable(this, RECURRENCE_END_DATE_KEY, LocalDate::class.java)
        val type =
            BundleCompat.getSerializable(this, RECURRENCE_TYPE_KEY, RecurrenceType::class.java)
        val step = getInt(RECURRENCE_STEP_KEY)
        val weekdays =
            getIntArray(RECURRENCE_WEEKDAYS_KEY)?.map(DayOfWeek::of).orEmpty().toPersistentSet()
        return if (startDate != null && type != null && step > 0) {
            TaskRecurrenceUiState(
                startDate = startDate,
                endDate = endDate,
                type = type,
                step = step,
                weekdays = weekdays,
            )
        } else {
            null
        }
    }

    private fun Bundle.restoreTags(): PersistentList<Tag> {
        return BundleCompat.getParcelableArray(this, TAGS_KEY, Tag::class.java).orEmpty()
            .map { it as Tag }.toTypedArray().toPersistentList()
    }

    override fun onCleared() {
        commitInputs()
        super.onCleared()
    }
}

private const val INITIALIZED_KEY = "initialized"

private const val INITIAL_NAME_KEY = "initialName"

private const val INITIAL_NOTE_KEY = "initialNote"

private const val DEADLINE_DATE_KEY = "deadlineDate"

private const val DEADLINE_TIME_KEY = "deadlineTime"

private const val START_AFTER_DATE_KEY = "startAfterDate"

private const val START_AFTER_TIME_KEY = "startAfterTime"

private const val SCHEDULED_DATE_KEY = "scheduledDate"

private const val SCHEDULE_TIME_KEY = "scheduledTime"

private const val RECURRENCES_KEY = "recurrences"

private const val RECURRENCE_START_DATE_KEY = "recurrenceStartDate"

private const val RECURRENCE_END_DATE_KEY = "recurrenceEndDate"

private const val RECURRENCE_TYPE_KEY = "recurrenceType"

private const val RECURRENCE_STEP_KEY = "recurrenceStep"

private const val RECURRENCE_WEEKDAYS_KEY = "recurrenceWeekdays"

private const val PARENT_KEY = "parent"

private const val TAGS_KEY = "tags"

private const val TAG_FIELD_STATE_KEY = "tagFieldState"

private const val TAG_QUERY_KEY = "tagQuery"
