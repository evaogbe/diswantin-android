package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testutils.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class CurrentTaskScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysCurrentTaskName_withCurrentTask() {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = {},
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutCurrentTask() {
        val taskRepository = FakeTaskRepository()
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = {},
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displayErrorMessage_whenUiFailure() {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = CurrentTaskViewModel(taskRepository)

        taskRepository.setThrows(taskRepository::currentTaskStream, true)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = {},
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun callsOnAddTask_whenFabClicked() {
        var onAddTaskCalled = false
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = { onAddTaskCalled = true },
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_task_button))
            .performClick()

        assertThat(onAddTaskCalled).isTrue()
    }

    @Test
    fun callsOnAddTask_whenAddTaskClicked() {
        var onAddTaskCalled = false
        val taskRepository = FakeTaskRepository()
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = { onAddTaskCalled = true },
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_task_button))
            .performClick()

        assertThat(onAddTaskCalled).isTrue()
    }

    @Test
    fun callsOnEditTask_whenEditClicked() {
        var onEditTaskCalled = false
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = {},
                    onEditTask = { id ->
                        assertThat(id).isEqualTo(task.id)
                        onEditTaskCalled = true
                    },
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()

        assertThat(onEditTaskCalled).isTrue()
    }

    @Test
    fun displaysNextTaskName_whenRemoveClicked() {
        val (task1, task2) = genTasks(2)
        val taskRepository = FakeTaskRepository(task1, task2)
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = {},
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(task1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.remove_button)).performClick()

        composeTestRule.onNodeWithText(task2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenRemoveFailed() {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository(task)
        val viewModel = CurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    onNavigateToSearch = {},
                    onAddTask = {},
                    onEditTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        taskRepository.setThrows(taskRepository::remove, true)
        composeTestRule.onNodeWithText(stringResource(R.string.remove_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_remove_error))
            .assertIsDisplayed()
    }

    private fun genTasks(count: Int) = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1)).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        )
    }.take(count).toList()
}
