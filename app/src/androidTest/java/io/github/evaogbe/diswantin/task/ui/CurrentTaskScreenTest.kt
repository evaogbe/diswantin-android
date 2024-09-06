package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalTestApi::class)
class CurrentTaskScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysCurrentTaskName_withCurrentTask() {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutCurrentTask() {
        val taskRepository = FakeTaskRepository()
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displayErrorMessage_withFailureUi() {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository.withTasks(task)
        taskRepository.setThrows(taskRepository::getCurrentTask, true)

        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun callsOnAddTask_whenAddTaskClicked() {
        var onAddTaskCalled = false
        val taskRepository = FakeTaskRepository()
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = { onAddTaskCalled = true },
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button))
            .performClick()

        assertThat(onAddTaskCalled).isTrue()
    }

    @Test
    fun displaysMatchingTaskOptions_whenTaskSearchedFor() {
        val query = loremFaker.verbs.base()
        val tasks = generateSequence(
            Task(
                id = 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        ) {
            Task(
                id = it.id + 1L,
                createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1))
                    .toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        }.take(3).toList()
        val taskRepository = FakeTaskRepository.withTasks(tasks)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[1].name))
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchTasksFailed() {
        val query = loremFaker.verbs.base()
        val taskRepository = FakeTaskRepository.withTasks(genTasks(2))
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        taskRepository.setThrows(taskRepository::search, true)
        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(
            hasText(stringResource(R.string.search_task_options_error))
        )
    }

    @Test
    fun displaysNextTaskName_whenParentTaskAdded() {
        val (task1, task2) = genTasks(2)
        val taskRepository = FakeTaskRepository.withTasks(task1, task2)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.skip_dialog_title))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(task2.name.substring(0, 1))
        composeTestRule.waitUntilExactlyOneExists(hasText(task2.name))
        composeTestRule.onNodeWithText(task2.name).performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.confirm_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.skip_dialog_title))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(task1.name).assertDoesNotExist()
        composeTestRule.onNodeWithText(task2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenAddParentTaskFailed() {
        var userMessage: String? = null
        val (task1, task2) = genTasks(2)
        val taskRepository = FakeTaskRepository.withTasks(task1, task2)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = { userMessage = it },
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        taskRepository.setThrows(taskRepository::addParent, true)
        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(task2.name.substring(0, 1))
        composeTestRule.waitUntilExactlyOneExists(hasText(task2.name))
        composeTestRule.onNodeWithText(task2.name).performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.confirm_button)).performClick()

        composeTestRule.waitUntil {
            userMessage == stringResource(R.string.current_task_add_parent_error)
        }
    }

    @Test
    fun displaysNextTaskName_whenMarkDoneClicked() {
        val (task1, task2) = genTasks(2)
        val taskRepository = FakeTaskRepository.withTasks(task1, task2)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = {},
                    onAddTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.mark_done_button)).performClick()

        composeTestRule.onNodeWithText(task2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenMarkDoneFailed() {
        var userMessage: String? = null
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    setUserMessage = { userMessage = it },
                    onAddTask = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        taskRepository.setThrows(taskRepository::markDone, true)
        composeTestRule.onNodeWithText(stringResource(R.string.mark_done_button)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil {
            userMessage == stringResource(R.string.current_task_mark_done_error)
        }
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

    private fun createCurrentTaskViewModel(taskRepository: FakeTaskRepository) =
        CurrentTaskViewModel(taskRepository, Clock.systemDefaultZone())
}
