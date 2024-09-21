package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
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
        val task = genTask().copy(
            deadlineDate = LocalDate.parse("2024-08-23"),
            deadlineTime = LocalTime.parse("17:00"),
        )
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T21:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel =
            TaskDetailViewModel(createSavedStateHandle(), taskRepository, clock, Locale.US)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
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
        composeTestRule.onNodeWithText("Friday, August 23, 2024 at 5:00 PM").assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val taskRepository = FakeTaskRepository()
        taskRepository.setThrows(taskRepository::getTaskDetailById, true)

        val viewModel = createTaskDetailViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
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
    fun popsBackStack_whenTaskDeleted() {
        val task = genTask()
        var onPopBackStackCalled = false
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createTaskDetailViewModel(taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
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
    fun displaysErrorMessage_whenDeleteTaskFailed() {
        var userMessage: String? = null
        val task = genTask()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createTaskDetailViewModel(taskRepository)

        taskRepository.setThrows(taskRepository::delete, true)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskDetailScreen(
                    onPopBackStack = {},
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
            userMessage == stringResource(R.string.task_detail_delete_error)
        }
    }

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createSavedStateHandle() = SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))

    private fun createTaskDetailViewModel(taskRepository: TaskRepository) =
        TaskDetailViewModel(
            createSavedStateHandle(),
            taskRepository,
            Clock.systemDefaultZone(),
            Locale.getDefault(),
        )
}
