package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
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
                    setCurrentTaskId = {},
                    setUserMessage = {},
                    onAddTask = {},
                    onAdviceClick = {},
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
                    setCurrentTaskId = {},
                    setUserMessage = {},
                    onAddTask = {},
                    onAdviceClick = {},
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
        val viewModel = createCurrentTaskViewModel(taskRepository)

        taskRepository.setThrows(taskRepository::getCurrentTask, true)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setCurrentTaskId = {},
                    setUserMessage = {},
                    onAddTask = {},
                    onAdviceClick = {},
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
                    setCurrentTaskId = {},
                    setUserMessage = {},
                    onAddTask = { onAddTaskCalled = true },
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button))
            .performClick()

        assertThat(onAddTaskCalled).isTrue()
    }

    @Test
    fun displaysNextTaskName_whenMarkDoneClicked() {
        val (task1, task2) = genTasks(2)
        val taskRepository = FakeTaskRepository.withTasks(task1, task2)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setCurrentTaskId = {},
                    setUserMessage = {},
                    onAddTask = {},
                    onAdviceClick = {},
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
        var setUserMessageCalled = false
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setCurrentTaskId = {},
                    setUserMessage = {
                        assertThat(it)
                            .isEqualTo(stringResource(R.string.current_task_mark_done_error))
                        setUserMessageCalled = true
                    },
                    onAddTask = {},
                    onAdviceClick = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        taskRepository.setThrows(taskRepository::markDone, true)
        composeTestRule.onNodeWithText(stringResource(R.string.mark_done_button)).performClick()
        composeTestRule.waitForIdle()

        assertThat(setUserMessageCalled).isTrue()
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
