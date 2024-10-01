package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
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
    fun hidesAddTaskButton_when20TasksAdded() {
        val tasks = List(20) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.unique.base()} ${loremFaker.lorem.words()}",
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
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true,
        )
            .onParent()
            .performTextInput(tasks[0].name.dropLast(1))

        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).performClick()

        tasks.drop(1).forEach { task ->
            composeTestRule.onNodeWithTag(TaskCategoryFormLayoutTestTag)
                .performScrollToNode(hasText(stringResource(R.string.add_task_button)))
            composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()
            composeTestRule.onNodeWithText(
                stringResource(R.string.task_name_label),
                useUnmergedTree = true,
            )
                .onParent()
                .performTextInput(task.name.dropLast(1))

            composeTestRule.waitUntilExactlyOneExists(hasText(task.name))
            composeTestRule.onNodeWithText(task.name).performClick()
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true,
        ).assertDoesNotExist()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
        every { taskCategoryRepository.getCategoryWithTasksById(any()) } returns flow {
            throw RuntimeException("Test")
        }

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
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_fetch_error))
            .assertIsDisplayed()
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
                    topBarAction = null,
                    topBarActionHandled = {},
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
    fun displaysErrorMessage_whenSearchTasksFails() {
        var userMessage: UserMessage? = null
        val query = loremFaker.verbs.base()
        val db = FakeDatabase()
        val taskRepository = spyk(FakeTaskRepository(db))
        every { taskRepository.search(any()) } returns flow { throw RuntimeException("Test") }

        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
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

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.search_task_options_error)
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
                    topBarAction = null,
                    topBarActionHandled = {},
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
            Timber.d("task: %s", task)
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
        var userMessage: UserMessage? = null
        val name = loremFaker.lorem.words()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
        coEvery { taskCategoryRepository.create(any()) } throws RuntimeException("Test")

        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskCategoryFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_save_error_new))
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        viewModel.saveCategory()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_category_form_save_error_new)
        }
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
                    topBarAction = null,
                    topBarActionHandled = {},
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
        var userMessage: UserMessage? = null
        val name = loremFaker.lorem.words()
        val category = genTaskCategory()
        val db = FakeDatabase().apply {
            insertTaskCategory(category, emptySet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
        coEvery { taskCategoryRepository.update(any()) } throws RuntimeException("Test")

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
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectTaskType = {},
                    taskCategoryFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_category_form_save_error_edit))
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        viewModel.saveCategory()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_category_form_save_error_edit)
        }
    }

    private fun genTaskCategory() = TaskCategory(id = 1L, name = loremFaker.lorem.words())

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))
}
