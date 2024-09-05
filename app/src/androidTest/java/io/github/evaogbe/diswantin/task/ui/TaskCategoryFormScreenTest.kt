package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

@OptIn(ExperimentalTestApi::class)
class TaskCategoryFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        taskCategoryRepository.setThrows(taskCategoryRepository::getCategoryWithTasksById, true)

        val viewModel = TaskCategoryFormViewModel(
            createSavedStateHandleForEdit(),
            taskCategoryRepository,
            taskRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_fetch_error))
    }

    @Test
    fun displaysMatchingTaskOptions_whenTaskSearchedFor() {
        val query = loremFaker.verbs.base()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.unique.words()}",
            )
        }
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchTasksFailed() {
        var userMessage: String? = null
        val query = loremFaker.verbs.base()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    setUserMessage = { userMessage = it },
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        taskRepository.setThrows(taskRepository::search, true)
        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntil {
            userMessage == stringResource(R.string.task_category_form_search_tasks_error)
        }
    }

    @Test
    fun popsBackStack_whenCategoryCreated() {
        var onPopBackStackCalled = false
        val name = loremFaker.lorem.words()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.unique.base()} ${loremFaker.lorem.words()}"
            )
        }
        Timber.d("tasks: %s", tasks)
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)

        tasks.forEach { task ->
            composeTestRule.onNodeWithText(
                stringResource(R.string.task_name_label),
                useUnmergedTree = true
            )
                .onParent()
                .performTextInput(task.name.substring(0, 1))
            composeTestRule.waitUntilExactlyOneExists(hasText(task.name))
            composeTestRule.onNodeWithText(task.name).performClick()

            composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()
        }

        viewModel.saveCategory()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        val name = loremFaker.lorem.words()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_save_error_new))
            .assertDoesNotExist()

        taskCategoryRepository.setThrows(taskCategoryRepository::create, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        viewModel.saveCategory()

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_save_error_new))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenCategoryUpdated() {
        var onPopBackStackCalled = false
        val name = loremFaker.lorem.words()
        val category = genTaskCategory()
        val db = FakeDatabase().apply {
            insertTaskCategory(category, emptySet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = TaskCategoryFormViewModel(
            createSavedStateHandleForEdit(),
            taskCategoryRepository,
            taskRepository,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        viewModel.saveCategory()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        val name = loremFaker.lorem.words()
        val category = genTaskCategory()
        val db = FakeDatabase().apply {
            insertTaskCategory(category, emptySet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = TaskCategoryFormViewModel(
            createSavedStateHandleForEdit(),
            taskCategoryRepository,
            taskRepository,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_save_error_edit))
            .assertDoesNotExist()

        taskCategoryRepository.setThrows(taskCategoryRepository::update, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        viewModel.saveCategory()

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_save_error_edit))
            .assertIsDisplayed()
    }

    private fun genTaskCategory() = TaskCategory(id = 1L, name = loremFaker.lorem.words())

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))
}
