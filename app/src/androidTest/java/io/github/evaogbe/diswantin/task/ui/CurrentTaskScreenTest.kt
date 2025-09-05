package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.matchesSnackbar
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
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
        val task = genTasks().first()
        val viewModel = createCurrentTaskViewModel({ db ->
            db.insertTask(task)
        })

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
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
        val viewModel = createCurrentTaskViewModel({})

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
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
        val task = genTasks().first()
        val viewModel = createCurrentTaskViewModel(
            initDatabase = { db -> db.insertTask(task) },
            initTaskRepositorySpy = { repository ->
                every { repository.getCurrentTask(any()) } returns flow {
                    throw RuntimeException("Test")
                }
            },
        )

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
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
        val viewModel = createCurrentTaskViewModel({})

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onNavigateToAdvice = {},
                    onAddTask = { onAddTaskCalled = true },
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()

        assertThat(onAddTaskCalled).isTrue()
    }

    @Test
    fun displaysNextTaskName_whenClickSkip() {
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val (task1, task2) = genTasks().take(2).toList()
        val db = FakeDatabase().apply {
            insertTask(task1)
            insertTask(task2)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task1.id,
                    startDate = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
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
                    showSnackbar = {},
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.skip_sheet_confirm_button))
            .performClick()

        composeTestRule.onNodeWithText(task2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSkipFails() {
        var snackbarState: SnackbarState? = null
        val clock =
            Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
        val task = genTasks().first()
        val db = FakeDatabase().apply {
            insertTask(task)
            insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    startDate = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
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
                    showSnackbar = { snackbarState = it },
                    onNavigateToAdvice = {},
                    onAddTask = {},
                    onNavigateToTask = {},
                    currentTaskViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.skip_sheet_confirm_button))
            .performClick()

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
        assertThat(snackbarState).isNotNull()
            .matchesSnackbar(stringResource(R.string.current_task_skip_error))
    }

    @Test
    fun displaysNextTaskName_whenClickMarkDone() {
        val (task1, task2) = genTasks().take(2).toList()
        val viewModel = createCurrentTaskViewModel({ db ->
            db.insertTask(task1)
            db.insertTask(task2)
        })

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
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
        var snackbarState: SnackbarState? = null
        val task = genTasks().first()
        val viewModel = createCurrentTaskViewModel(
            initDatabase = { db -> db.insertTask(task) },
            initTaskRepositorySpy = { repository ->
                coEvery { repository.markDone(any()) } throws RuntimeException("Test")
            },
        )

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentTaskScreen(
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = { snackbarState = it },
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
            snackbarState?.matches(stringResource(R.string.current_task_mark_done_error)) == true
        }
    }

    private fun genTasks(): Sequence<Task> {
        val initialCreatedAt = faker.random.randomPastDate().toInstant()
        return generateSequence(
            Task(
                id = 1L,
                createdAt = initialCreatedAt,
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                updatedAt = initialCreatedAt,
            )
        ) {
            val nextCreatedAt =
                faker.random.randomPastDate(min = it.createdAt.plusMillis(1)).toInstant()
            Task(
                id = it.id + 1L,
                createdAt = nextCreatedAt,
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                updatedAt = nextCreatedAt,
            )
        }
    }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun createCurrentTaskViewModel(
        initDatabase: (FakeDatabase) -> Unit,
        initTaskRepositorySpy: ((TaskRepository) -> Unit)? = null,
    ): CurrentTaskViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = if (initTaskRepositorySpy == null) {
            FakeTaskRepository(db, clock)
        } else {
            spyk(FakeTaskRepository(db, clock)).also(initTaskRepositorySpy)
        }
        return CurrentTaskViewModel(taskRepository, clock)
    }
}
