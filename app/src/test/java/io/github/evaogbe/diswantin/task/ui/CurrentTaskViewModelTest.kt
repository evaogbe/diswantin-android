package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
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
                    currentTask = task1,
                    userMessage = null,
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
                    userMessage = null,
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
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    userMessage = null,
                )
            )

            viewModel.markCurrentTaskDone()

            assertThat(taskRepository.getTaskDetailById(task1.id).first())
                .isNotNull()
                .prop(TaskDetail::doneAt)
                .isNotNull()
            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task2,
                    userMessage = null,
                )
            )
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
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(
                    CurrentTaskUiState.Present(
                        currentTask = task,
                        userMessage = null,
                    )
                )

            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task,
                    userMessage = R.string.current_task_mark_done_error,
                )
            )
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

    private fun createClock() = Clock.systemDefaultZone()
}
