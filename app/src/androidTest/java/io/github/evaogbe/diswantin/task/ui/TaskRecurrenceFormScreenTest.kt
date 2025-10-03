package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.FixedClockMonitor
import io.github.evaogbe.diswantin.ui.preferences.LocalClock
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime

class TaskRecurrenceFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysDefaultRecurrence_whenNew() {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val clockMonitor = FixedClockMonitor(now)
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForNew(),
            taskRepository,
            tagRepository,
            clockMonitor,
        ).apply { initialize() }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalClock provides clock) {
                DiswantinTheme {
                    TaskRecurrentFormScreen(
                        topBarAction = null,
                        topBarActionHandled = {},
                        taskFormViewModel = viewModel,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Aug 23, 2024").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("day").assertIsDisplayed()
    }

    @Test
    fun displaysExistingRecurrence_whenEdit() {
        val task = genTask()
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(task)
            db.insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    startDate = LocalDate.parse("2024-07-22"),
                    type = RecurrenceType.DayOfMonth,
                    step = 2
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskRecurrentFormScreen(
                    topBarAction = null,
                    topBarActionHandled = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText("Jul 22, 2024").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("months").assertIsDisplayed()
    }

    private fun genTask(id: Long = 1L): Task {
        val createdAt = faker.random.randomPastDate().toInstant()
        return Task(
            id = id,
            createdAt = createdAt,
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            updatedAt = createdAt,
        )
    }

    private fun createSavedStateHandleForNew() =
        SavedStateHandle(mapOf("id" to null, "name" to null))

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf("id" to 1L, "name" to null))

    private fun createClockMonitor() = FixedClockMonitor()

    private fun createTaskFormViewModelForEdit(
        initDatabase: (FakeDatabase) -> Unit
    ): TaskFormViewModel {
        val clockMonitor = createClockMonitor()
        val db = FakeDatabase().also(initDatabase)
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = FakeTagRepository(db)
        return TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clockMonitor,
        ).apply { initialize() }
    }
}
