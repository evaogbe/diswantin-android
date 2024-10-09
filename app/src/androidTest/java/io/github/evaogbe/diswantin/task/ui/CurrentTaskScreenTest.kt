package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToAdvice = {},
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
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToAdvice = {},
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
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToAdvice = {},
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
    fun callsOnAddTask_whenClickAddTask() {
        var onAddTaskCalled = false
        val clock = createClock()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToAdvice = {},
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
    fun displaysNextTaskName_whenClickSkip() {
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val (task1, task2) = genTasks(2)
        val db = FakeDatabase().apply {
            insertTask(task1)
            insertTask(task2)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task1.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    week = 4,
                )
            )
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = CurrentTaskTopBarAction.Skip,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.skip_dialog_confirm_button))
            .performClick()

        composeTestRule.onNodeWithText(task2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSkipFails() {
        var userMessage: UserMessage? = null
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    week = 4,
                )
            )
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.skip(any()) } throws RuntimeException("Test")

        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = CurrentTaskTopBarAction.Skip,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.skip_dialog_confirm_button))
            .performClick()

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
        assertThat(userMessage).isEqualTo(UserMessage.String(R.string.current_task_skip_error))
    }

    @Test
    fun displaysNextTaskName_whenClickMarkDone() {
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
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToAdvice = {},
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
    fun displaysErrorMessage_whenMarkDoneFails() {
        var userMessage: UserMessage? = null
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
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_mark_done_button))
            .performClick()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.current_task_mark_done_error)
        }
    }

    @Test
    fun displaysCelebrationMessage_whenCompletionCountMultipleOf20() {
        var userMessage: UserMessage? = null
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-01-01"),
                    type = RecurrenceType.Day,
                    step = 1,
                    week = 1,
                )
            )
            repeat(19) {
                insertTaskCompletion(
                    TaskCompletion(
                        taskId = task.id,
                        // 1704157200 = 2024-01-02T00:00:00Z
                        doneAt = Instant.ofEpochMilli(1704153600L + 86400000L * it)
                    )
                )
            }
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_mark_done_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_empty))
            .assertIsDisplayed()
        assertThat(userMessage)
            .isEqualTo(UserMessage.Plural(R.plurals.completed_tasks_celebration_message, 20))
    }

    @Test
    fun displaysErrorMessage_whenFetchCompletionCountFails() {
        var userMessage: UserMessage? = null
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTasks(1).single()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-01-01"),
                    type = RecurrenceType.Day,
                    step = 1,
                    week = 1,
                )
            )
            repeat(19) {
                insertTaskCompletion(
                    TaskCompletion(
                        taskId = task.id,
                        // 1704157200 = 2024-01-02T00:00:00Z
                        doneAt = Instant.ofEpochMilli(1704153600L + 86400000L * it)
                    )
                )
            }
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        every { taskRepository.getCompletionCount() } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = CurrentTaskViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_mark_done_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.current_task_empty))
            .assertIsDisplayed()
        assertThat(userMessage)
            .isEqualTo(UserMessage.String(R.string.current_task_fetch_completion_error))
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

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
}
