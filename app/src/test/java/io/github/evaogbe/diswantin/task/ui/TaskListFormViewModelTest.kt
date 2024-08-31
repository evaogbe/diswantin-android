package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskList
import io.github.evaogbe.diswantin.task.data.TaskListWithTasks
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskListRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
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
            )
        )
        assertThat(viewModel.nameInput).isEqualTo("")
    }

    @Test
    fun `initializes for edit with taskListId`() = runTest(mainDispatcherRule.testDispatcher) {
        val taskList = TaskList(id = 1L, name = loremFaker.lorem.words())
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                listId = taskList.id,
            )
        }
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTaskList(TaskListWithTasks(taskList, tasks))
        }
        val taskRepository = FakeTaskRepository(db)
        val taskListRepository = FakeTaskListRepository(db)
        val viewModel = TaskListFormViewModel(
            SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to taskList.id)),
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
            taskListRepository.setThrows(taskListRepository::getById, true)

            val viewModel = TaskListFormViewModel(
                SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to 1L)),
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
            )
        )
    }

    @Test
    fun `searchTasks sets task options to empty when repository throws`() =
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
                )
            )
        }

    @Test
    fun `saveTaskList creates task list without taskListId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val tasks = List(3) {
                Task(
                    id = it + 1L,
                    createdAt = faker.random.randomPastDate().toInstant(),
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
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

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskListFormUiState.Success(
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.setTask(0, tasks[0])
            viewModel.setTask(1, tasks[1])
            viewModel.setTask(2, tasks[2])
            viewModel.saveTaskList()

            val taskList = taskListRepository.taskLists.single()
            assertThat(taskList.name).isEqualTo(name)
            assertThat(taskListRepository.getById(taskList.id).first())
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
                )
            )
        }

    @Test
    fun `saveTaskList updates task list with taskListId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val taskList = TaskList(id = 1L, name = loremFaker.lorem.words())
            val tasks = List(3) {
                Task(
                    id = it + 1L,
                    createdAt = faker.random.randomPastDate().toInstant(),
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    listId = taskList.id,
                )
            }
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskList(TaskListWithTasks(taskList, tasks))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel = TaskListFormViewModel(
                SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to taskList.id)),
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
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveTaskList()

            assertThat(taskListRepository.getById(taskList.id).first())
                .isNotNull()
                .isEqualTo(TaskListWithTasks(taskList.copy(name = name), tasks))
            assertThat(viewModel.uiState.value).isEqualTo(TaskListFormUiState.Saved)
        }

    @Test
    fun `saveTaskList shows error message with update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val taskList = TaskList(id = 1L, name = loremFaker.lorem.words())
            val tasks = List(3) {
                Task(
                    id = it + 1L,
                    createdAt = faker.random.randomPastDate().toInstant(),
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    listId = taskList.id,
                )
            }
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskList(TaskListWithTasks(taskList, tasks))
            }
            val taskRepository = FakeTaskRepository(db)
            val taskListRepository = FakeTaskListRepository(db)
            val viewModel = TaskListFormViewModel(
                SavedStateHandle(mapOf(Destination.EditTaskListForm.ID_KEY to taskList.id)),
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
                )
            )
        }
}
