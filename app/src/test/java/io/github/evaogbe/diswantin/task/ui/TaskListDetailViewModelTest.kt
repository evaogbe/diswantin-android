package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetch task list by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val taskListWithTasks = genTaskListWithTasks()
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskListWithTasks)
        val viewModel = createTaskListDetailViewModel(taskListRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskListDetailUiState.Success(
                taskList = taskListWithTasks.taskList,
                tasks = taskListWithTasks.tasks.map { it.toTaskItemState() }.toImmutableList(),
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState emits failure when task list not found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskListRepository = FakeTaskListRepository()
            val viewModel = createTaskListDetailViewModel(taskListRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskListDetailUiState.Failure)
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskListRepository = FakeTaskListRepository()
            taskListRepository.setThrows(taskListRepository::getTaskListWithTaskItemsById, true)

            val viewModel = createTaskListDetailViewModel(taskListRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskListDetailUiState.Failure)
        }

    @Test
    fun `deleteTaskList sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val taskListWithTasks = genTaskListWithTasks()
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskListWithTasks)
        val viewModel = createTaskListDetailViewModel(taskListRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskListDetailUiState.Success(
                taskList = taskListWithTasks.taskList,
                tasks = taskListWithTasks.tasks.map { it.toTaskItemState() }.toImmutableList(),
                userMessage = null,
            )
        )

        viewModel.deleteTaskList()

        assertThat(taskListRepository.taskLists).isEmpty()
        assertThat(viewModel.uiState.value).isEqualTo(TaskListDetailUiState.Deleted)
    }

    @Test
    fun `deleteTaskList shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskListWithTasks = genTaskListWithTasks()
            val taskListRepository = FakeTaskListRepository.withTaskLists(taskListWithTasks)
            val viewModel = createTaskListDetailViewModel(taskListRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskListRepository.setThrows(taskListRepository::delete, true)
            viewModel.deleteTaskList()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListDetailUiState.Success(
                    taskList = taskListWithTasks.taskList,
                    tasks = taskListWithTasks.tasks.map { it.toTaskItemState() }.toImmutableList(),
                    userMessage = R.string.task_list_detail_delete_error,
                )
            )
        }

    private fun Task.toTaskItemState() = TaskItemState(
        id = id,
        name = name,
        recurring = recurring,
        isDone = false,
    )

    private fun genTaskListWithTasks() = TaskListWithTasks(
        TaskList(id = 1L, name = loremFaker.lorem.words()),
        List(faker.random.nextInt(bound = 5)) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                listId = 1L,
            )
        },
    )

    private fun createTaskListDetailViewModel(taskListRepository: FakeTaskListRepository) =
        TaskListDetailViewModel(
            SavedStateHandle(mapOf(Destination.TaskListDetail.ID_KEY to 1L)),
            taskListRepository,
            Clock.systemDefaultZone(),
        )
}
