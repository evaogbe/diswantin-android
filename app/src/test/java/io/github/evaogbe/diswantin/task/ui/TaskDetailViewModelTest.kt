package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
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
        val clock = createClock()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = task.toTaskDetail(),
                userMessage = null,
                clock = clock,
            )
        )
    }

    @Test
    fun `uiState emits failure when task not found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure)
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val taskRepository = FakeTaskRepository()
            taskRepository.setThrows(taskRepository::getTaskDetailById, true)

            val viewModel = TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskDetailUiState.Failure)
        }

    @Test
    fun `deleteTask sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTask()
        val clock = createClock()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskDetailUiState.Success(
                task = task.toTaskDetail(),
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
            val task = genTask()
            val clock = createClock()
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel = TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::delete, true)
            viewModel.deleteTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskDetailUiState.Success(
                    task = task.toTaskDetail(),
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
        scheduledAt = scheduledAt,
        recurring = recurring,
        doneAt = null,
        listId = null,
        listName = null,
    )
}
