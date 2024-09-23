package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
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
        val clock = createClock()
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setUserMessage = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutCurrentTask() {
        val clock = createClock()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setUserMessage = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_empty))
            .assertIsDisplayed()
    }

    @Test
    fun displayErrorMessage_withFailureUi() {
        val clock = createClock()
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        every { taskRepository.getCurrentTask(any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setUserMessage = {},
                    onAddTask = {},
                    onNavigateToTask = {},
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
        val clock = createClock()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setUserMessage = {},
                    onAddTask = { onAddTaskCalled = true },
                    onNavigateToTask = {},
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
        val clock = createClock()
        val (task1, task2) = genTasks(2)
        val db = FakeDatabase().apply {
            insertTask(task1)
            insertTask(task2)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setUserMessage = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_mark_done_button))
            .performClick()

        composeTestRule.onNodeWithText(task2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenMarkDoneFailed() {
        var userMessage: String? = null
        val clock = createClock()
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.markDone(any()) } throws RuntimeException("Test")

        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setUserMessage = { userMessage = it },
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_mark_done_button))
            .performClick()
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

    private fun createClock() = Clock.systemDefaultZone()
}
