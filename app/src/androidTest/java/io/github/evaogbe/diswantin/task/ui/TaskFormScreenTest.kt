package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class TaskFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysNewTaskTopBar_whenNew() {
        val taskRepository = FakeTaskRepository()
        val viewModel = createTaskFormViewModelForNew(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(onPopBackStack = {}, taskFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_title_new))
            .assertIsDisplayed()
    }

    @Test
    fun displaysEditTaskTopBar_whenEdit() {
        val task = genTask()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createTaskFormViewModelForEdit(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(onPopBackStack = {}, taskFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_title_edit))
            .assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val taskRepository = FakeTaskRepository()
        val viewModel = createTaskFormViewModelForEdit(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(onPopBackStack = {}, taskFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenTaskCreated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var onPopBackStackCalled = false
        val taskRepository = FakeTaskRepository()
        val viewModel = createTaskFormViewModelForNew(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    taskFormViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.ok_button)).performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val taskRepository = FakeTaskRepository()
        val viewModel = createTaskFormViewModelForNew(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(onPopBackStack = {}, taskFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_save_error_new))
            .assertDoesNotExist()

        taskRepository.setThrows(taskRepository::create, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_save_error_new))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenTaskUpdated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var onPopBackStackCalled = false
        val task = genTask().copy(deadline = faker.random.randomFutureDate().toInstant())
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createTaskFormViewModelForEdit(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    taskFormViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.deadline_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_at_label))
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.clear_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.ok_button)).performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val task = genTask()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createTaskFormViewModelForEdit(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(onPopBackStack = {}, taskFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_save_error_edit))
            .assertDoesNotExist()

        taskRepository.setThrows("update", true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_save_error_edit))
            .assertIsDisplayed()
    }

    private fun genTask(id: Long = 1L) = Task(
        id = id,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createTaskFormViewModelForNew(taskRepository: TaskRepository) =
        TaskFormViewModel(SavedStateHandle(), taskRepository, Clock.systemDefaultZone())

    private fun createTaskFormViewModelForEdit(taskRepository: TaskRepository) =
        TaskFormViewModel(
            SavedStateHandle(mapOf(Destination.EditTaskForm.ID_KEY to 1L)),
            taskRepository,
            Clock.systemDefaultZone()
        )
}
