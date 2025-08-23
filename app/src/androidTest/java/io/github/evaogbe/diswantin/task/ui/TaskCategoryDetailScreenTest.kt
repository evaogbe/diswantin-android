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
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
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
        val category = genTaskCategory()
        val tasks = genTasks()
        val viewModel = createTaskCategoryDetailViewModel { db ->
            tasks.forEach(db::insertTask)
            db.insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
        }

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

        composeTestRule.onNodeWithText(category.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val viewModel = createTaskCategoryDetailViewModel {}

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
        var onPopBackStackCalled = false
        val category = genTaskCategory()
        val tasks = genTasks()
        val viewModel = createTaskCategoryDetailViewModel { db ->
            tasks.forEach(db::insertTask)
            db.insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryDetailScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
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
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenDeleteCategoryFails() {
        var userMessage: UserMessage? = null
        val category = genTaskCategory()
        val tasks = genTasks()
        val clock = createClock()
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
        coEvery { taskCategoryRepository.delete(any()) } throws RuntimeException("Test")

        val viewModel = TaskCategoryDetailViewModel(
            createSavedStateHandle(),
            taskCategoryRepository,
            taskRepository,
            clock,
        )

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
            userMessage == UserMessage.String(R.string.task_category_detail_delete_error)
        }
    }

    private fun genTaskCategory() = TaskCategory(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks() = generateSequence(
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
    }.take(3).toList()

    private fun createSavedStateHandle() = SavedStateHandle(mapOf("id" to 1L))

    private fun createClock() = Clock.systemDefaultZone()

    private fun createTaskCategoryDetailViewModel(
        initDatabase: (FakeDatabase) -> Unit
    ): TaskCategoryDetailViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TaskCategoryDetailViewModel(
            createSavedStateHandle(),
            FakeTaskCategoryRepository(db),
            FakeTaskRepository(db, clock),
            clock,
        )
    }
}
