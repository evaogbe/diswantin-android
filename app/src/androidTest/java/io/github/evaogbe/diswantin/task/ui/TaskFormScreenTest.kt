package io.github.evaogbe.diswantin.task.ui

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
class TaskFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun topBar_displaysTitleForNew_whenNew() {
        composeTestRule.setContent {
            TaskFormTopBar(
                uiState = TaskFormTopBarState(isNew = true, showSave = false, saveEnabled = false),
                onClose = {},
                onSave = {},
            )
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_title_new))
            .assertIsDisplayed()
    }

    @Test
    fun topBar_displaysTitleForEdit_whenEdit() {
        composeTestRule.setContent {
            TaskFormTopBar(
                uiState = TaskFormTopBarState(isNew = false, showSave = false, saveEnabled = false),
                onClose = {},
                onSave = {},
            )
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_title_edit))
            .assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val viewModel = createTaskFormViewModelForEdit {}

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun displaysAllDateTimeButtonsInitially() {
        val viewModel = createTaskFormViewModelForNew()

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }

    @Test
    fun doesNotDisplayAddScheduleButton_whenNonRecurringTaskHasDeadline() {
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(
                genTask().copy(
                    deadlineDate = faker.random.randomFutureDate().toLocalDate()
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.deadline_date_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }


    @Test
    fun doesNotDisplayAddScheduleButton_whenNonRecurringTaskHasStartAfter() {
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(
                genTask().copy(
                    startAfterDate = faker.random.randomFutureDate().toLocalDate()
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.start_after_date_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }

    @Test
    fun doesNotDisplayAddScheduleButton_whenRecurringTaskHasDeadline() {
        val task = genTask().copy(deadlineTime = faker.random.randomFutureDate().toLocalTime())
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(task)
            db.insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = faker.random.randomPastDate().toLocalDate(),
                    type = RecurrenceType.Day,
                    step = 1
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.deadline_time_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }

    @Test
    fun doesNotDisplayAddScheduleButton_whenRecurringTaskHasStartAfter() {
        val task = genTask().copy(startAfterTime = faker.random.randomFutureDate().toLocalTime())
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(task)
            db.insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = faker.random.randomPastDate().toLocalDate(),
                    type = RecurrenceType.Day,
                    step = 1
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.start_after_time_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }

    @Test
    fun displaysAddScheduleTimeButton_whenNonRecurringTaskHasScheduledDate() {
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(
                genTask().copy(
                    scheduledDate = faker.random.randomFutureDate().toLocalDate()
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_date_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
    }

    @Test
    fun displaysScheduleTime_whenNonRecurringTaskHasScheduledTime() {
        val scheduledAt = faker.random.randomFutureDate()
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(
                genTask().copy(
                    scheduledDate = scheduledAt.toLocalDate(),
                    scheduledTime = scheduledAt.toLocalTime()
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_date_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_time_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }

    @Test
    fun displaysScheduledTime_whenRecurringTaskHasScheduledTime() {
        val task = genTask().copy(scheduledTime = faker.random.randomFutureDate().toLocalTime())
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(task)
            db.insertTaskRecurrence(
                TaskRecurrence(
                    taskId = task.id,
                    start = faker.random.randomPastDate().toLocalDate(),
                    type = RecurrenceType.Day,
                    step = 1
                )
            )
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_time_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_date_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.start_after_time_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_start_after_time_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_time_button))
            .assertDoesNotExist()
    }

    @Test
    fun displaysParentTaskField_whenHasOtherTasks() {
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(genTask())
            db.insertTask(genTask(id = 2L))
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label), useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenFetchExistingParentTaskFails() {
        var userMessage: UserMessage? = null
        val clock = createClock()
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTask(genTask(id = 2L))
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        every { taskRepository.getParent(any()) } returns flow { throw RuntimeException("Test") }

        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label), useUnmergedTree = true
        ).assertDoesNotExist()
        assertThat(userMessage).isEqualTo(UserMessage.String(R.string.task_form_fetch_parent_task_error))
    }

    @Test
    fun displaysTagField_whenHasTags() {
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(genTask())
            db.insertTag(Tag(name = loremFaker.lorem.words()))
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.add_tag_button), useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenFetchExistingTagFails() {
        var userMessage: UserMessage? = null
        val clock = createClock()
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTag(Tag(name = loremFaker.lorem.words()))
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = spyk(FakeTagRepository(db))
        every { tagRepository.getTagsByTaskId(any(), any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.tag_name_label), useUnmergedTree = true
        ).assertDoesNotExist()
        assertThat(userMessage).isEqualTo(UserMessage.String(R.string.task_form_fetch_tags_error))
    }

    @Test
    fun hidesAddTagButton_when10TagsAdded() {
        val tags = List(10) {
            Tag(id = it + 1L, name = loremFaker.lorem.unique.words())
        }
        val viewModel = createTaskFormViewModelForNew { db ->
            tags.forEach(db::insertTag)
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        tags.forEach { tag ->
            composeTestRule.onNodeWithText(stringResource(R.string.add_tag_button))
                .performScrollTo().performClick()
            composeTestRule.onNodeWithText(
                stringResource(R.string.tag_name_label), useUnmergedTree = true
            ).onParent().performTextInput(tag.name.substring(0, 1))

            composeTestRule.waitForIdle()
            composeTestRule.waitUntilExactlyOneExists(hasText(tag.name))
            composeTestRule.onNodeWithText(tag.name).performClick()
        }

        composeTestRule.waitUntilDoesNotExist(hasText(stringResource(R.string.tag_name_label)))
        composeTestRule.onNodeWithText(
            stringResource(R.string.tag_name_label), useUnmergedTree = true
        ).assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_tag_button)).assertDoesNotExist()

        composeTestRule.onNodeWithText(tags.first().name).performScrollTo().performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.add_tag_button)).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun displaysMatchingTaskOptions_whenParentTaskSearchedFor() {
        val query = loremFaker.verbs.base()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.unique.words()}",
            )
        }
        val viewModel = createTaskFormViewModelForNew { db ->
            tasks.forEach(db::insertTask)
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label), useUnmergedTree = true
        ).onParent().performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchParentTasksFails() {
        var userMessage: UserMessage? = null
        val query = loremFaker.verbs.base()
        val clock = createClock()
        val db = FakeDatabase().apply {
            insertTask(genTask())
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        every { taskRepository.search(any(), any()) } returns flow {
            throw RuntimeException("Test")
        }

        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            SavedStateHandle(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label), useUnmergedTree = true
        ).onParent().performTextInput(query)

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.search_task_options_error)
        }
    }

    @Test
    fun displaysMatchingTagOptions_whenTagSearchedFor() {
        val query = loremFaker.verbs.base()
        val tags = List(3) {
            Tag(id = it + 1L, name = "$query ${loremFaker.lorem.unique.words()}")
        }
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(genTask())
            tags.forEach { db.insertTag(it) }
        }

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_tag_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.tag_name_label), useUnmergedTree = true
        ).onParent().performTextInput(query)

        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExists(hasText(tags[0].name))
        composeTestRule.onNodeWithText(tags[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tags[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tags[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchTagsFails() {
        var userMessage: UserMessage? = null
        val query = loremFaker.verbs.base()
        val tag = Tag(name = faker.string.regexify("""$query \w+"""))
        val clock = createClock()
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTag(tag)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = spyk(FakeTagRepository(db))
        every { tagRepository.search(any(), any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_tag_button)).performClick()
        composeTestRule.onNodeWithText(
            stringResource(R.string.tag_name_label), useUnmergedTree = true
        ).onParent().performTextInput(query)

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.search_tag_options_error)
        }
    }

    @Test
    fun popsBackStack_whenTaskCreated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var onPopBackStackCalled = false
        val topBarActionState = MutableStateFlow<TaskFormTopBarAction?>(null)
        val viewModel = createTaskFormViewModelForNew()

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.ok_button)).performClick()
        topBarActionState.value = TaskFormTopBarAction.Save

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        var userMessage: UserMessage? = null
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val topBarActionState = MutableStateFlow<TaskFormTopBarAction?>(null)
        val clock = createClock()
        val db = FakeDatabase()
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.create(any()) } throws RuntimeException("Test")

        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            SavedStateHandle(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    setUserMessage = { userMessage = it },
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextInput(name)
        topBarActionState.value = TaskFormTopBarAction.Save

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_form_save_error_new)
        }
    }

    @Test
    fun popsBackStack_whenTaskUpdated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var onPopBackStackCalled = false
        val topBarActionState = MutableStateFlow<TaskFormTopBarAction?>(null)
        val task = genTask().copy(deadlineDate = faker.random.randomFutureDate().toLocalDate())
        val viewModel = createTaskFormViewModelForEdit { db ->
            db.insertTask(task)
        }

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    setUserMessage = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.deadline_date_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_time_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_date_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.scheduled_time_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextReplacement(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.remove_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.ok_button)).performClick()
        topBarActionState.value = TaskFormTopBarAction.Save

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        var userMessage: UserMessage? = null
        val topBarActionState = MutableStateFlow<TaskFormTopBarAction?>(null)
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val task = genTask()
        val clock = createClock()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db, clock))
        coEvery { taskRepository.update(any()) } throws RuntimeException("Test")

        val tagRepository = FakeTagRepository(db)
        val viewModel = TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            tagRepository,
            clock,
            createLocale(),
        )

        composeTestRule.setContent {
            val topBarAction by topBarActionState.collectAsStateWithLifecycle()

            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = topBarAction,
                    topBarActionHandled = { topBarActionState.value = null },
                    setUserMessage = { userMessage = it },
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextReplacement(name)
        topBarActionState.value = TaskFormTopBarAction.Save

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_form_save_error_edit)
        }
    }

    private fun genTask(id: Long = 1L) = Task(
        id = id,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createSavedStateHandleForEdit() = SavedStateHandle(mapOf("id" to 1L))

    private fun createClock() = Clock.systemDefaultZone()

    private fun createLocale() = Locale.getDefault()

    private fun createTaskFormViewModelForNew(
        initDatabase: (FakeDatabase) -> Unit = {}
    ): TaskFormViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TaskFormViewModel(
            SavedStateHandle(),
            FakeTaskRepository(db, clock),
            FakeTagRepository(db),
            clock,
            createLocale(),
        )
    }

    private fun createTaskFormViewModelForEdit(
        initDatabase: (FakeDatabase) -> Unit
    ): TaskFormViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TaskFormViewModel(
            createSavedStateHandleForEdit(),
            FakeTaskRepository(db, clock),
            FakeTagRepository(db),
            clock,
            createLocale(),
        )
    }
}
