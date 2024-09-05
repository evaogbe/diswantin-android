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
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
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
class TaskCategoryFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new without categoryId`() = runTest(mainDispatcherRule.testDispatcher) {
        val db = FakeDatabase()
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryFormUiState.Success(
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
    fun `initializes for edit with categoryId`() = runTest(mainDispatcherRule.testDispatcher) {
        val category = genTaskCategory()
        val tasks = genTasks(categoryId = category.id)
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTaskCategory(category, tasks.map(Task::id).toSet())
        }
        val taskRepository = FakeTaskRepository(db)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = TaskCategoryFormViewModel(
            createSavedStateHandleForEdit(),
            taskCategoryRepository,
            taskRepository,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isFalse()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryFormUiState.Success(
                tasks = tasks.toImmutableList(),
                editingTaskIndex = null,
                taskOptions = persistentListOf(),
                hasSaveError = false,
                userMessage = null,
            )
        )
        assertThat(viewModel.nameInput).isEqualTo(category.name)
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            taskCategoryRepository.setThrows(taskCategoryRepository::getCategoryWithTasksById, true)

            val viewModel = TaskCategoryFormViewModel(
                createSavedStateHandleForEdit(),
                taskCategoryRepository,
                taskRepository
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Failure)
        }

    @Test
    fun `searchTasks sets task options`() = runTest(mainDispatcherRule.testDispatcher) {
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
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel =
            TaskCategoryFormViewModel(SavedStateHandle(), taskCategoryRepository, taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchTasks(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryFormUiState.Success(
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
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel =
                TaskCategoryFormViewModel(
                    SavedStateHandle(),
                    taskCategoryRepository,
                    taskRepository
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::search, true)
            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = R.string.task_category_form_search_tasks_error,
                )
            )
        }

    @Test
    fun `saveCategory creates task category without categoryId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val tasks = genTasks()
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel =
                TaskCategoryFormViewModel(
                    SavedStateHandle(),
                    taskCategoryRepository,
                    taskRepository
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
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
            viewModel.saveCategory()

            val category = taskCategoryRepository.taskCategories.single()
            assertThat(category.name).isEqualTo(name)
            assertThat(taskCategoryRepository.getCategoryWithTasksById(category.id).first())
                .isNotNull()
                .prop(TaskCategoryWithTasks::tasks)
                .isEqualTo(tasks.map { it.copy(categoryId = category.id) })
            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Saved)
        }

    @Test
    fun `saveCategory shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel =
                TaskCategoryFormViewModel(
                    SavedStateHandle(),
                    taskCategoryRepository,
                    taskRepository
                )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskCategoryRepository.setThrows(taskCategoryRepository::create, true)
            viewModel.updateNameInput(name)
            viewModel.saveCategory()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    tasks = persistentListOf(),
                    editingTaskIndex = 0,
                    taskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `saveCategory updates task category with categoryId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val category = genTaskCategory()
            val tasks = genTasks(categoryId = category.id)
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskCategory(category, tasks.map(Task::id).toSet())
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = TaskCategoryFormViewModel(
                createSavedStateHandleForEdit(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    tasks = tasks.toImmutableList(),
                    editingTaskIndex = null,
                    taskOptions = persistentListOf(),
                    hasSaveError = false,
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveCategory()

            assertThat(taskCategoryRepository.getCategoryWithTasksById(category.id).first())
                .isNotNull()
                .isEqualTo(TaskCategoryWithTasks(category.copy(name = name), tasks))
            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Saved)
        }

    @Test
    fun `saveCategory shows error message with update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val category = genTaskCategory()
            val tasks = genTasks(categoryId = category.id)
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskCategory(category, tasks.map(Task::id).toSet())
            }
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = TaskCategoryFormViewModel(
                createSavedStateHandleForEdit(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskCategoryRepository.setThrows(taskCategoryRepository::update, true)
            viewModel.updateNameInput(name)
            viewModel.saveCategory()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    tasks = tasks.toImmutableList(),
                    editingTaskIndex = null,
                    taskOptions = persistentListOf(),
                    hasSaveError = true,
                    userMessage = null,
                )
            )
        }

    private fun genTaskCategory() = TaskCategory(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks(categoryId: Long? = null) =
        List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "${it + 1}. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                categoryId = categoryId,
            )
        }

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))
}
