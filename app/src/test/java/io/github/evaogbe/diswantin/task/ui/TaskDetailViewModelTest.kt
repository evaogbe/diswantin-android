package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testutils.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
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
class TaskDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches task by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask()
        val clock = Clock.systemDefaultZone()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = TaskDetailViewModel(
            SavedStateHandle(mapOf(Destination.TaskDetail.ID_KEY to task.id)),
            taskRepository,
            clock
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = task,
                taskChain = listOf(),
                userMessage = null,
                clock = clock
            )
        )
    }

    @Test
    fun `uiState is failure when repository throws`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask()
        val clock = Clock.systemDefaultZone()
        val taskRepository = FakeTaskRepository(task)
        taskRepository.setThrows(taskRepository::getById, true)

        val viewModel = TaskDetailViewModel(
            SavedStateHandle(mapOf(Destination.TaskDetail.ID_KEY to task.id)),
            taskRepository,
            clock
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure)
    }

    @Test
    fun `removeTask sets uiState to removed`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask()
        val clock = Clock.systemDefaultZone()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = TaskDetailViewModel(
            SavedStateHandle(mapOf(Destination.TaskDetail.ID_KEY to task.id)),
            taskRepository,
            clock
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = task,
                taskChain = listOf(),
                userMessage = null,
                clock = clock
            )
        )

        viewModel.removeTask()

        assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Removed)
    }

    @Test
    fun `removeTask shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val clock = Clock.systemDefaultZone()
            val taskRepository = FakeTaskRepository(task)
            val viewModel = TaskDetailViewModel(
                SavedStateHandle(mapOf(Destination.TaskDetail.ID_KEY to task.id)),
                taskRepository,
                clock
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::remove, true)
            viewModel.removeTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    task = task,
                    taskChain = listOf(),
                    userMessage = R.string.task_detail_delete_error,
                    clock = clock
                )
            )
        }

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )
}
