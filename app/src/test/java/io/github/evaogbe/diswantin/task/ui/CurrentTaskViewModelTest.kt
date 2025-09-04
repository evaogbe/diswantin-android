package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
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

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task1.id,
                    name = task1.name,
                    note = task1.note,
                    isRefreshing = false,
                    canSkip = false,
                )
            )

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
                    tagIds = emptySet(),
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingTagIds = emptySet(),
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task1.id,
                    name = name,
                    note = task1.note,
                    isRefreshing = false,
                    canSkip = false,
                )
            )
        }

    @Test
    fun `uiState emits failure when fetch current task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTasks(1).single()
            val viewModel = createCurrentTaskViewModel(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getCurrentTask(any()) } returns flow {
                        throw RuntimeException("Test")
                    }
                },
            )

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
                    startDate = LocalDate.parse("2024-08-22"),
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

        assertThat(viewModel.uiState.value).isEqualTo(
            CurrentTaskUiState.Present(
                id = task.id,
                name = task.name,
                note = task.note,
                isRefreshing = false,
                canSkip = true,
            )
        )
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
                        startDate = LocalDate.parse("2024-08-22"),
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

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task1.id,
                    name = task1.name,
                    note = task1.note,
                    isRefreshing = false,
                    canSkip = true,
                )
            )

            viewModel.skipCurrentTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task2.id,
                    name = task2.name,
                    note = task2.note,
                    isRefreshing = false,
                    canSkip = false,
                )
            )
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
                        startDate = LocalDate.parse("2024-08-22"),
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

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    isRefreshing = false,
                    canSkip = true,
                )
            )
            assertThat(viewModel.userMessage.value).isEqualTo(CurrentTaskUserMessage.SkipError)
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

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task1.id,
                    name = task1.name,
                    note = task1.note,
                    isRefreshing = false,
                    canSkip = false,
                )
            )

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task2.id,
                    name = task2.name,
                    note = task2.note,
                    isRefreshing = false,
                    canSkip = false,
                )
            )
            assertThat(viewModel.userMessage.value).isNull()
            assertThat(taskRepository.getTaskDetailById(task1.id).first()).isNotNull()
                .prop(TaskDetail::doneAt).isNotNull()
        }

    @Test
    fun `markCurrentTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTasks(1).single()
            val viewModel = createCurrentTaskViewModel(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    coEvery { repository.markDone(any()) } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
                viewModel.userMessage.collect()
            }

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    isRefreshing = false,
                    canSkip = false,
                )
            )
            assertThat(viewModel.userMessage.value).isEqualTo(CurrentTaskUserMessage.MarkDoneError)
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

    private fun createCurrentTaskViewModel(
        initDatabase: (FakeDatabase) -> Unit,
        initTaskRepositorySpy: (TaskRepository) -> Unit,
    ): CurrentTaskViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        initTaskRepositorySpy(taskRepository)
        return CurrentTaskViewModel(taskRepository, clock)
    }
}
