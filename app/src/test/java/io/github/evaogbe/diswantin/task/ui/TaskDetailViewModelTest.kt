package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches task by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val (task1, task2) = List(2) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            )
        }
        val clock = createClock()
        val locale = createLocale()
        val db = FakeDatabase().apply {
            insertTask(task1)
            insertTask(task2)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task1.id,
                    startDate = LocalDate.parse("2024-08-22"),
                    endDate = LocalDate.parse("2025-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                ),
            )
            insertChain(parentId = task1.id, childId = task2.id)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            tagRepository,
            clock,
            locale,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.childTaskPagingData.asSnapshot()).containsExactly(
            TaskSummaryUiState(id = task2.id, name = task2.name, isDone = false)
        )
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                id = task1.id,
                name = task1.name,
                note = task1.note,
                formattedDeadline = null,
                formattedStartAfter = null,
                formattedScheduledAt = null,
                recurrence = TaskRecurrenceUiState(
                    startDate = LocalDate.parse("2024-08-22"),
                    endDate = LocalDate.parse("2025-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    weekdays = persistentSetOf(),
                    locale = locale,
                ),
                isDone = false,
                parent = null,
                tags = persistentListOf(),
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState emits failure when task not found`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createTaskDetailViewModel({})

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isInstanceOf<TaskDetailUiState.Failure>()
            .prop(TaskDetailUiState.Failure::exception).isInstanceOf<NullPointerException>()
    }

    @Test
    fun `uiState emits failure when fetch task detail throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val viewModel = createTaskDetailViewModel(
                initDatabase = { db -> db.insertTask(genTask()) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getTaskDetailById(any()) } returns flow { throw exception }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure(exception))
        }

    @Test
    fun `uiState emits failure when fetch task recurrences throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val viewModel = createTaskDetailViewModel(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    every { repository.getTaskRecurrencesByTaskId(any()) } returns flow {
                        throw exception
                    }
                })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure(exception))
        }

    @Test
    fun `uiState emits failure when fetch tags throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val viewModel = createTaskDetailViewModel(
                initDatabase = { db -> db.insertTask(task) },
                initTagRepositorySpy = { repository ->
                    every { repository.getTagsByTaskId(any(), any()) } returns flow {
                        throw exception
                    }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure(exception))
        }

    @Test
    fun `can toggle task done`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask()
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                id = task.id,
                name = task.name,
                note = task.note,
                formattedDeadline = null,
                formattedStartAfter = null,
                formattedScheduledAt = null,
                recurrence = null,
                isDone = false,
                parent = null,
                tags = persistentListOf(),
                userMessage = null,
            )
        )

        viewModel.markTaskDone()

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                id = task.id,
                name = task.name,
                note = task.note,
                formattedDeadline = null,
                formattedStartAfter = null,
                formattedScheduledAt = null,
                recurrence = null,
                isDone = true,
                parent = null,
                tags = persistentListOf(),
                userMessage = null,
            )
        )

        viewModel.unmarkTaskDone()

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                id = task.id,
                name = task.name,
                note = task.note,
                formattedDeadline = null,
                formattedStartAfter = null,
                formattedScheduledAt = null,
                recurrence = null,
                isDone = false,
                parent = null,
                tags = persistentListOf(),
                userMessage = null,
            )
        )
    }

    @Test
    fun `markTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskDetailViewModel(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    coEvery { repository.markDone(any()) } throws RuntimeException("Test")
                })

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.markTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    formattedDeadline = null,
                    formattedStartAfter = null,
                    formattedScheduledAt = null,
                    recurrence = null,
                    isDone = false,
                    parent = null,
                    tags = persistentListOf(),
                    userMessage = TaskDetailUserMessage.MarkDoneError,
                )
            )
        }

    @Test
    fun `unmarkTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            taskRepository.markDone(task.id)
            coEvery { taskRepository.unmarkDone(any()) } throws RuntimeException("Test")

            val tagRepository = FakeTagRepository(db)
            val viewModel = TaskDetailViewModel(
                createSavedStateHandle(),
                taskRepository,
                tagRepository,
                clock,
                createLocale(),
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.unmarkTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    formattedDeadline = null,
                    formattedStartAfter = null,
                    formattedScheduledAt = null,
                    recurrence = null,
                    isDone = true,
                    parent = null,
                    tags = persistentListOf(),
                    userMessage = TaskDetailUserMessage.UnmarkDoneError,
                )
            )
        }

    @Test
    fun `deleteTask sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask()
        val viewModel = createTaskDetailViewModel({ db ->
            db.insertTask(task)
        })

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                id = task.id,
                name = task.name,
                note = task.note,
                formattedDeadline = null,
                formattedStartAfter = null,
                formattedScheduledAt = null,
                recurrence = null,
                isDone = false,
                parent = null,
                tags = persistentListOf(),
                userMessage = null,
            )
        )

        viewModel.deleteTask()

        assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Deleted)
    }

    @Test
    fun `deleteTask shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val viewModel = createTaskDetailViewModel(
                initDatabase = { db -> db.insertTask(task) },
                initTaskRepositorySpy = { repository ->
                    coEvery { repository.delete(any()) } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.deleteTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    formattedDeadline = null,
                    formattedStartAfter = null,
                    formattedScheduledAt = null,
                    recurrence = null,
                    isDone = false,
                    parent = null,
                    tags = persistentListOf(),
                    userMessage = TaskDetailUserMessage.DeleteError,
                )
            )
        }

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createSavedStateHandle(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every { savedStateHandle.toRoute<TaskDetailRoute>() } returns TaskDetailRoute(id = 1L)
        return savedStateHandle
    }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun createLocale(): Locale = Locale.US

    private fun createTaskDetailViewModel(
        initDatabase: (FakeDatabase) -> Unit,
        initTaskRepositorySpy: ((TaskRepository) -> Unit)? = null,
        initTagRepositorySpy: ((TagRepository) -> Unit)? = null,
    ): TaskDetailViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = if (initTaskRepositorySpy == null) {
            FakeTaskRepository(db, clock)
        } else {
            spyk(FakeTaskRepository(db, clock)).also(initTaskRepositorySpy)
        }
        val tagRepository = if (initTagRepositorySpy == null) {
            FakeTagRepository(db)
        } else {
            spyk(FakeTagRepository(db)).also(initTagRepositorySpy)
        }
        return TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )
    }
}
