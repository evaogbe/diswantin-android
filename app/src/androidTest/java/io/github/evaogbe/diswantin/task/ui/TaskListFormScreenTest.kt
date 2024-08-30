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
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class TaskListFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

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
        val taskRepository = FakeTaskRepository(tasks)
        val taskListRepository = FakeTaskListRepository()
        val viewModel = TaskListFormViewModel(taskListRepository, taskRepository)

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

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(tasks[0].name.substring(0, 1))
        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(tasks[1].name.substring(0, 1))
        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[1].name))
        composeTestRule.onNodeWithText(tasks[1].name).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(tasks[2].name.substring(0, 1))
        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[2].name))
        composeTestRule.onNodeWithText(tasks[2].name).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveError() {
        val name = loremFaker.lorem.words()
        val taskRepository = FakeTaskRepository()
        val taskListRepository = FakeTaskListRepository()
        val viewModel = TaskListFormViewModel(taskListRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskListFormScreen(onPopBackStack = {}, taskListFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_save_error))
            .assertDoesNotExist()

        taskListRepository.setThrows(taskListRepository::create, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.task_list_form_save_error))
            .assertIsDisplayed()
    }
}
