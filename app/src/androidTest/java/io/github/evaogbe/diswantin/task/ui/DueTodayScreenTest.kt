package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DueTodayScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val faker = Faker()

    private val loremFaker = LoremFaker()

    @Test
    fun displaysTaskNames_withTasks() {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T08:00:00Z"), ZoneId.of("America/New_York"))
        val tasks = List(3) {
            val createdAt = faker.random.randomPastDate().toInstant()
            Task(
                id = it + 1L,
                createdAt = createdAt,
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.unique.words()}",
                deadlineDate = LocalDate.parse("2024-08-23"),
                updatedAt = createdAt,
            )
        }
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = DueTodayViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                DueTodayScreen(onSelectTask = {}, dueTodayViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutTasks() {
        val clock = Clock.systemDefaultZone()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db, clock)
        val viewModel = DueTodayViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                DueTodayScreen(onSelectTask = {}, dueTodayViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.due_tasks_empty)).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val clock = Clock.systemDefaultZone()
        val db = FakeDatabase()
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        every { taskRepository.getTasksDueAt(any(), any()) } returns flowOf(
            PagingData.from(
                emptyList(),
                LoadStates(
                    refresh = LoadState.Error(RuntimeException("Test")),
                    prepend = LoadState.NotLoading(endOfPaginationReached = false),
                    append = LoadState.NotLoading(endOfPaginationReached = false),
                ),
            )
        )

        val viewModel = DueTodayViewModel(taskRepository, clock)

        composeTestRule.setContent {
            DiswantinTheme {
                DueTodayScreen(onSelectTask = {}, dueTodayViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.due_tasks_fetch_error))
            .assertIsDisplayed()
    }
}
