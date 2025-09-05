package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableStateFlow
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
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            tagRepository,
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
                    showSnackbar = {},
                    onNavigateToTask = {},
                    onNavigateToTag = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(task.name).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("Friday, August 23, 2024 at 5:00 PM") or hasText("Friday, August 23, 2024 at 5:00 PM")
        ).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val viewModel = createTaskDetailViewModel(
            initDatabase = {},
            initTaskRepositorySpy = { repository ->
                every { repository.getTaskDetailById(any()) } returns flow {
                    throw RuntimeException("Test")
                }
            },
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    showSnackbar = {},
                    onNavigateToTask = {},
                    onNavigateToTag = {},
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
        val topBarActionState = MutableStateFlow<TaskDetailTopBarAction?>(null)
        val task = genTask()
        val viewModel = createTaskDetailViewModel({ db ->
            db.insertTask(task)
        })

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = { topBarState = it },
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    showSnackbar = {},
                    onNavigateToTask = {},
                    onNavigateToTag = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        assertThat(topBarState).isEqualTo(TaskDetailTopBarState(taskId = task.id, isDone = false))

        topBarActionState.value = TaskDetailTopBarAction.MarkDone

        composeTestRule.waitUntil {
            topBarState == TaskDetailTopBarState(taskId = task.id, isDone = true)
        }

        topBarActionState.value = TaskDetailTopBarAction.UnmarkDone

        composeTestRule.waitUntil {
            topBarState == TaskDetailTopBarState(taskId = task.id, isDone = false)
        }
    }

    @Test
    fun displaysErrorMessage_whenMarkTaskDoneFails() {
        var snackbarState: SnackbarState? = null
        val topBarActionState = MutableStateFlow<TaskDetailTopBarAction?>(null)
        val task = genTask()
        val viewModel = createTaskDetailViewModel(
            initDatabase = { db -> db.insertTask(task) },
            initTaskRepositorySpy = { repository ->
                coEvery { repository.markDone(any()) } throws RuntimeException("Test")
            },
        )

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    showSnackbar = { snackbarState = it },
                    onNavigateToTask = {},
                    onNavigateToTag = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        topBarActionState.value = TaskDetailTopBarAction.MarkDone

        composeTestRule.waitUntil {
            snackbarState?.matches(stringResource(R.string.task_detail_mark_done_error)) == true
        }
    }

    @Test
    fun displaysErrorMessage_whenUnmarkTaskDoneFails() {
        var snackbarState: SnackbarState? = null
        val topBarActionState = MutableStateFlow<TaskDetailTopBarAction?>(null)
        val task = genTask()
        val viewModel = createTaskDetailViewModel(
            initDatabase = { db -> db.insertTask(task) },
            initTaskRepositorySpy = { repository ->
                coEvery { repository.unmarkDone(any()) } throws RuntimeException("Test")
            },
        )
        viewModel.markTaskDone()

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    showSnackbar = { snackbarState = it },
                    onNavigateToTask = {},
                    onNavigateToTag = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        topBarActionState.value = TaskDetailTopBarAction.UnmarkDone

        composeTestRule.waitUntil {
            snackbarState?.matches(stringResource(R.string.task_detail_unmark_done_error)) == true
        }
    }

    @Test
    fun popsBackStack_whenTaskDeleted() {
        var onPopBackStackCalled = false
        val topBarActionState = MutableStateFlow<TaskDetailTopBarAction?>(null)
        val task = genTask()
        val viewModel = createTaskDetailViewModel({ db ->
            db.insertTask(task)
        })

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    showSnackbar = {},
                    onNavigateToTask = {},
                    onNavigateToTag = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        topBarActionState.value = TaskDetailTopBarAction.Delete

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenDeleteTaskFails() {
        var snackbarState: SnackbarState? = null
        val topBarActionState = MutableStateFlow<TaskDetailTopBarAction?>(null)
        val task = genTask()
        val viewModel = createTaskDetailViewModel(
            initDatabase = { db -> db.insertTask(task) },
            initTaskRepositorySpy = { repository ->
                coEvery { repository.delete(any()) } throws RuntimeException("Test")
            },
        )

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    showSnackbar = { snackbarState = it },
                    onNavigateToTask = {},
                    onNavigateToTag = {},
                    taskDetailViewModel = viewModel,
                )
            }
        }

        topBarActionState.value = TaskDetailTopBarAction.Delete

        composeTestRule.waitUntil {
            snackbarState?.matches(stringResource(R.string.task_detail_delete_error)) == true
        }
    }

    private fun genTask(): Task {
        val createdAt = faker.random.randomPastDate().toInstant()
        return Task(
            id = 1L,
            createdAt = createdAt,
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            updatedAt = createdAt,
        )
    }

    private fun createSavedStateHandle() = SavedStateHandle(mapOf("id" to 1L))

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun createLocale() = Locale.US

    private fun createTaskDetailViewModel(
        initDatabase: (FakeDatabase) -> Unit,
        initTaskRepositorySpy: ((TaskRepository) -> Unit)? = null,
    ): TaskDetailViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = if (initTaskRepositorySpy == null) {
            FakeTaskRepository(db, clock)
        } else {
            spyk(FakeTaskRepository(db, clock)).also(initTaskRepositorySpy)
        }
        val tagRepository = FakeTagRepository(db)
        return TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )
    }
}
