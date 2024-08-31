package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
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
            val tasks = List(faker.random.nextInt(bound = 5)) {
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
            val viewModel = TaskSearchViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Initial)

            viewModel.searchTasks(blankQuery)

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Initial)

            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value)
                .isEqualTo(TaskSearchUiState.Success(searchResults = tasks.toPersistentList()))

            viewModel.searchTasks(blankQuery)

            assertThat(viewModel.uiState.value)
                .isEqualTo(TaskSearchUiState.Success(searchResults = persistentListOf()))
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = faker.string.regexify("""\S+""")
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskSearchViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::search, true)
            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState.Failure)
        }
}
