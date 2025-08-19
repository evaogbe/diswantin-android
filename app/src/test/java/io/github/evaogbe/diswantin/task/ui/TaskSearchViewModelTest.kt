package io.github.evaogbe.diswantin.task.ui

import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.util.regex.Pattern

@OptIn(ExperimentalCoroutinesApi::class)
class TaskSearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val faker = Faker()

    @Test
    fun `uiState fetches search results matching the query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val blankQuery = faker.string.regexify(""" *""")
            val query = faker.string.regexify("""\S+""")
            val namePattern =
                """([^\r\n]* )?${Pattern.quote(query)}[^\r\n]*""".toRegex(RegexOption.IGNORE_CASE)
            val tasks = List(3) {
                Task(
                    id = it + 1L,
                    createdAt = faker.random.randomPastDate().toInstant(),
                    name = faker.string.regexify(namePattern),
                )
            }
            val taskRepository = FakeTaskRepository.withTasks(tasks)
            val viewModel = createTaskSearchViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState(hasCriteria = false))

            viewModel.searchTasks(
                name = blankQuery,
                deadlineDateRange = null,
                startAfterDateRange = null,
                scheduledDateRange = null,
                doneDateRange = null,
                recurrenceDate = null,
            )

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState(hasCriteria = false))

            viewModel.searchTasks(
                name = query,
                deadlineDateRange = null,
                startAfterDateRange = null,
                scheduledDateRange = null,
                doneDateRange = null,
                recurrenceDate = null,
            )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState(hasCriteria = true))
            assertThat(viewModel.searchResultPagingData.asSnapshot()).containsExactly(
                TaskItemUiState(id = tasks[0].id, name = tasks[0].name, isDone = false),
                TaskItemUiState(id = tasks[1].id, name = tasks[1].name, isDone = false),
                TaskItemUiState(id = tasks[2].id, name = tasks[2].name, isDone = false),
            )

            viewModel.searchTasks(
                name = blankQuery,
                deadlineDateRange = null,
                startAfterDateRange = null,
                scheduledDateRange = null,
                doneDateRange = null,
                recurrenceDate = null,
            )
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(TaskSearchUiState(hasCriteria = false))
        }

    private fun createTaskSearchViewModel(taskRepository: TaskRepository) =
        TaskSearchViewModel(taskRepository, Clock.systemDefaultZone())
}
