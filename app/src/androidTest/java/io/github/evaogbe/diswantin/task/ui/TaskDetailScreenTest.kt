package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

class TaskDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysTask() {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T21:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTask().copy(
            deadlineDate = LocalDate.parse("2024-08-23"),
            deadlineTime = LocalTime.parse("17:00"),
        )
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel =
            TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Friday, August 23, 2024 at 5:00 PM") or
                    hasText("Friday, August 23, 2024 at 5:00 PM")
        ).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val clock = createClock()
        val db = FakeDatabase()
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        every { taskRepository.getTaskDetailById(any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_detail_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun togglesTaskDone() {
        var topBarState: TaskDetailTopBarState? = null
        val clock = createClock()
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = { topBarState = it },
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        assertThat(topBarState).isEqualTo(TaskDetailTopBarState(taskId = task.id, isDone = false))

        viewModel.markTaskDone()

        composeTestRule.waitUntil {
            topBarState == TaskDetailTopBarState(taskId = task.id, isDone = true)
        }

        viewModel.unmarkTaskDone()

        composeTestRule.waitUntil {
            topBarState == TaskDetailTopBarState(taskId = task.id, isDone = false)
        }
    }

    @Test
    fun displaysErrorMessage_whenMarkTaskDoneFails() {
        var userMessage: UserMessage? = null
        val clock = createClock()
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.markDone(any()) } throws RuntimeException("Test")

        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        viewModel.markTaskDone()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_detail_mark_done_error)
        }
    }

    @Test
    fun displaysCelebrationMessage_whenCompletionCountMultipleOf20() {
        var userMessage: UserMessage? = null
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-01-01"),
                    type = RecurrenceType.Day,
                    step = 1,
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
        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        viewModel.markTaskDone()

        composeTestRule.waitUntil {
            userMessage == UserMessage.Plural(R.plurals.completed_tasks_celebration_message, 20)
        }
    }

    @Test
    fun displaysErrorMessage_whenFetchCompletionCountFails() {
        var userMessage: UserMessage? = null
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-01-01"),
                    type = RecurrenceType.Day,
                    step = 1,
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

        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        viewModel.markTaskDone()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_detail_fetch_completion_error)
        }
    }

    @Test
    fun displaysErrorMessage_whenUnmarkTaskDoneFails() {
        var userMessage: UserMessage? = null
        val clock = createClock()
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.unmarkDone(any()) } throws RuntimeException("Test")

        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )
        viewModel.markTaskDone()

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        viewModel.unmarkTaskDone()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_detail_unmark_done_error)
        }
    }

    @Test
    fun popsBackStack_whenTaskDeleted() {
        var onPopBackStackCalled = false
        val clock = createClock()
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        viewModel.deleteTask()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenDeleteTaskFails() {
        var userMessage: UserMessage? = null
        val clock = createClock()
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.delete(any()) } throws RuntimeException("Test")

        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            clock,
            Locale.US,
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onNavigateToTask = {},
                    onNavigateToCategory = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        viewModel.deleteTask()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_detail_delete_error)
        }
    }

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createSavedStateHandle() = SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
}
