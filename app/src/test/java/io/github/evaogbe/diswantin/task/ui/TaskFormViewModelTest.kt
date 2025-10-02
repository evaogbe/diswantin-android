package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isDataClassEqualTo
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskTag
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new without taskId`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createTaskFormViewModelForNew()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = "",
                initialNote = "",
                recurrence = null,
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Hidden,
                tags = persistentListOf(),
                tagOptions = persistentListOf(),
                showParentField = false,
                parent = null,
                changed = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `initializes for edit with taskId`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask().copy(
            note = loremFaker.quote.famousLastWords(),
            deadlineDate = LocalDate.parse("2024-08-23"),
            deadlineTime = LocalTime.parse("18:00"),
            startAfterDate = LocalDate.parse("2024-08-22"),
            startAfterTime = LocalTime.parse("17:00"),
        )
        val parentTask = genTask(id = 2L)
        val tag = genTag()
        val viewModel = createTaskFormViewModelForEdit(
            initDatabase = { db ->
                db.insertTask(task)
                db.insertTask(parentTask)
                db.insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        startDate = LocalDate.parse("2024-08-22"),
                        endDate = LocalDate.parse("2025-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    )
                )
                db.insertChain(parentTask.id, task.id)
                db.insertTag(tag, setOf(task.id))
            },
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isFalse()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = task.name,
                initialNote = task.note,
                recurrence = TaskRecurrenceUiState(
                    startDate = LocalDate.parse("2024-08-22"),
                    endDate = LocalDate.parse("2025-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    weekdays = persistentSetOf(),
                ),
                deadlineDate = LocalDate.parse("2024-08-23"),
                deadlineTime = LocalTime.parse("18:00"),
                startAfterDate = LocalDate.parse("2024-08-22"),
                startAfterTime = LocalTime.parse("17:00"),
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Closed,
                tags = persistentListOf(tag),
                tagOptions = persistentListOf(),
                showParentField = true,
                parent = ParentTask.fromTask(parentTask),
                changed = false,
                userMessage = null,
            )
        )
        assertThat(viewModel.recurrenceUiState.value).isNotNull().isDataClassEqualTo(
            TaskRecurrenceUiState(
                startDate = LocalDate.parse("2024-08-22"),
                endDate = LocalDate.parse("2025-08-22"),
                type = RecurrenceType.Day,
                step = 1,
                weekdays = persistentSetOf(),
            )
        )
    }

    @Test
    fun `uiState emits failure when fetch existing task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getTaskById(any()) } returns flow { throw exception }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure(exception))
        }

    @Test
    fun `uiState emits failure when fetch existing task recurrences fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getTaskRecurrencesByTaskId(any()) } returns flow {
                        throw exception
                    }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure(exception))
        }

    @Test
    fun `uiState emits changed when new form and name changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateName(name)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and note changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val note = loremFaker.quote.famousLastWords()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateNote(note)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and deadline date changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val deadlineDate = faker.random.randomFutureDate().toLocalDate()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateDeadlineDate(deadlineDate)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = deadlineDate,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and deadline time changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val deadlineTime = faker.random.randomFutureDate().toLocalTime()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateDeadlineTime(deadlineTime)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = deadlineTime,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and start after date changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val startAfterDate = faker.random.randomFutureDate().toLocalDate()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateStartAfterDate(startAfterDate)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = startAfterDate,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and start after time changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val startAfterTime = faker.random.randomFutureDate().toLocalTime()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateStartAfterTime(startAfterTime)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = startAfterTime,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and scheduled date changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val scheduledDate = faker.random.randomFutureDate().toLocalDate()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateScheduledDate(scheduledDate)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = scheduledDate,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and scheduled time changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val scheduledAt = faker.random.randomFutureDate()
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateScheduledDate(scheduledAt.toLocalDate())
            viewModel.updateScheduledTime(scheduledAt.toLocalTime())

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = scheduledAt.toLocalDate(),
                    scheduledTime = scheduledAt.toLocalTime(),
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and tag changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = genTag()
            val viewModel = createTaskFormViewModelForNew({ db ->
                db.insertTag(tag)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.addTag(tag)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Closed,
                    tags = persistentListOf(tag),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and recurrence changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val recurrence = TaskRecurrenceUiState(
                startDate = faker.random.randomPastDate().toLocalDate(),
                endDate = null,
                type = RecurrenceType.Day,
                step = 1,
                weekdays = persistentSetOf()
            )
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateRecurrence(recurrence)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = recurrence,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and parent task changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val parentTask = genTask()
            val viewModel = createTaskFormViewModelForNew({ db ->
                db.insertTask(parentTask)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateParent(ParentTask.fromTask(parentTask))

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = true,
                    parent = ParentTask.fromTask(parentTask),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and name changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateName("")

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and note changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val note = loremFaker.quote.famousLastWords()
            val task = genTask().copy(note = note)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateNote("")

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and deadline date changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val deadlineDate = faker.random.randomFutureDate().toLocalDate()
            val task = genTask().copy(deadlineDate = deadlineDate)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateDeadlineDate(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and deadline time changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val deadlineTime = faker.random.randomFutureDate().toLocalTime()
            val task = genTask().copy(deadlineTime = deadlineTime)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateDeadlineTime(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and start after date changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val startAfterDate = faker.random.randomFutureDate().toLocalDate()
            val task = genTask().copy(startAfterDate = startAfterDate)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateStartAfterDate(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and start after time changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val startAfterTime = faker.random.randomFutureDate().toLocalTime()
            val task = genTask().copy(startAfterTime = startAfterTime)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateStartAfterTime(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and scheduled date changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val scheduledDate = faker.random.randomFutureDate().toLocalDate()
            val task = genTask().copy(scheduledDate = scheduledDate)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateScheduledDate(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and scheduled time changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val scheduledAt = faker.random.randomFutureDate()
            val task = genTask().copy(
                scheduledDate = scheduledAt.toLocalDate(),
                scheduledTime = scheduledAt.toLocalTime(),
            )
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateScheduledTime(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = scheduledAt.toLocalDate(),
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and tag changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val tag = genTag()
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
                db.insertTag(tag, setOf(task.id))
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.removeTag(tag)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Closed,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and recurrence changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
                db.insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        startDate = faker.random.randomPastDate().toLocalDate(),
                        type = RecurrenceType.Day,
                        step = 1
                    )
                )
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateRecurrence(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and parent task changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
                db.insertTask(parentTask)
                db.insertChain(parentTask.id, task.id)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateParent(null)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = true,
                    parent = null,
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState shows parent task field when taskId is null and repository has tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createTaskFormViewModelForNew({ db ->
                db.insertTask(genTask())
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = true,
                    parent = null,
                    changed = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState shows parent task field when taskId is present and repository has other tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val viewModel = createTaskFormViewModelForEdit({ db ->
                db.insertTask(task)
                db.insertTask(parentTask)
            })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = true,
                    parent = null,
                    changed = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides parent task field when fetch count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createTaskFormViewModelForNew(
                initDatabase = { db -> db.insertTask(genTask()) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getTaskCount() } returns flow { throw RuntimeException("Test") }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchParentTaskError,
                )
            )
        }

    @Test
    fun `uiState hides parent task field when fetch existing task parent fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getParent(any()) } returns flow {
                        throw RuntimeException("Test")
                    }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchParentTaskError,
                )
            )
        }

    @Test
    fun `uiState shows tag field when has tags`() = runTest(mainDispatcherRule.testDispatcher) {
        val tag = genTag()
        val viewModel = createTaskFormViewModelForNew({ db ->
            db.insertTag(tag)
        })

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = "",
                initialNote = "",
                recurrence = null,
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Closed,
                tags = persistentListOf(),
                tagOptions = persistentListOf(),
                showParentField = false,
                parent = null,
                changed = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState hides tag field when does not have tags`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createTaskFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides tag field when query has tags fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = genTag()
            val viewModel = createTaskFormViewModelForNew(
                initDatabase = { db -> db.insertTag(tag) },
                initTagRepositorySpy = { repository ->
                    coEvery { repository.hasTags() } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchTagsError,
                )
            )
        }

    @Test
    fun `uiState hides tag field when fetch existing tags fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val tag = genTag()
            val viewModel = createTaskFormViewModelForEdit(
                initDatabase = { db ->
                    db.insertTask(task)
                    db.insertTag(tag, setOf(task.id))
                },
                initTagRepositorySpy = { repository ->
                    every { repository.getTagsByTaskId(any(), any()) } returns flow {
                        throw RuntimeException("Test")
                    }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchTagsError,
                )
            )
        }

    @Test
    fun `searchTags fetches tagOptions`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val tags = List(faker.random.nextInt(min = 2, max = 5)) {
            val createdAt = faker.random.randomPastDate().toInstant()
            Tag(
                id = it + 1L,
                name = faker.string.unique.regexify("""$query \w+"""),
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
        val viewModel = createTaskFormViewModelForNew({ db ->
            tags.forEach(db::insertTag)
        })

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchTags(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = "",
                initialNote = "",
                recurrence = null,
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Closed,
                tags = persistentListOf(),
                tagOptions = tags.toImmutableList(),
                showParentField = false,
                parent = null,
                changed = false,
                userMessage = null,
            )
        )

        viewModel.addTag(tags.first())
        viewModel.searchTags(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = "",
                initialNote = "",
                recurrence = null,
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Closed,
                tags = persistentListOf(tags.first()),
                tagOptions = tags.drop(1).toImmutableList(),
                showParentField = false,
                parent = null,
                changed = true,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTags shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val tagCreatedAt = faker.random.randomPastDate().toInstant()
            val tag = Tag(
                name = faker.string.regexify("""$query \w+"""),
                createdAt = tagCreatedAt,
                updatedAt = tagCreatedAt,
            )
            val viewModel = createTaskFormViewModelForNew(
                initDatabase = { db -> db.insertTag(tag) },
                initTagRepositorySpy = { repository ->
                    every { repository.search(any(), any()) } returns flow {
                        throw RuntimeException("Test")
                    }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.searchTags(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Closed,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.SearchTagsError,
                )
            )
        }

    @Test
    fun `saveTask creates task without taskId`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val now = Instant.parse("2024-08-22T08:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("America/New_York"))
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForNew(),
            taskRepository,
            tagRepository,
            clock,
        )
        viewModel.initialize()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = "",
                initialNote = "",
                recurrence = null,
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Hidden,
                tags = persistentListOf(),
                tagOptions = persistentListOf(),
                showParentField = false,
                parent = null,
                changed = false,
                userMessage = null,
            )
        )

        viewModel.updateName(name)
        viewModel.updateDeadlineDate(LocalDate.parse("2024-08-23"))
        viewModel.updateDeadlineTime(LocalTime.parse("17:00"))
        viewModel.updateRecurrence(
            TaskRecurrenceUiState(
                startDate = LocalDate.parse("2024-08-22"),
                endDate = null,
                type = RecurrenceType.Day,
                step = 1,
                weekdays = persistentSetOf(),
            ),
        )
        viewModel.saveTask()

        assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)

        val task = taskRepository.tasks.single()
        val taskRecurrence = taskRepository.getTaskRecurrencesByTaskId(task.id).first().single()
        assertThat(task).isEqualToIgnoringGivenProperties(
            Task(
                createdAt = now,
                name = name,
                deadlineDate = null,
                deadlineTime = LocalTime.parse("17:00"),
                updatedAt = now,
            ),
            Task::id,
        )
        assertThat(taskRecurrence).isEqualToIgnoringGivenProperties(
            TaskRecurrence(
                taskId = task.id,
                startDate = LocalDate.parse("2024-08-22"),
                type = RecurrenceType.Day,
                step = 1,
                endDate = null,
            ),
            TaskRecurrence::id,
        )
    }

    @Test
    fun `saveTask shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val viewModel = createTaskFormViewModelForNew(
                initDatabase = {},
                initTaskRepositorySpy = { repository ->
                    coEvery { repository.create(any()) } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = "",
                    initialNote = "",
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = TaskFormUserMessage.CreateError,
                )
            )
        }

    @Test
    fun `saveTask updates task with taskId`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTask().copy(
            deadlineDate = LocalDate.parse("2024-08-23"),
            deadlineTime = LocalTime.parse("22:00"),
            startAfterDate = LocalDate.parse("2024-08-22"),
            startAfterTime = LocalTime.parse("21:00"),
        )
        val tag = genTag()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTag(tag)
        }
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
        )
        viewModel.initialize()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                initialName = task.name,
                initialNote = task.note,
                recurrence = null,
                deadlineDate = LocalDate.parse("2024-08-23"),
                deadlineTime = LocalTime.parse("22:00"),
                startAfterDate = LocalDate.parse("2024-08-22"),
                startAfterTime = LocalTime.parse("21:00"),
                scheduledDate = null,
                scheduledTime = null,
                tagFieldState = TagFieldState.Closed,
                tags = persistentListOf(),
                tagOptions = persistentListOf(),
                showParentField = false,
                parent = null,
                changed = false,
                userMessage = null,
            )
        )

        viewModel.updateName(name)
        viewModel.updateDeadlineDate(null)
        viewModel.updateDeadlineTime(null)
        viewModel.updateStartAfterDate(null)
        viewModel.updateStartAfterTime(null)
        viewModel.updateScheduledDate(LocalDate.parse("2024-08-22"))
        viewModel.updateScheduledTime(LocalTime.parse("17:00"))
        viewModel.updateRecurrence(
            TaskRecurrenceUiState(
                startDate = LocalDate.parse("2024-08-22"),
                endDate = LocalDate.parse("2025-08-22"),
                type = RecurrenceType.Week,
                step = 2,
                weekdays = persistentSetOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            ),
        )
        viewModel.addTag(tag)
        viewModel.saveTask()

        assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
            task.copy(
                name = name,
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = LocalTime.parse("17:00"),
                updatedAt = Instant.parse("2024-08-22T08:00:00Z"),
            )
        )
        assertThat(tagRepository.getTagsByTaskId(task.id, 2).first()).containsExactly(tag)
        assertThat(
            taskRepository.getTaskRecurrencesByTaskId(task.id).first()
                .map { it.copy(id = 0) }).containsExactly(
            TaskRecurrence(
                taskId = task.id,
                startDate = LocalDate.parse("2024-08-22"),
                type = RecurrenceType.Week,
                step = 2,
                endDate = LocalDate.parse("2025-08-22"),
            ),
            TaskRecurrence(
                taskId = task.id,
                startDate = LocalDate.parse("2024-08-26"),
                type = RecurrenceType.Week,
                step = 2,
                endDate = LocalDate.parse("2025-08-22"),
            ),
        )
    }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    coEvery { repository.update(any()) } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = true,
                    userMessage = TaskFormUserMessage.EditError,
                )
            )
        }

    @Test
    fun `saveTask maintains parent task when fetch task count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTask(parentTask)
                insertChain(parentId = parentTask.id, childId = task.id)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getTaskCount() } returns flow { throw RuntimeException("Test") }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
            )
            viewModel.initialize()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = ParentTask.fromTask(parentTask),
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchParentTaskError,
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name, updatedAt = Instant.parse("2024-08-22T08:00:00Z")),
                parentTask,
            )
            assertThat(taskRepository.getParent(task.id).first()).isEqualTo(parentTask)
        }

    @Test
    fun `saveTask maintains parent task when fetch existing parent fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTask(parentTask)
                insertChain(parentId = parentTask.id, childId = task.id)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getParent(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
            )
            viewModel.initialize()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchParentTaskError,
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name, updatedAt = Instant.parse("2024-08-22T08:00:00Z")),
                parentTask,
            )
            assertThat(FakeTaskRepository(db).getParent(task.id).first()).isEqualTo(parentTask)
        }

    @Test
    fun `saveTask maintains tag when query has tag fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val tag = genTag()
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTag(tag, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db)
            val tagRepository = spyk(FakeTagRepository(db))
            coEvery { tagRepository.hasTags() } throws RuntimeException("Test")

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
            )
            viewModel.initialize()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(tag),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchTagsError,
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name, updatedAt = Instant.parse("2024-08-22T08:00:00Z"))
            )
            assertThat(tagRepository.getTagsByTaskId(task.id, 2).first()).containsExactly(tag)
        }

    @Test
    fun `saveTask maintains tag when fetch existing tag fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val tag = genTag()
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTag(tag, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.getTagsByTaskId(any(), any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
            )
            viewModel.initialize()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = task.name,
                    initialNote = task.note,
                    recurrence = null,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    tagFieldState = TagFieldState.Hidden,
                    tags = persistentListOf(),
                    tagOptions = persistentListOf(),
                    showParentField = false,
                    parent = null,
                    changed = false,
                    userMessage = TaskFormUserMessage.FetchTagsError,
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name, updatedAt = Instant.parse("2024-08-22T08:00:00Z"))
            )
            assertThat(tagRepository.taskTags.single { it.taskId == task.id }).prop(TaskTag::tagId)
                .isEqualTo(tag.id)
        }

    private fun genTask(id: Long = 1L): Task {
        val createdAt = faker.random.randomPastDate().toInstant()
        return Task(
            id = id,
            createdAt = createdAt,
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            updatedAt = createdAt,
        )
    }

    private fun genTag(): Tag {
        val createdAt = faker.random.randomPastDate().toInstant()
        return Tag(
            id = 1L,
            name = loremFaker.lorem.words(),
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    }

    private fun createSavedStateHandleForNew(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = spyk(SavedStateHandle(mapOf("id" to null, "name" to null)))
        every {
            savedStateHandle.toRoute<TaskFormRoute.Main>()
        } returns TaskFormRoute.Main.new(name = null)
        return savedStateHandle
    }

    private fun createSavedStateHandleForEdit(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = spyk(SavedStateHandle(mapOf("id" to 1L, "name" to null)))
        every {
            savedStateHandle.toRoute<TaskFormRoute.Main>()
        } returns TaskFormRoute.Main.edit(id = 1L)
        return savedStateHandle
    }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun createTaskFormViewModelForNew(
        initDatabase: (FakeDatabase) -> Unit = {},
        initTaskRepositorySpy: ((TaskRepository) -> Unit)? = null,
        initTagRepositorySpy: ((TagRepository) -> Unit)? = null,
    ): TaskFormViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = if (initTaskRepositorySpy == null) {
            FakeTaskRepository(db)
        } else {
            spyk(FakeTaskRepository(db)).also(initTaskRepositorySpy)
        }
        val tagRepository = if (initTagRepositorySpy == null) {
            FakeTagRepository(db)
        } else {
            spyk(FakeTagRepository(db)).also(initTagRepositorySpy)
        }
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForNew(),
            taskRepository,
            tagRepository,
            clock,
        )
        viewModel.initialize()
        return viewModel
    }

    private fun createTaskFormViewModelForEdit(
        initDatabase: (FakeDatabase) -> Unit,
        initTaskRepositorySpy: ((TaskRepository) -> Unit)? = null,
        initTagRepositorySpy: ((TagRepository) -> Unit)? = null,
    ): TaskFormViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = if (initTaskRepositorySpy == null) {
            FakeTaskRepository(db)
        } else {
            spyk(FakeTaskRepository(db)).also(initTaskRepositorySpy)
        }
        val tagRepository = if (initTagRepositorySpy == null) {
            FakeTagRepository(db)
        } else {
            spyk(FakeTagRepository(db)).also(initTagRepositorySpy)
        }
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
        )
        viewModel.initialize()
        return viewModel
    }
}
