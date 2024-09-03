package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class TaskListDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysTaskListNameWithTasks() {
        val taskListWithTasks = genTaskListWithTasks()
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskListWithTasks)
        val viewModel = createTaskListDetailViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListDetailScreen(
                    onPopBackStack = {},
                    onEditTaskList = {},
                    onSelectTask = {},
                    taskListDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(taskListWithTasks.taskList.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskListWithTasks.tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskListWithTasks.tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskListWithTasks.tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val taskListRepository = FakeTaskListRepository()
        val viewModel = createTaskListDetailViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListDetailScreen(
                    onPopBackStack = {},
                    onEditTaskList = {},
                    onSelectTask = {},
                    taskListDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_detail_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenTaskListDeleted() {
        var onPopBackStackClicked = false
        val taskListWithTasks = genTaskListWithTasks()
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskListWithTasks)
        val viewModel = createTaskListDetailViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListDetailScreen(
                    onPopBackStack = { onPopBackStackClicked = true },
                    onEditTaskList = {},
                    onSelectTask = {},
                    taskListDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackClicked).isTrue()
    }

    @Test
    fun showsErrorMessage_whenDeleteTaskListFailed() {
        val taskListWithTasks = genTaskListWithTasks()
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskListWithTasks)
        val viewModel = createTaskListDetailViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListDetailScreen(
                    onPopBackStack = {},
                    onEditTaskList = {},
                    onSelectTask = {},
                    taskListDetailViewModel = viewModel,
                )
            }
        }

        taskListRepository.setThrows(taskListRepository::delete, true)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_detail_delete_error))
            .assertIsDisplayed()
    }

    private fun genTaskListWithTasks() = TaskListWithTasks(
        TaskList(id = 1L, name = loremFaker.lorem.words()),
        List(3) {
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
            SavedStateHandle(mapOf(Destination.ID_KEY to 1L)),
            taskListRepository,
            Clock.systemDefaultZone(),
        )
}
