package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
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
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TagFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun hidesAddTaskButton_when20TasksAdded() {
        val tasks = List(20) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.unique.base()} ${loremFaker.lorem.words()}",
            )
        }
        val viewModel = createTagFormViewModelForNew { db ->
            tasks.forEach(db::insertTask)
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true,
        ).onParent().performTextInput(tasks[0].name.substring(0, 1))

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).performClick()

        tasks.drop(1).forEach { task ->
            composeTestRule.onNodeWithTag(TagFormLayoutTestTag)
                .performScrollToNode(hasText(stringResource(R.string.add_task_button)))
            composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()
            composeTestRule.onNodeWithText(
                stringResource(R.string.task_name_label),
                useUnmergedTree = true,
            ).onParent().performTextInput(task.name.substring(0, 1))

            composeTestRule.waitForIdle()
            composeTestRule.waitUntilExactlyOneExists(hasText(task.name))
            composeTestRule.onNodeWithText(task.name).performClick()
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label),
            useUnmergedTree = true,
        ).assertDoesNotExist()

        composeTestRule.onNodeWithTag(TagFormLayoutTestTag)
            .performScrollToNode(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).performTouchInput { swipeLeft() }

        composeTestRule.onNodeWithTag(TagFormLayoutTestTag)
            .performScrollToNode(hasText(stringResource(R.string.add_task_button)))
        composeTestRule.onNodeWithText(stringResource(R.string.add_task_button))
            .assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = spyk(FakeTagRepository(db))
        every { tagRepository.getById(any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = createTagFormViewModelForEdit(tagRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.tag_form_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun displaysMatchingTaskOptions_whenTaskSearchedFor() {
        val query = loremFaker.verbs.base()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.unique.words()}",
            )
        }
        val viewModel = createTagFormViewModelForNew { db ->
            tasks.forEach(db::insertTask)
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label), useUnmergedTree = true
        ).onParent().performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchTasksFails() {
        var userMessage: UserMessage? = null
        val query = loremFaker.verbs.base()
        val db = FakeDatabase()
        val taskRepository = spyk(FakeTaskRepository(db))
        every { taskRepository.searchTaggedTasks(any(), any(), any()) } returns flow {
            throw RuntimeException("Test")
        }

        val tagRepository = FakeTagRepository(db)
        val viewModel = createTagFormViewModelForNew(tagRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_name_label), useUnmergedTree = true
        ).onParent().performTextInput(query)

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.search_task_options_error)
        }
    }

    @Test
    fun popsBackStack_whenTagCreated() {
        var onPopBackStackCalled = false
        val name = loremFaker.lorem.words()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.unique.base()} ${loremFaker.lorem.words()}"
            )
        }
        val viewModel = createTagFormViewModelForNew { db ->
            tasks.forEach(db::insertTask)
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextInput(name)

        tasks.forEach { task ->
            composeTestRule.onNodeWithText(
                stringResource(R.string.task_name_label), useUnmergedTree = true
            ).onParent().performTextInput(task.name.substring(0, 1))
            composeTestRule.waitForIdle()
            composeTestRule.waitUntilExactlyOneExists(hasText(task.name))
            composeTestRule.onNodeWithText(task.name).performClick()

            composeTestRule.onNodeWithText(stringResource(R.string.add_task_button)).performClick()
        }

        viewModel.saveTag()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        var userMessage: UserMessage? = null
        val name = loremFaker.lorem.words()
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = spyk(FakeTagRepository(db))
        coEvery { tagRepository.create(any()) } throws RuntimeException("Test")

        val viewModel =
            createTagFormViewModelForNew(tagRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextInput(name)
        viewModel.saveTag()

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.tag_form_save_error_new)
        }
    }

    @Test
    fun popsBackStack_whenTagUpdated() {
        var onPopBackStackCalled = false
        val name = loremFaker.lorem.words()
        val tag = genTag()
        val viewModel = createTagFormViewModelForEdit { db ->
            db.insertTag(tag)
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextReplacement(name)
        viewModel.saveTag()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        var userMessage: UserMessage? = null
        val name = loremFaker.lorem.words()
        val tag = genTag()
        val db = FakeDatabase().apply {
            insertTag(tag)
        }
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = spyk(FakeTagRepository(db))
        coEvery { tagRepository.update(any()) } throws RuntimeException("Test")

        val viewModel =
            createTagFormViewModelForEdit(tagRepository, taskRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    initialName = "",
                    onSelectTaskType = {},
                    tagFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextReplacement(name)
        viewModel.saveTag()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.tag_form_save_error_edit)
        }
    }

    private fun genTag() = Tag(id = 1L, name = loremFaker.lorem.words())

    private fun createSavedStateHandleForEdit() = SavedStateHandle(mapOf("id" to 1L))

    private fun createTagFormViewModelForNew(
        initDatabase: (FakeDatabase) -> Unit = {},
    ): TagFormViewModel {
        val db = FakeDatabase().also(initDatabase)
        return createTagFormViewModelForNew(
            FakeTagRepository(db),
            FakeTaskRepository(db),
        )
    }

    private fun createTagFormViewModelForNew(
        tagRepository: TagRepository,
        taskRepository: TaskRepository,
    ) = TagFormViewModel(
        SavedStateHandle(),
        tagRepository,
        taskRepository,
    )

    private fun createTagFormViewModelForEdit(
        initDatabase: (FakeDatabase) -> Unit
    ): TagFormViewModel {
        val db = FakeDatabase().also(initDatabase)
        return createTagFormViewModelForEdit(
            FakeTagRepository(db),
            FakeTaskRepository(db),
        )
    }

    private fun createTagFormViewModelForEdit(
        tagRepository: TagRepository,
        taskRepository: TaskRepository,
    ) = TagFormViewModel(
        createSavedStateHandleForEdit(),
        tagRepository,
        taskRepository,
    )
}
