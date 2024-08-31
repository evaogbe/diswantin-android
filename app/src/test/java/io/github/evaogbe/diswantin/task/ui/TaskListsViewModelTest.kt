package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
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

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches task lists`() = runTest(mainDispatcherRule.testDispatcher) {
        val taskLists = List(faker.random.nextInt(bound = 5)) {
            TaskList(id = it + 1L, name = "$it. ${loremFaker.lorem.words()}")
        }
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskLists.map {
            TaskListWithTasks(it, emptyList())
        })
        val viewModel = TaskListsViewModel(taskListRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value)
            .isEqualTo(TaskListsUiState.Success(taskLists = taskLists.toImmutableList()))
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskListRepository = FakeTaskListRepository()
            taskListRepository.setThrows(taskListRepository::taskListsStream, true)

            val viewModel = TaskListsViewModel(taskListRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskListsUiState.Failure)
        }
}
