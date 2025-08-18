package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentTaskViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches current task from repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val (task1, task2) = genTasks(2)
            val db = FakeDatabase().apply {
                insertTask(task1)
                insertTask(task2)
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task1, canSkip = false))

            taskRepository.update(
                EditTaskForm(
                    name = name,
                    note = "",
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = task1.deadlineTime,
                    startAfterDate = task1.startAfterDate,
                    startAfterTime = task1.startAfterTime,
                    scheduledDate = task1.scheduledDate,
                    scheduledTime = task1.scheduledTime,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1.copy(name = name),
                    canSkip = false,
                )
            )
        }

    @Test
    fun `uiState emits failure when fetch current task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTasks(1).single()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getCurrentTask(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isInstanceOf<CurrentTaskUiState.Failure>()
        }

    @Test
    fun `uiState can skip when recurring task`() = runTest(mainDispatcherRule.testDispatcher) {
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                )
            )
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value)
            .isEqualTo(CurrentTaskUiState.Present(currentTask = task, canSkip = true))
    }

    @Test
    fun `uiState cannot skip when fetch task recurrences fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val task = genTasks(1).single()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    )
                )
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getTaskRecurrencesByTaskId(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task, canSkip = false))
            assertThat(viewModel.userMessage.value)
                .isEqualTo(UserMessage.String(R.string.current_task_fetch_recurrences_error))
        }

    @Test
    fun `skipCurrentTask replaces current task with next task`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val (task1, task2) = genTasks(2)
            val db = FakeDatabase().apply {
                insertTask(task1)
                insertTask(task2)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task1.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    )
                )
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task1, canSkip = true))

            viewModel.skipCurrentTask()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task2, canSkip = false))
        }

    @Test
    fun `skipCurrentTask shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val task = genTasks(1).single()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    )
                )
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            coEvery { taskRepository.skip(any()) } throws RuntimeException("Test")

            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            viewModel.skipCurrentTask()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task, canSkip = true))
            assertThat(viewModel.userMessage.value)
                .isEqualTo(UserMessage.String(R.string.current_task_skip_error))
        }

    @Test
    fun `markCurrentTaskDone sets current task doneAt`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val (task1, task2) = genTasks(2)
            val db = FakeDatabase().apply {
                insertTask(task1)
                insertTask(task2)
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task1, canSkip = false))

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task2, canSkip = false))
            assertThat(viewModel.userMessage.value).isNull()
            assertThat(taskRepository.getTaskDetailById(task1.id).first())
                .isNotNull()
                .prop(TaskDetail::doneAt)
                .isNotNull()
        }

    @Test
    fun `markCurrentTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTasks(1).single()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))

            coEvery { taskRepository.markDone(any()) } throws RuntimeException("Test")

            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task, canSkip = false))
            assertThat(viewModel.userMessage.value)
                .isEqualTo(UserMessage.String(R.string.current_task_mark_done_error))
        }

    @Test
    fun `markCurrentTaskDone shows celebration message when multiple of 20 completed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val task = genTasks(1).single()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-01-01"),
                        type = RecurrenceType.Day,
                        step = 1,
                    )
                )
                repeat(19) {
                    insertTaskCompletion(
                        TaskCompletion(
                            taskId = task.id,
                            // 1704157200 = 2024-01-02T00:00:00Z
                            doneAt = Instant.ofEpochMilli(1704153600L + 86400000L * it)
                        )
                    )
                }
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Empty)
            assertThat(viewModel.userMessage.value)
                .isEqualTo(UserMessage.Plural(R.plurals.completed_tasks_celebration_message, 20))
        }

    @Test
    fun `markCurrentTaskDone shows error message when fetch completion count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val task = genTasks(1).single()
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-01-01"),
                        type = RecurrenceType.Day,
                        step = 1,
                    )
                )
                repeat(19) {
                    insertTaskCompletion(
                        TaskCompletion(
                            taskId = task.id,
                            // 1704157200 = 2024-01-02T00:00:00Z
                            doneAt = Instant.ofEpochMilli(1704153600L + 86400000L * it)
                        )
                    )
                }
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getCompletionCount() } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = CurrentTaskViewModel(taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Empty)
            assertThat(viewModel.userMessage.value)
                .isEqualTo(UserMessage.String(R.string.current_task_fetch_completion_error))
        }

    private fun genTasks(count: Int) = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1)).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    }.take(count).toList()

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
}
