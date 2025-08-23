package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCategoryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches task category by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val category = genTaskCategory()
        val tasks = genTasks()
        val viewModel = createTaskCategoryDetailViewModel { db ->
            tasks.forEach(db::insertTask)
            db.insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
        }

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Pending)

        advanceUntilIdle()

        val taskItems = tasks.map { it.toTaskItemUiState() }
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryDetailUiState.Success(
                category = category,
                userMessage = null,
            )
        )
        assertThat(viewModel.taskItemPagingData.asSnapshot()).isEqualTo(taskItems)
    }

    @Test
    fun `uiState emits failure when task category not found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createTaskCategoryDetailViewModel {}

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf<TaskCategoryDetailUiState.Failure>()
                .prop(TaskCategoryDetailUiState.Failure::exception)
                .isInstanceOf<NullPointerException>()
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val category = genTaskCategory()
            val tasks = genTasks()
            val clock = createClock()
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            every { taskCategoryRepository.getById(any()) } returns flow {
                throw exception
            }

            val viewModel = TaskCategoryDetailViewModel(
                createSavedStateHandle(),
                taskCategoryRepository,
                taskRepository,
                clock,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryDetailUiState.Failure(exception)
            )
        }

    @Test
    fun `deleteCategory sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val category = genTaskCategory()
        val tasks = genTasks()
        val clock = createClock()
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val taskCategoryRepository = FakeTaskCategoryRepository(db)
        val viewModel = TaskCategoryDetailViewModel(
            createSavedStateHandle(),
            taskCategoryRepository,
            taskRepository,
            clock,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Pending)

        advanceUntilIdle()

        val taskItems = tasks.map { it.toTaskItemUiState() }
        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryDetailUiState.Success(
                category = category,
                userMessage = null,
            )
        )
        assertThat(viewModel.taskItemPagingData.asSnapshot()).isEqualTo(taskItems)

        viewModel.deleteCategory()
        advanceUntilIdle()

        assertThat(taskCategoryRepository.taskCategories).isEmpty()
        assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Deleted)
    }

    @Test
    fun `deleteCategory shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val category = genTaskCategory()
            val tasks = genTasks()
            val clock = createClock()
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTaskCategory(taskCategory = category, taskIds = tasks.map { it.id }.toSet())
            }
            val taskRepository = FakeTaskRepository(db, clock)
            val taskCategoryRepository = spyk(FakeTaskCategoryRepository(db))
            coEvery { taskCategoryRepository.delete(any()) } throws RuntimeException("Test")

            val viewModel = TaskCategoryDetailViewModel(
                createSavedStateHandle(),
                taskCategoryRepository,
                taskRepository,
                clock,
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Pending)

            advanceUntilIdle()

            val taskItems = tasks.map { it.toTaskItemUiState() }
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryDetailUiState.Success(
                    category = category,
                    userMessage = null,
                )
            )
            assertThat(viewModel.taskItemPagingData.asSnapshot()).isEqualTo(taskItems)

            viewModel.deleteCategory()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryDetailUiState.Success(
                    category = category,
                    userMessage = UserMessage.String(R.string.task_category_detail_delete_error),
                )
            )
            assertThat(viewModel.taskItemPagingData.asSnapshot()).isEqualTo(taskItems)
        }

    private fun Task.toTaskItemUiState() = TaskItemUiState(id = id, name = name, isDone = false)

    private fun genTaskCategory() = TaskCategory(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks() = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            categoryId = 1L,
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            categoryId = 1L,
        )
    }.take(faker.random.nextInt(bound = 5)).toList()

    private fun createSavedStateHandle(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every {
            savedStateHandle.toRoute<TaskCategoryDetailRoute>()
        } returns TaskCategoryDetailRoute(id = 1L)
        return savedStateHandle
    }

    private fun createClock() = Clock.systemDefaultZone()

    private fun createTaskCategoryDetailViewModel(
        initDatabase: (FakeDatabase) -> Unit,
    ): TaskCategoryDetailViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        return TaskCategoryDetailViewModel(
            createSavedStateHandle(),
            FakeTaskCategoryRepository(db),
            FakeTaskRepository(db, clock),
            clock,
        )
    }
}
