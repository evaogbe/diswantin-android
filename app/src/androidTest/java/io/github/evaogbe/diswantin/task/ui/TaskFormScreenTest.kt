package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
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
    fun displaysFormTypeButtonGroup_whenNew() {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.form_type_button_category))
            .assertIsDisplayed()
    }

    @Test
    fun doesNotDisplayFormTypeButtonGroup_whenEdit() {
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.form_type_button_category))
            .assertDoesNotExist()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun displaysParentTaskField_whenHasOtherTasks() {
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTask(genTask(id = 2L))
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenFetchExistingParentTaskFails() {
        var userMessage: UserMessage? = null
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTask(genTask(id = 2L))
        }
        val taskRepository = spyk(FakeTaskRepository(db))
        every { taskRepository.getParent(any()) } returns flow { throw RuntimeException("Test") }

        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        ).assertDoesNotExist()
        assertThat(userMessage)
            .isEqualTo(UserMessage.String(R.string.task_form_fetch_parent_task_error))
    }

    @Test
    fun displaysCategoryField_whenHasCategories() {
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTaskCategory(TaskCategory(name = loremFaker.lorem.words()), emptySet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_category_label),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenFetchExistingCategoryFails() {
        var userMessage: UserMessage? = null
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTaskCategory(TaskCategory(name = loremFaker.lorem.words()), emptySet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
        every { taskCategoryRepository.getByTaskId(any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_category_label),
            useUnmergedTree = true
        ).assertDoesNotExist()
        assertThat(userMessage)
            .isEqualTo(UserMessage.String(R.string.task_form_fetch_category_error))
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
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(tasks[0].name))
        composeTestRule.onNodeWithText(tasks[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tasks[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchParentTasksFails() {
        var userMessage: UserMessage? = null
        val query = loremFaker.verbs.base()
        val db = FakeDatabase().apply {
            insertTask(genTask())
        }
        val taskRepository = spyk(FakeTaskRepository(db))
        every { taskRepository.search(any()) } returns flow { throw RuntimeException("Test") }

        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.parent_task_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.search_task_options_error)
        }
    }

    @Test
    fun displaysMatchingCategoryOptions_whenCategorySearchedFor() {
        val query = loremFaker.verbs.base()
        val categories = List(3) {
            TaskCategory(id = it + 1L, name = "$query ${loremFaker.lorem.unique.words()}")
        }
        val db = FakeDatabase().apply {
            insertTask(genTask())
            categories.forEach { insertTaskCategory(it, emptySet()) }
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_category_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(categories[0].name))
        composeTestRule.onNodeWithText(categories[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(categories[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(categories[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSearchCategoriesFails() {
        var userMessage: UserMessage? = null
        val query = loremFaker.verbs.base()
        val category = TaskCategory(name = faker.string.regexify("""$query \w+"""))
        val db = FakeDatabase().apply {
            insertTask(genTask())
            insertTaskCategory(category, emptySet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
        every { taskCategoryRepository.search(any()) } returns flow {
            throw RuntimeException("Test")
        }

        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.task_category_label),
            useUnmergedTree = true
        )
            .onParent()
            .performTextInput(query)

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.search_task_category_options_error)
        }
    }

    @Test
    fun popsBackStack_whenTaskCreated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var onPopBackStackCalled = false
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
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
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.add_deadline_date_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.ok_button)).performClick()
        viewModel.saveTask()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        var userMessage: UserMessage? = null
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val db = FakeDatabase()
        val taskRepository = spyk(FakeTaskRepository(db))
        coEvery { taskRepository.create(any()) } throws RuntimeException("Test")

        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_save_error_new))
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        viewModel.saveTask()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_form_save_error_new)
        }
    }

    @Test
    fun popsBackStack_whenTaskUpdated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var onPopBackStackCalled = false
        val task = genTask().copy(deadlineDate = faker.random.randomFutureDate().toLocalDate())
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = {},
                    onSelectCategoryType = {},
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
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.clear_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.add_scheduled_at_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.ok_button)).performClick()
        viewModel.saveTask()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        var userMessage: UserMessage? = null
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val task = genTask()
        val db = FakeDatabase().apply {
            insertTask(task)
        }
        val taskRepository = spyk(FakeTaskRepository(db))
        coEvery { taskRepository.update(any()) } throws RuntimeException("Test")

        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TaskFormScreen(
                    onPopBackStack = {},
                    setTopBarState = {},
                    topBarAction = null,
                    topBarActionHandled = {},
                    setUserMessage = { userMessage = it },
                    onSelectCategoryType = {},
                    onEditRecurrence = {},
                    taskFormViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.task_form_save_error_edit))
            .assertDoesNotExist()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        viewModel.saveTask()

        composeTestRule.waitUntil {
            userMessage == UserMessage.String(R.string.task_form_save_error_edit)
        }
    }

    private fun genTask(id: Long = 1L) = Task(
        id = id,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createTaskFormViewModelForNew(
        taskRepository: TaskRepository,
        taskCategoryRepository: TaskCategoryRepository,
    ) =
        TaskFormViewModel(
            SavedStateHandle(),
            taskRepository,
            taskCategoryRepository,
            Clock.systemDefaultZone(),
            Locale.getDefault(),
        )

    private fun createTaskFormViewModelForEdit(
        taskRepository: TaskRepository,
        taskCategoryRepository: TaskCategoryRepository,
    ) =
        TaskFormViewModel(
            SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L)),
            taskRepository,
            taskCategoryRepository,
            Clock.systemDefaultZone(),
            Locale.getDefault(),
        )
}
