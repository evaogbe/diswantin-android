package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.task.data.TaskSearchCriteria
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.mockk.every
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.util.regex.Pattern

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val faker = Faker()

    @Test
    fun `uiState fetches search results matching the query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val blankQuery = faker.string.regexify(""" *""")
            val query = faker.string.regexify("""\S+""")
            val tasks = List(3) {
                Task(
                    id = it + 1L,
                    createdAt = faker.random.randomPastDate().toInstant(),
                    name = faker.string.regexify(
                        """([^\r\n]* )?${Pattern.quote(query)}[^\r\n]*"""
                            .toRegex(RegexOption.IGNORE_CASE)
                    ),
                )
            }
            val taskRepository = FakeTaskRepository.withTasks(tasks)
            val viewModel = createTaskSearchViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Initial)

            viewModel.searchTasks(TaskSearchCriteria(name = blankQuery))

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Initial)

            viewModel.searchTasks(TaskSearchCriteria(name = query))

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskSearchUiState.Success(
                    searchResults = persistentListOf(
                        TaskItemUiState(id = tasks[0].id, name = tasks[0].name, isDone = false),
                        TaskItemUiState(id = tasks[1].id, name = tasks[1].name, isDone = false),
                        TaskItemUiState(id = tasks[2].id, name = tasks[2].name, isDone = false),
                    ),
                )
            )

            viewModel.searchTasks(TaskSearchCriteria(name = blankQuery))

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Initial)
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = faker.string.regexify("""\S+""")
            val exception = RuntimeException("Test")
            val taskRepository = spyk<FakeTaskRepository>()
            every { taskRepository.searchTaskItems(any()) } returns flow { throw exception }

            val viewModel = createTaskSearchViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.searchTasks(TaskSearchCriteria(name = query))

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Failure(exception))
        }

    private fun createTaskSearchViewModel(taskRepository: TaskRepository) =
        TaskSearchViewModel(taskRepository, Clock.systemDefaultZone())
}
