package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class TaskSearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysSearchResults_withSearchResults() {
        val query = loremFaker.verbs.base()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.unique.words()}",
            )
        }
        val taskRepository = FakeTaskRepository.withTasks(tasks)
        val viewModel = createTaskSearchViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskSearchScreen(
                    query = query,
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutSearchResults() {
        val query = loremFaker.verbs.base()
        val taskRepository = FakeTaskRepository()
        val viewModel = createTaskSearchViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskSearchScreen(
                    query = query,
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_search_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displayErrorMessage_withFailureUi() {
        val query = loremFaker.verbs.base()
        val taskRepository = spyk<FakeTaskRepository>()
        every { taskRepository.searchTaskItems(any()) } returns flowOf(
            PagingData.from(
                emptyList(),
                LoadStates(
                    refresh = LoadState.Error(RuntimeException("Test")),
                    prepend = LoadState.NotLoading(endOfPaginationReached = false),
                    append = LoadState.NotLoading(endOfPaginationReached = false),
                ),
            )
        )

        val viewModel = createTaskSearchViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskSearchScreen(
                    query = query,
                    topBarAction = TaskSearchTopBarAction.Search,
                    topBarActionHandled = {},
                    onAddTask = {},
                    onSelectSearchResult = {},
                    taskSearchViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_search_error))
            .assertIsDisplayed()
    }

    private fun createTaskSearchViewModel(taskRepository: TaskRepository) =
        TaskSearchViewModel(taskRepository, Clock.systemDefaultZone())
}
