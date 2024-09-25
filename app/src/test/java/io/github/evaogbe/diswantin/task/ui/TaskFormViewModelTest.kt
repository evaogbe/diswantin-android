package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.RecurrenceType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryRepository
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new without taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isTrue()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
            assertThat(viewModel.nameInput).isEmpty()
        }

    @Test
    fun `initializes for edit with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val locale = Locale.US
            val task = genTask().copy(
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = LocalTime.parse("17:00"),
            )
            val category = TaskCategory(name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskRecurrence(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                        week = 4,
                    )
                )
                insertTaskCategory(category, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel =
                TaskFormViewModel(
                    createSavedStateHandleForEdit(),
                    taskRepository,
                    taskCategoryRepository,
                    clock,
                    locale,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("17:00"),
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = TaskRecurrenceUiState(
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                        weekdays = persistentSetOf(),
                        locale = locale,
                    ),
                    showCategoryField = true,
                    category = category,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
            assertThat(viewModel.nameInput).isEqualTo(task.name)
        }

    @Test
    fun `uiState emits failure when fetch existing task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            every { taskRepository.getById(any()) } returns flow { throw exception }

            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure(exception))
        }

    @Test
    fun `uiState emits failure when fetch existing task recurrences fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getTaskRecurrencesByTaskId(any()) } returns flow {
                throw exception
            }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure(exception))
        }

    @Test
    fun `uiState shows category field when has categories`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val category = TaskCategory(name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTaskCategory(category, emptySet())
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = true,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides category field when does not have categories`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides category field when query has categories fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val category = TaskCategory(name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTaskCategory(category, emptySet())
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.hasCategoriesStream } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides category field when fetch existing category fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val category = TaskCategory(name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskCategory(category, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.getByTaskId(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState shows parent task field when taskId is null and repository has tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase().apply {
                insertTask(genTask())
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState shows parent task field when taskId is present and repository has other tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase().apply {
                insertTask(genTask())
                insertTask(genTask(id = 2L))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides parent task field when fetch count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase().apply {
                insertTask(genTask())
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getCount() } returns flow { throw RuntimeException("Test") }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState hides parent task field when fetch existing task parent fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getParent(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `searchCategories fetches categoryOptions`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val categories = List(faker.random.nextInt(min = 1, max = 5)) {
            TaskCategory(id = it + 1L, name = faker.string.regexify("""$query \w+"""))
        }
        val db = FakeDatabase().apply {
            categories.forEach { insertTaskCategory(it, emptySet()) }
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchCategories(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                deadlineDate = null,
                deadlineTime = null,
                scheduledDate = null,
                scheduledTime = null,
                recurrence = null,
                showCategoryField = true,
                category = null,
                categoryOptions = categories.toImmutableList(),
                showParentTaskField = false,
                parentTask = null,
                parentTaskOptions = persistentListOf(),
                hasSaveError = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchCategories shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val category = TaskCategory(name = faker.string.regexify("""$query \w+"""))
            val db = FakeDatabase().apply {
                insertTaskCategory(category, emptySet())
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.search(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.searchCategories(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = true,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = R.string.search_task_category_options_error,
                )
            )
        }

    @Test
    fun `searchTasks fetches parentTaskOptions`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val tasks = List(faker.random.nextInt(min = 1, max = 5)) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        }
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchTasks(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskFormUiState.Success(
                deadlineDate = null,
                deadlineTime = null,
                scheduledDate = null,
                scheduledTime = null,
                recurrence = null,
                showCategoryField = false,
                category = null,
                categoryOptions = persistentListOf(),
                showParentTaskField = true,
                parentTask = null,
                parentTaskOptions = tasks.toPersistentList(),
                hasSaveError = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val db = FakeDatabase().apply {
                insertTask(genTask())
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.search(any()) } returns flow { throw RuntimeException("Test") }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = true,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = R.string.search_task_options_error,
                )
            )
        }

    @Test
    fun `saveTask creates task without taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val locale = Locale.US
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db, clock)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = TaskFormViewModel(
                SavedStateHandle(),
                taskRepository,
                taskCategoryRepository,
                clock,
                locale,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineDate(LocalDate.parse("2024-08-22"))
            viewModel.updateDeadlineTime(LocalTime.parse("17:00"))
            viewModel.updateRecurrence(
                TaskRecurrenceUiState(
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Day,
                    step = 1,
                    weekdays = persistentSetOf(),
                    locale = locale,
                ),
            )
            viewModel.saveTask()

            val task = taskRepository.tasks.single()
            assertThat(task).isEqualToIgnoringGivenProperties(
                Task(
                    createdAt = Instant.parse("2024-08-22T08:00:00Z"),
                    name = name,
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("17:00"),
                ),
                Task::id,
            )
            assertThat(taskRepository.getTaskRecurrencesByTaskId(task.id).first().single())
                .isEqualToIgnoringGivenProperties(
                    TaskRecurrence(
                        taskId = task.id,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence::id,
                )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val db = FakeDatabase()
            val taskRepository = spyk(FakeTaskRepository(db))
            coEvery { taskRepository.create(any()) } throws RuntimeException("Test")

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForNew(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `saveTask updates task with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val locale = Locale.US
            val task = genTask().copy(
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = LocalTime.parse("21:00"),
            )
            val category = TaskCategory(id = 1L, name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskCategory(category, emptySet())
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel =
                TaskFormViewModel(
                    createSavedStateHandleForEdit(),
                    taskRepository,
                    taskCategoryRepository,
                    clock,
                    locale,
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("21:00"),
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = true,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineDate(null)
            viewModel.updateDeadlineTime(null)
            viewModel.updateScheduledDate(LocalDate.parse("2024-08-22"))
            viewModel.updateScheduledTime(LocalTime.parse("17:00"))
            viewModel.updateRecurrence(
                TaskRecurrenceUiState(
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Week,
                    step = 2,
                    weekdays = persistentSetOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                    locale = locale,
                ),
            )
            viewModel.updateCategory(category)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = LocalDate.parse("2024-08-22"),
                    scheduledTime = LocalTime.parse("17:00"),
                    categoryId = category.id,
                )
            )
            assertThat(
                taskRepository.getTaskRecurrencesByTaskId(task.id).first()
                    .map { it.copy(id = 0) }
            ).containsExactly(
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-22"),
                    type = RecurrenceType.Week,
                    step = 2,
                    week = 4,
                ),
                TaskRecurrence(
                    taskId = task.id,
                    start = LocalDate.parse("2024-08-26"),
                    type = RecurrenceType.Week,
                    step = 2,
                    week = 5,
                ),
            )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val db = FakeDatabase().apply {
                insertTask(task)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            coEvery { taskRepository.update(any()) } throws RuntimeException("Test")

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `saveTask maintains category when query has category fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val category = TaskCategory(id = 1L, name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskCategory(category, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.hasCategoriesStream } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = category,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = category.id,
                )
            )
        }

    @Test
    fun `saveTask maintains category when fetch existing category fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val category = TaskCategory(id = 1L, name = loremFaker.lorem.words())
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTaskCategory(category, setOf(task.id))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.getByTaskId(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = category.id,
                )
            )
        }

    @Test
    fun `saveTask maintains parent task when fetch task count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTask(parentTask)
                insertChain(parentId = parentTask.id, childId = task.id)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getCount() } returns flow { throw RuntimeException("Test") }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = parentTask,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(taskRepository.getParent(task.id).first()).isEqualTo(parentTask)
        }

    @Test
    fun `saveTask maintains parent task when fetch existing parent fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val parentTask = genTask(id = 2L)
            val db = FakeDatabase().apply {
                insertTask(task)
                insertTask(parentTask)
                insertChain(parentId = parentTask.id, childId = task.id)
            }
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.getParent(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = createTaskFormViewModelForEdit(taskRepository, taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrence = null,
                    showCategoryField = false,
                    category = null,
                    categoryOptions = persistentListOf(),
                    showParentTaskField = false,
                    parentTask = null,
                    parentTaskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(FakeTaskRepository(db).getParent(task.id).first()).isEqualTo(parentTask)
        }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun genTask(id: Long = 1L) = Task(
        id = id,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
    )

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))

    private fun createTaskFormViewModelForNew(
        taskRepository: TaskRepository,
        taskCategoryRepository: TaskCategoryRepository,
    ) =
        TaskFormViewModel(
            SavedStateHandle(),
            taskRepository,
            taskCategoryRepository,
            createClock(),
            Locale.US
        )

    private fun createTaskFormViewModelForEdit(
        taskRepository: TaskRepository,
        taskCategoryRepository: TaskCategoryRepository,
    ) =
        TaskFormViewModel(
            createSavedStateHandleForEdit(),
            taskRepository,
            taskCategoryRepository,
            createClock(),
            Locale.US,
        )
}
