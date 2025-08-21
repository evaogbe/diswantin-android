package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
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
        val clock = createClock()
        val locale = Locale.US
        val (task1, task2) = List(2) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            )
        }
        val db = FakeDatabase().apply {
            insertTask(task1)
            insertTask(task2)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task1.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                ),
            )
            insertChain(parentId = task1.id, childId = task2.id)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, locale)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.childTaskPagingData.asSnapshot()).containsExactly(
            TaskItemUiState(id = task2.id, name = task2.name, isDone = false)
        )
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = task1.toTaskDetail(),
                recurrence = TaskRecurrenceUiState(
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    weekdays = persistentSetOf(),
                    locale = locale,
                ),
                userMessage = null,
                clock = clock,
            )
        )
    }

    @Test
    fun `uiState emits failure when task not found`() = runTest(mainDispatcherRule.testDispatcher) {
        val clock = createClock()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel =
            TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isInstanceOf<TaskDetailUiState.Failure>()
            .prop(TaskDetailUiState.Failure::exception).isInstanceOf<NullPointerException>()
    }

    @Test
    fun `uiState emits failure when fetch task detail throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val exception = RuntimeException("Test")
            val db = FakeDatabase().apply {
                insertTask(genTask())
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getTaskDetailById(any()) } returns flow { throw exception }

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure(exception))
        }

    @Test
    fun `uiState emits failure when fetch task recurrences throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val exception = RuntimeException("Test")
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getTaskRecurrencesByTaskId(any()) } returns flow {
                throw exception
            }

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure(exception))
        }

    @Test
    fun `can toggle task done`() = runTest(mainDispatcherRule.testDispatcher) {
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel =
            TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = TaskDetail(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    doneAt = null,
                    categoryId = null,
                    categoryName = null,
                    parentId = null,
                    parentName = null,
                ),
                recurrence = null,
                userMessage = null,
                clock = clock,
            )
        )

        viewModel.markTaskDone()

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = TaskDetail(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    doneAt = Instant.parse("2024-08-22T08:00:00Z"),
                    categoryId = null,
                    categoryName = null,
                    parentId = null,
                    parentName = null,
                ),
                recurrence = null,
                userMessage = null,
                clock = clock,
            )
        )

        viewModel.unmarkTaskDone()

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = TaskDetail(
                    id = task.id,
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    doneAt = null,
                    categoryId = null,
                    categoryName = null,
                    parentId = null,
                    parentName = null,
                ),
                recurrence = null,
                userMessage = null,
                clock = clock,
            )
        )
    }

    @Test
    fun `markTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            coEvery { taskRepository.markDone(any()) } throws RuntimeException("Test")

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.markTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    task = TaskDetail(
                        id = task.id,
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        doneAt = null,
                        categoryId = null,
                        categoryName = null,
                        parentId = null,
                        parentName = null,
                    ),
                    recurrence = null,
                    userMessage = UserMessage.String(R.string.task_detail_mark_done_error),
                    clock = clock,
                )
            )
        }

    @Test
    fun `unmarkTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            taskRepository.markDone(task.id)
            coEvery { taskRepository.unmarkDone(any()) } throws RuntimeException("Test")

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.unmarkTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    task = TaskDetail(
                        id = task.id,
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        doneAt = Instant.parse("2024-08-22T08:00:00Z"),
                        categoryId = null,
                        categoryName = null,
                        parentId = null,
                        parentName = null,
                    ),
                    recurrence = null,
                    userMessage = UserMessage.String(R.string.task_detail_unmark_done_error),
                    clock = clock,
                )
            )
        }

    @Test
    fun `deleteTask sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val clock = createClock()
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel =
            TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = task.toTaskDetail(),
                recurrence = null,
                userMessage = null,
                clock = clock,
            )
        )

        viewModel.deleteTask()

        assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Deleted)
    }

    @Test
    fun `deleteTask shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            coEvery { taskRepository.delete(any()) } throws RuntimeException("Test")

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.deleteTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    task = task.toTaskDetail(),
                    recurrence = null,
                    userMessage = UserMessage.String(R.string.task_detail_delete_error),
                    clock = clock,
                )
            )
        }

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createSavedStateHandle() = SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun Task.toTaskDetail() = TaskDetail(
        id = id,
        name = name,
        note = note,
        deadlineDate = deadlineDate,
        deadlineTime = deadlineTime,
        startAfterDate = startAfterDate,
        startAfterTime = startAfterTime,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        doneAt = null,
        categoryId = null,
        categoryName = null,
        parentId = null,
        parentName = null,
    )
}
