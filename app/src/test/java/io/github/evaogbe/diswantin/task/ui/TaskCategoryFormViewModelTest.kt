package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCategoryFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

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

        assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

        advanceUntilIdle()

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryFormUiState.Success(
                newTasks = persistentListOf(),
                isEditing = true,
                taskOptions = persistentListOf(),
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
        assertThat(viewModel.existingTaskPagingData.asSnapshot()).isEqualTo(tasks)
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryFormUiState.Success(
                newTasks = persistentListOf(),
                isEditing = false,
                taskOptions = persistentListOf(),
                userMessage = null,
            )
        )
        assertThat(viewModel.nameInput).isEqualTo(category.name)
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.getById(any()) } returns flow {
                throw exception
            }

            val viewModel = TaskCategoryFormViewModel(
                createSavedStateHandleForEdit(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Failure(exception))
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

        assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

        advanceUntilIdle()

        viewModel.searchTasks(query)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryFormUiState.Success(
                newTasks = persistentListOf(),
                isEditing = true,
                taskOptions = tasks.toImmutableList(),
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val db = FakeDatabase()
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.search(any()) } returns flow { throw RuntimeException("Test") }

            val taskCategoryRepository = FakeTaskCategoryRepository(db)
            val viewModel = TaskCategoryFormViewModel(
                SavedStateHandle(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

            advanceUntilIdle()

            viewModel.searchTasks(query)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    userMessage = UserMessage.String(R.string.search_task_options_error),
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
            val viewModel = TaskCategoryFormViewModel(
                SavedStateHandle(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            tasks.forEach(viewModel::addTask)
            advanceUntilIdle()
            viewModel.saveCategory()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Saved)

            val category = taskCategoryRepository.taskCategories.single()
            assertThat(category.name).isEqualTo(name)
            assertThat(taskRepository.getTasksByCategoryId(category.id).asSnapshot()).isEqualTo(
                tasks.map { it.copy(categoryId = category.id) })
        }

    @Test
    fun `saveCategory shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            coEvery { taskCategoryRepository.create(any()) } throws RuntimeException("Test")

            val viewModel = TaskCategoryFormViewModel(
                SavedStateHandle(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

            advanceUntilIdle()

            viewModel.updateNameInput(name)
            viewModel.saveCategory()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    userMessage = UserMessage.String(R.string.task_category_form_save_error_new),
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

            assertThat(viewModel.existingTaskPagingData.asSnapshot()).isEqualTo(tasks)
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    userMessage = null,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.saveCategory()
            advanceUntilIdle()

            val updatedCategory = taskCategoryRepository.getById(category.id).first()
            val updatedTasks = taskRepository.getTasksByCategoryId(category.id).asSnapshot()
            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Saved)
            assertThat(updatedCategory).isEqualTo(category.copy(name = name))
            assertThat(updatedTasks).isEqualTo(tasks)
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
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            coEvery { taskCategoryRepository.update(any()) } throws RuntimeException("Test")

            val viewModel = TaskCategoryFormViewModel(
                createSavedStateHandleForEdit(),
                taskCategoryRepository,
                taskRepository,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryFormUiState.Pending)

            advanceUntilIdle()

            viewModel.updateNameInput(name)
            viewModel.saveCategory()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryFormUiState.Success(
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    userMessage = UserMessage.String(R.string.task_category_form_save_error_edit),
                )
            )
        }

    private fun genTaskCategory() = TaskCategory(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks(categoryId: Long? = null) = List(3) {
        Task(
            id = it + 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${it + 1}. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            categoryId = categoryId,
        )
    }

    private fun createSavedStateHandleForEdit() = SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L))
}
