package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.loadstate.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class TagDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysTagNameWithTasks() {
        val tag = genTag()
        val tasks = genTasks()
        val viewModel = createTagDetailViewModel { db ->
            tasks.forEach(db::insertTask)
            db.insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TagDetailScreen(
                    onPopBackStack = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTask = {},
                    tagDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(tag.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val viewModel = createTagDetailViewModel {}

        composeTestRule.setContent {
            DiswantinTheme {
                TagDetailScreen(
                    onPopBackStack = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectTask = {},
                    tagDetailViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.tag_detail_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenTagDeleted() {
        var onPopBackStackCalled = false
        val topBarActionState = MutableStateFlow<TagDetailTopBarAction?>(null)
        val tag = genTag()
        val tasks = genTasks()
        val viewModel = createTagDetailViewModel { db ->
            tasks.forEach(db::insertTask)
            db.insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
        }

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TagDetailScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    setUserMessage = {},
                    onSelectTask = {},
                    tagDetailViewModel = viewModel,
                )
            }
        }

        topBarActionState.value = TagDetailTopBarAction.Delete

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenDeleteTagFails() {
        var userMessage: UserMessage? = null
        val topBarActionState = MutableStateFlow<TagDetailTopBarAction?>(null)
        val tag = genTag()
        val tasks = genTasks()
        val clock = createClock()
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = spyk(FakeTagRepository(db))
        coEvery { tagRepository.delete(any()) } throws RuntimeException("Test")

        val viewModel = TagDetailViewModel(
            createSavedStateHandle(),
            tagRepository,
            taskRepository,
            clock,
        )

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TagDetailScreen(
                    onPopBackStack = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    setUserMessage = { userMessage = it },
                    onSelectTask = {},
                    tagDetailViewModel = viewModel,
                )
            }
        }

        topBarActionState.value = TagDetailTopBarAction.Delete

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.tag_detail_delete_error)
        }
    }

    private fun genTag() = Tag(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks() = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    }.take(3).toList()

    private fun createSavedStateHandle() = SavedStateHandle(mapOf("id" to 1L))

    private fun createClock() = Clock.systemDefaultZone()

    private fun createTagDetailViewModel(initDatabase: (FakeDatabase) -> Unit): TagDetailViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TagDetailViewModel(
            createSavedStateHandle(),
            FakeTagRepository(db),
            FakeTaskRepository(db, clock),
            clock,
        )
    }
}
