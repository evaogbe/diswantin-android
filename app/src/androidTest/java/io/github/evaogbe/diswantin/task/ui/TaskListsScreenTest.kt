package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class TaskListsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    @Test
    fun displaysListNames_withTaskLists() {
        val taskLists = List(3) { TaskList(id = it + 1L, name = loremFaker.lorem.words()) }
        val taskListRepository = FakeTaskListRepository.withTaskLists(taskLists.map {
            TaskListWithTasks(it, emptyList())
        })
        val viewModel = TaskListsViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListsScreen(
                    onAddList = {},
                    onSelectTaskList = {},
                    taskListsViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(taskLists[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskLists[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskLists[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutTaskLists() {
        val taskListRepository = FakeTaskListRepository()
        val viewModel = TaskListsViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListsScreen(
                    onAddList = {},
                    onSelectTaskList = {},
                    taskListsViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_lists_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val taskListRepository = FakeTaskListRepository()
        taskListRepository.setThrows(taskListRepository::taskListsStream, true)

        val viewModel = TaskListsViewModel(taskListRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListsScreen(
                    onAddList = {},
                    onSelectTaskList = {},
                    taskListsViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_lists_fetch_error))
            .assertIsDisplayed()
    }
}
