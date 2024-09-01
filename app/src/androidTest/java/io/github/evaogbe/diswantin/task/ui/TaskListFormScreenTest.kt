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
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import timber.log.Timber

class TaskListFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        taskListRepository.setThrows(taskListRepository::getTaskListWithTasksById, true)

        val viewModel = TaskListFormViewModel(
            SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to 1L)),
            taskListRepository,
            taskRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListFormScreen(onPopBackStack = {}, taskListFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_fetch_error))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun popsBackStack_whenTaskListCreated() {
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
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel =
            TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    taskListFormViewModel = viewModel
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

        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        val name = loremFaker.lorem.words()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel =
            TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListFormScreen(onPopBackStack = {}, taskListFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_save_error_new))
            .assertDoesNotExist()

        taskListRepository.setThrows(taskListRepository::create, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_save_error_new))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenTaskListUpdated() {
        var onPopBackStackCalled = false
        val name = loremFaker.lorem.words()
        val taskList = TaskList(id = 1L, name = loremFaker.lorem.words())
        val db = FakeDatabase().apply {
            insertTaskList(taskList, emptyList())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel = TaskListFormViewModel(
            SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to taskList.id)),
            taskListRepository,
            taskRepository,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    taskListFormViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        val name = loremFaker.lorem.words()
        val taskList = TaskList(id = 1L, name = loremFaker.lorem.words())
        val db = FakeDatabase().apply {
            insertTaskList(taskList, emptyList())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel = TaskListFormViewModel(
            SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to taskList.id)),
            taskListRepository,
            taskRepository,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListFormScreen(onPopBackStack = {}, taskListFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_save_error_edit))
            .assertDoesNotExist()

        taskListRepository.setThrows(taskListRepository::update, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_save_error_edit))
            .assertIsDisplayed()
    }
}
