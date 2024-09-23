package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
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
    }

    @Test
    fun `uiState emits failure when task not found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db, clock)
            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure)
        }

    @Test
    fun `uiState emits failure when fetch task detail throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val db = FakeDatabase()
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getTaskDetailById(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure)
        }

    @Test
    fun `uiState emits failure when fetch task recurrences throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db, clock))
            every { taskRepository.getTaskRecurrencesByTaskId(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel =
                TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure)
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
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
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
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
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
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
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
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        doneAt = null,
                        categoryId = null,
                        categoryName = null,
                        parentId = null,
                        parentName = null,
                    ),
                    recurrence = null,
                    userMessage = R.string.task_detail_mark_done_error,
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
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        doneAt = Instant.parse("2024-08-22T08:00:00Z"),
                        categoryId = null,
                        categoryName = null,
                        parentId = null,
                        parentName = null,
                    ),
                    recurrence = null,
                    userMessage = R.string.task_detail_unmark_done_error,
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
                    userMessage = R.string.task_detail_delete_error,
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

    private fun createClock() = Clock.systemDefaultZone()

    private fun Task.toTaskDetail() = TaskDetail(
        id = id,
        name = name,
        deadlineDate = deadlineDate,
        deadlineTime = deadlineTime,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        doneAt = null,
        categoryId = null,
        categoryName = null,
        parentId = null,
        parentName = null,
    )
}
