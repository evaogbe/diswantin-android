package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.spyk
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class TaskCategoryDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysCategoryNameWithTasks() {
        val categoryWithTasks = genTaskCategoryWithTasks()
        val taskCategoryRepository = FakeTaskCategoryRepository.withCategories(categoryWithTasks)
        val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryDetailScreen(
                    onPopBackStack = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTask = {},
                    taskCategoryDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(categoryWithTasks.category.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(categoryWithTasks.tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(categoryWithTasks.tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(categoryWithTasks.tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val taskCategoryRepository = FakeTaskCategoryRepository()
        val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryDetailScreen(
                    onPopBackStack = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTask = {},
                    taskCategoryDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_detail_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenCategoryDeleted() {
        var onPopBackStackClicked = false
        val categoryWithTasks = genTaskCategoryWithTasks()
        val taskCategoryRepository = FakeTaskCategoryRepository.withCategories(categoryWithTasks)
        val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryDetailScreen(
                    onPopBackStack = { onPopBackStackClicked = true },
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTask = {},
                    taskCategoryDetailViewModel = viewModel,
                )
            }
        }

        viewModel.deleteCategory()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackClicked).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenDeleteCategoryFailed() {
        var userMessage: String? = null
        val categoryWithTasks = genTaskCategoryWithTasks()
        val taskCategoryRepository =
            spyk(FakeTaskCategoryRepository.withCategories(categoryWithTasks))
        coEvery { taskCategoryRepository.delete(any()) } throws RuntimeException("Test")

        val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryDetailScreen(
                    onPopBackStack = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectTask = {},
                    taskCategoryDetailViewModel = viewModel,
                )
            }
        }

        viewModel.deleteCategory()

        composeTestRule.waitUntil {
            userMessage == stringResource(R.string.task_category_detail_delete_error)
        }
    }

    private fun genTaskCategoryWithTasks() = TaskCategoryWithTasks(
        TaskCategory(id = 1L, name = loremFaker.lorem.words()),
        generateSequence(
            Task(
                id = 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                categoryId = 1L,
            )
        ) {
            Task(
                id = it.id + 1L,
                createdAt = faker.random.randomPastDate(min = it.createdAt).toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                categoryId = 1L,
            )
        }.take(3).toList(),
    )

    private fun createTaskCategoryDetailViewModel(
        taskCategoryRepository: FakeTaskCategoryRepository,
    ) =
        TaskCategoryDetailViewModel(
            SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L)),
            taskCategoryRepository,
            Clock.systemDefaultZone(),
        )
}
