package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class TaskCategoryListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    @Test
    fun displaysCategoryNames_withCategories() {
        val taskCategories = List(3) {
            TaskCategory(id = it + 1L, name = loremFaker.lorem.unique.words())
        }
        val taskCategoryRepository = FakeTaskCategoryRepository.withCategories(taskCategories.map {
            TaskCategoryWithTasks(it, emptyList())
        })
        val viewModel = TaskCategoryListViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryListScreen(
                    onAddCategory = {},
                    onSelectCategory = {},
                    taskCategoryListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(taskCategories[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskCategories[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(taskCategories[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutCategories() {
        val taskCategoryRepository = FakeTaskCategoryRepository()
        val viewModel = TaskCategoryListViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryListScreen(
                    onAddCategory = {},
                    onSelectCategory = {},
                    taskCategoryListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_list_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val taskCategoryRepository = FakeTaskCategoryRepository()
        taskCategoryRepository.setThrows(taskCategoryRepository::categoryListStream, true)

        val viewModel = TaskCategoryListViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryListScreen(
                    onAddCategory = {},
                    onSelectCategory = {},
                    taskCategoryListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_list_fetch_error))
            .assertIsDisplayed()
    }
}
