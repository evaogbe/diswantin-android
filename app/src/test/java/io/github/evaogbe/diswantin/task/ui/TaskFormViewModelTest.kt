package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskTag
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import java.util.Locale

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
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
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
        val tag = Tag(id = 1L, name = loremFaker.lorem.words())
        val clock = createClock()
        val locale = createLocale()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTask(parentTask)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                )
            )
            insertChain(parentTask.id, task.id)
            insertTag(tag, setOf(task.id))
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
            locale,
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
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    weekdays = persistentSetOf(),
                    locale = locale,
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
                showParentTaskField = true,
                parentTask = parentTask,
                parentTaskOptions = persistentListOf(),
                changed = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState emits failure when fetch existing task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            val tagRepository = FakeTagRepository(db)
            every { taskRepository.getById(any()) } returns flow { throw exception }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getTaskRecurrencesByTaskId(any()) } returns flow {
                throw exception
            }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and tag changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = Tag(id = 1L, name = loremFaker.lorem.words())
            val viewModel = createTaskFormViewModelForNew { db ->
                db.insertTag(tag)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and recurrence changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = createLocale()
            val recurrence = TaskRecurrenceUiState(
                start = faker.random.randomPastDate().toLocalDate(),
                type = RecurrenceType.Day,
                step = 1,
                weekdays = persistentSetOf(),
                locale = locale
            )
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db, clock)
            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForNew(),
                taskRepository,
                tagRepository,
                clock,
                locale,
            )

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and parent task changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val parentTask = genTask()
            val viewModel = createTaskFormViewModelForNew { db ->
                db.insertTask(parentTask)
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateParentTask(parentTask)

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
                    showParentTaskField = true,
                    parentTask = parentTask,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and name changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and tag changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val tag = Tag(id = 1L, name = loremFaker.lorem.words())
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
                db.insertTag(tag, setOf(task.id))
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and recurrence changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
                db.insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = faker.random.randomPastDate().toLocalDate(),
                        type = RecurrenceType.Day,
                        step = 1
                    )
                )
            }

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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
                db.insertTask(parentTask)
                db.insertChain(parentTask.id, task.id)
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateParentTask(null)

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
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState shows parent task field when taskId is null and repository has tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createTaskFormViewModelForNew { db ->
                db.insertTask(genTask())
            }

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
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
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
            val viewModel = createTaskFormViewModelForEdit { db ->
                db.insertTask(task)
                db.insertTask(parentTask)
            }

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
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides parent task field when fetch count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(genTask())
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getCount() } returns flow { throw RuntimeException("Test") }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForNew(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_parent_task_error),
                )
            )
        }

    @Test
    fun `uiState hides parent task field when fetch existing task parent fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getParent(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_parent_task_error),
                )
            )
        }

    @Test
    fun `uiState shows tag field when has tags`() = runTest(mainDispatcherRule.testDispatcher) {
        val tag = Tag(name = loremFaker.lorem.words())
        val viewModel = createTaskFormViewModelForNew { db ->
            db.insertTag(tag)
        }

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
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides tag field when query has tags fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = Tag(name = loremFaker.lorem.words())
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTag(tag)
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.hasTagsStream } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForNew(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_tags_error),
                )
            )
        }

    @Test
    fun `uiState hides tag field when fetch existing tags fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val tag = Tag(name = loremFaker.lorem.words())
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTag(tag, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.getTagsByTaskId(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_tags_error),
                )
            )
        }

    @Test
    fun `searchParentTasks fetches parentTaskOptions`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val tasks = List(faker.random.nextInt(min = 2, max = 5)) {
                Task(
                    id = it + 1L,
                    createdAt = faker.random.randomPastDate().toInstant(),
                    name = "$query ${loremFaker.lorem.words()}",
                )
            }
            val currentTask = tasks.first()
            val taskOptions = tasks.drop(1)
            val viewModel = createTaskFormViewModelForEdit { db ->
                tasks.forEach(db::insertTask)
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.searchParentTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    initialName = currentTask.name,
                    initialNote = currentTask.note,
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
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = taskOptions.toImmutableList(),
                    changed = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `searchParentTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(genTask())
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every {
                taskRepository.search(any(), any())
            } returns flow { throw RuntimeException("Test") }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForNew(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.searchParentTasks(query)

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
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.search_task_options_error),
                )
            )
        }

    @Test
    fun `searchTags fetches tagOptions`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val tags = List(faker.random.nextInt(min = 2, max = 5)) {
            Tag(id = it + 1L, name = faker.string.unique.regexify("""$query \w+"""))
        }
        val viewModel = createTaskFormViewModelForNew { db ->
            tags.forEach { db.insertTag(it) }
        }

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
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
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
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
                changed = true,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTags shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val tag = Tag(name = faker.string.regexify("""$query \w+"""))
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTag(tag)
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.search(any(), any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForNew(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.search_tag_options_error),
                )
            )
        }

    @Test
    fun `saveTask creates task without taskId`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val locale = createLocale()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForNew(),
            taskRepository,
            tagRepository,
            clock,
            locale,
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
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
                changed = false,
                userMessage = null,
            )
        )

        viewModel.updateName(name)
        viewModel.updateDeadlineDate(LocalDate.parse("2024-08-23"))
        viewModel.updateDeadlineTime(LocalTime.parse("17:00"))
        viewModel.updateRecurrence(
            TaskRecurrenceUiState(
                start = LocalDate.parse("2024-08-22"),
                type = RecurrenceType.Day,
                step = 1,
                weekdays = persistentSetOf(),
                locale = locale,
            ),
        )
        viewModel.saveTask()

        assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)

        val task = taskRepository.tasks.single()
        val taskRecurrence = taskRepository.getTaskRecurrencesByTaskId(task.id).first().single()
        assertThat(task).isEqualToIgnoringGivenProperties(
            Task(
                createdAt = Instant.parse("2024-08-22T08:00:00Z"),
                name = name,
                deadlineDate = null,
                deadlineTime = LocalTime.parse("17:00"),
            ),
            Task::id,
        )
        assertThat(taskRecurrence).isEqualToIgnoringGivenProperties(
            TaskRecurrence(
                taskId = task.id,
                start = LocalDate.parse("2024-08-22"),
                type = RecurrenceType.Day,
                step = 1,
            ),
            TaskRecurrence::id,
        )
    }

    @Test
    fun `saveTask shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val db = FakeDatabase()
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            coEvery { taskRepository.create(any()) } throws RuntimeException("Test")

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForNew(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = UserMessage.String(R.string.task_form_save_error_new),
                )
            )
        }

    @Test
    fun `saveTask updates task with taskId`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val clock = createClock()
        val locale = createLocale()
        val task = genTask().copy(
            deadlineDate = LocalDate.parse("2024-08-23"),
            deadlineTime = LocalTime.parse("22:00"),
            startAfterDate = LocalDate.parse("2024-08-22"),
            startAfterTime = LocalTime.parse("21:00"),
        )
        val tag = Tag(id = 1L, name = loremFaker.lorem.words())
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTag(tag)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
            locale,
        )

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
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
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
                start = LocalDate.parse("2024-08-22"),
                type = RecurrenceType.Week,
                step = 2,
                weekdays = persistentSetOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                locale = locale,
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
            )
        )
        assertThat(tagRepository.getTagsByTaskId(task.id).first()).containsExactly(tag)
        assertThat(
            taskRepository.getTaskRecurrencesByTaskId(task.id).first()
                .map { it.copy(id = 0) }).containsExactly(
            TaskRecurrence(
                taskId = task.id,
                start = LocalDate.parse("2024-08-22"),
                type = RecurrenceType.Week,
                step = 2,
            ),
            TaskRecurrence(
                taskId = task.id,
                start = LocalDate.parse("2024-08-26"),
                type = RecurrenceType.Week,
                step = 2,
            ),
        )
    }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            coEvery { taskRepository.update(any()) } throws RuntimeException("Test")

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = true,
                    userMessage = UserMessage.String(R.string.task_form_save_error_edit),
                )
            )
        }

    @Test
    fun `saveTask maintains parent task when fetch task count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTask(parentTask)
                insertChain(parentId = parentTask.id, childId = task.id)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getCount() } returns flow { throw RuntimeException("Test") }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = parentTask,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_parent_task_error),
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name),
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
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTask(parentTask)
                insertChain(parentId = parentTask.id, childId = task.id)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getParent(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_parent_task_error),
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name),
                parentTask,
            )
            assertThat(FakeTaskRepository(db).getParent(task.id).first()).isEqualTo(parentTask)
        }

    @Test
    fun `saveTask maintains tag when query has tag fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val tag = Tag(id = 1L, name = loremFaker.lorem.words())
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTag(tag, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.hasTagsStream } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    tags = persistentListOf(tag),
                    tagOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_tags_error),
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name)
            )
            assertThat(tagRepository.getTagsByTaskId(task.id).first()).containsExactly(tag)
        }

    @Test
    fun `saveTask maintains tag when fetch existing tag fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val tag = Tag(id = 1L, name = loremFaker.lorem.words())
            val clock = createClock()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTag(tag, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.getTagsByTaskId(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = TaskFormViewModel(
                createSavedStateHandleForEdit(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
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
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.task_form_fetch_tags_error),
                )
            )

            viewModel.updateName(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(name = name)
            )
            assertThat(tagRepository.taskTags.single { it.taskId == task.id }).prop(TaskTag::tagId)
                .isEqualTo(tag.id)
        }

    private fun genTask(id: Long = 1L) = Task(
        id = id,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
    )

    private fun createSavedStateHandleForNew(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every {
            savedStateHandle.toRoute<TaskFormRoute.Main>()
        } returns TaskFormRoute.Main.new(name = null)
        return savedStateHandle
    }

    private fun createSavedStateHandleForEdit(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every {
            savedStateHandle.toRoute<TaskFormRoute.Main>()
        } returns TaskFormRoute.Main.edit(id = 1L)
        return savedStateHandle
    }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun createLocale(): Locale = Locale.US

    private fun createTaskFormViewModelForNew(
        initDatabase: (FakeDatabase) -> Unit = {}
    ): TaskFormViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TaskFormViewModel(
            createSavedStateHandleForNew(),
            FakeTaskRepository(db, clock),
            FakeTagRepository(db),
            clock,
            createLocale(),
        )
    }

    private fun createTaskFormViewModelForEdit(
        initDatabase: (FakeDatabase) -> Unit
    ): TaskFormViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TaskFormViewModel(
            createSavedStateHandleForEdit(),
            FakeTaskRepository(db, clock),
            FakeTagRepository(db),
            clock,
            createLocale(),
        )
    }
}
