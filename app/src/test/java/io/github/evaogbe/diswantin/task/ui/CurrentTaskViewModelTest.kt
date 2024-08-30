package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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
            val (task1, task2) = genTasks(2)
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val taskRepository = FakeTaskRepository(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task1, userMessage = null))

            taskRepository.update(
                EditTaskForm(
                    name = name,
                    deadline = task1.deadline,
                    scheduledAt = task1.scheduledAt,
                    task = task1,
                )
            )

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1.copy(name = name),
                    userMessage = null
                )
            )
        }

    @Test
    fun `uiState is failure when repository throws`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository(task)
        taskRepository.setThrows(taskRepository::getCurrentTask, true)

        val viewModel = createCurrentTaskViewModel(taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(CurrentTaskUiState.Failure)
    }

    @Test
    fun `removeCurrentTask removes current task from repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    userMessage = null
                )
            )

            viewModel.removeCurrentTask()

            assertThat(taskRepository.tasks).doesNotContain(task1)
            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task2, userMessage = null))
        }

    @Test
    fun `removeCurrentTask does nothing when no current task`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskRepository = FakeTaskRepository()
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(CurrentTaskUiState.Empty)

            viewModel.removeCurrentTask()

            assertThat(viewModel.uiState.value).isEqualTo(CurrentTaskUiState.Empty)
        }

    @Test
    fun `removeCurrentTask shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTasks(1).single()
            val taskRepository = FakeTaskRepository(task)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentTaskUiState.Present(currentTask = task, userMessage = null))

            taskRepository.setThrows(taskRepository::remove, true)
            viewModel.removeCurrentTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task,
                    userMessage = R.string.current_task_remove_error
                )
            )
            assertThat(taskRepository.tasks).contains(task)
        }

    private fun genTasks(count: Int) = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1)).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        )
    }.take(count).toList()

    private fun createCurrentTaskViewModel(taskRepository: FakeTaskRepository) =
        CurrentTaskViewModel(taskRepository, Clock.systemDefaultZone())
}
