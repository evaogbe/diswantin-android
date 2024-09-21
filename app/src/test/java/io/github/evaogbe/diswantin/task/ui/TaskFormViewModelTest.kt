package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
    fun `initializes for new without taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isTrue()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
            assertThat(viewModel.nameInput).isEmpty()
        }

    @Test
    fun `initializes for edit with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val task = genTask().copy(
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = LocalTime.parse("17:00"),
            )
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                        week = 4,
                    )
                )
            }
            val taskRepository = FakeTaskRepository(db)
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("17:00"),
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = TaskRecurrenceUiState(
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = locale,
                    ),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
            assertThat(viewModel.nameInput).isEqualTo(task.name)
        }

    @Test
    fun `uiState emits failure when fetch existing task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            taskRepository.setThrows(taskRepository::getById, true)

            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure)
        }

    @Test
    fun `uiState emits failure when fetch existing task recurrences fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            taskRepository.setThrows(taskRepository::getTaskRecurrencesByTaskId, true)

            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure)
        }

    @Test
    fun `uiState emits failure when fetch existing task parent fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            taskRepository.setThrows(taskRepository::getParent, true)

            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure)
        }

    @Test
    fun `uiState shows parent task field when taskId is null and repository has tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val taskRepository = FakeTaskRepository.withTasks(genTask())
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState shows parent task field when taskId is present and repository has other tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val taskRepository = FakeTaskRepository.withTasks(genTask(), genTask(id = 2L))
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides parent task field when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val taskRepository = FakeTaskRepository.withTasks(genTask())
            taskRepository.setThrows(taskRepository::getCount, true)

            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `searchTasks fetches parentTaskOptions`() = runTest(mainDispatcherRule.testDispatcher) {
        val clock = createClock()
        val locale = Locale.US
        val query = loremFaker.verbs.base()
        val tasks = List(faker.random.nextInt(min = 1, max = 5)) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        }
        val taskRepository = FakeTaskRepository.withTasks(tasks)
        val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchTasks(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                deadlineDate = null,
                deadlineTime = null,
                scheduledDate = null,
                scheduledTime = null,
                recurrence = null,
                showParentTaskField = true,
                parentTask = null,
                parentTaskOptions = tasks.toPersistentList(),
                hasSaveError = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val query = loremFaker.verbs.base()
            val taskRepository = FakeTaskRepository.withTasks(genTask())
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::search, true)
            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = R.string.search_task_options_error,
                )
            )
        }

    @Test
    fun `saveTask creates task without taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val locale = Locale.US
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineDate(LocalDate.parse("2024-08-22"))
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

            val task = taskRepository.tasks.single()
            assertThat(task).isEqualToIgnoringGivenProperties(
                Task(
                    createdAt = Instant.parse("2024-08-22T08:00:00Z"),
                    name = name,
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("17:00"),
                ),
                Task::id,
            )
            assertThat(taskRepository.getTaskRecurrencesByTaskId(task.id).first().single())
                .isEqualToIgnoringGivenProperties(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence::id,
                )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val locale = Locale.US
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::create, true)
            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `saveTask updates task with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val locale = Locale.US
            val task = genTask().copy(
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = LocalTime.parse("21:00"),
            )
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("21:00"),
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineDate(null)
            viewModel.updateDeadlineTime(null)
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
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = LocalDate.parse("2024-08-22"),
                    scheduledTime = LocalTime.parse("17:00"),
                )
            )
            assertThat(
                taskRepository.getTaskRecurrencesByTaskId(task.id).first()
                    .map { it.copy(id = 0) }
            ).containsExactly(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Week,
                    step = 2,
                    week = 4,
                ),
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-26"),
                    type = RecurrenceType.Week,
                    step = 2,
                    week = 5,
                ),
            )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val locale = Locale.US
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock, locale)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::update, true)
            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun genTask(id: Long = 1L) = Task(
        id = id,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
    )

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))

}
