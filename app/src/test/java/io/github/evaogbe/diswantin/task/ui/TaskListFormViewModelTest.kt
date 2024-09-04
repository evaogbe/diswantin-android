package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new without taskListId`() = runTest(mainDispatcherRule.testDispatcher) {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel =
            TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskListFormUiState.Success(
                tasks = persistentListOf(),
                editingTaskIndex = 0,
                taskOptions = persistentListOf(),
                hasSaveError = false,
                userMessage = null,
            )
        )
        assertThat(viewModel.nameInput).isEqualTo("")
    }

    @Test
    fun `initializes for edit with taskListId`() = runTest(mainDispatcherRule.testDispatcher) {
        val taskList = genTaskList()
        val tasks = genTasks(listId = taskList.id)
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTaskList(taskList, tasks.map(Task::id))
        }
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel = TaskListFormViewModel(
            createSavedStateHandleForEdit(),
            taskListRepository,
            taskRepository,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isFalse()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskListFormUiState.Success(
                tasks = tasks.toImmutableList(),
                editingTaskIndex = null,
                taskOptions = persistentListOf(),
                hasSaveError = false,
                userMessage = null,
            )
        )
        assertThat(viewModel.nameInput).isEqualTo(taskList.name)
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            taskListRepository.setThrows(taskListRepository::getTaskListWithTasksById, true)

            val viewModel = TaskListFormViewModel(
                createSavedStateHandleForEdit(),
                taskListRepository,
                taskRepository
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskListFormUiState.Failure)
        }

    @Test
    fun `searchTasks adds task options`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val tasks = List(faker.random.nextInt(bound = 5)) {
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
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel =
            TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchTasks(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskListFormUiState.Success(
                tasks = persistentListOf(),
                editingTaskIndex = 0,
                taskOptions = tasks.toImmutableList(),
                hasSaveError = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel =
                TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::search, true)
            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListFormUiState.Success(
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = R.string.task_list_form_search_tasks_error,
                )
            )
        }

    @Test
    fun `saveTaskList creates task list without taskListId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val tasks = genTasks()
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
            }
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel =
                TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListFormUiState.Success(
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            tasks.forEachIndexed { index, task ->
                viewModel.setTask(index, task)
            }
            viewModel.saveTaskList()

            val taskList = taskListRepository.taskLists.single()
            assertThat(taskList.name).isEqualTo(name)
            assertThat(taskListRepository.getTaskListWithTasksById(taskList.id).first())
                .isNotNull()
                .prop(TaskListWithTasks::tasks)
                .isEqualTo(tasks.map { it.copy(listId = taskList.id) })
            assertThat(viewModel.uiState.value).isEqualTo(TaskListFormUiState.Saved)
        }

    @Test
    fun `saveTaskList shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel =
                TaskListFormViewModel(SavedStateHandle(), taskListRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskListRepository.setThrows(taskListRepository::create, true)
            viewModel.updateNameInput(name)
            viewModel.saveTaskList()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListFormUiState.Success(
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `saveTaskList updates task list with taskListId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val taskList = genTaskList()
            val tasks = genTasks(listId = taskList.id)
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskList(taskList, tasks.map(Task::id))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel = TaskListFormViewModel(
                createSavedStateHandleForEdit(),
                taskListRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListFormUiState.Success(
                    tasks = tasks.toImmutableList(),
                    editingTaskIndex = null,
                    taskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveTaskList()

            assertThat(taskListRepository.getTaskListWithTasksById(taskList.id).first())
                .isNotNull()
                .isEqualTo(TaskListWithTasks(taskList.copy(name = name), tasks))
            assertThat(viewModel.uiState.value).isEqualTo(TaskListFormUiState.Saved)
        }

    @Test
    fun `saveTaskList shows error message with update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val taskList = genTaskList()
            val tasks = genTasks(listId = taskList.id)
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskList(taskList, tasks.map(Task::id))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel = TaskListFormViewModel(
                createSavedStateHandleForEdit(),
                taskListRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskListRepository.setThrows(taskListRepository::update, true)
            viewModel.updateNameInput(name)
            viewModel.saveTaskList()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListFormUiState.Success(
                    tasks = tasks.toImmutableList(),
                    editingTaskIndex = null,
                    taskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    private fun genTaskList() = TaskList(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks(listId: Long? = null) =
        List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                listId = listId,
            )
        }

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))
}
