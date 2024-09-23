package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.NavArguments
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCategoryDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches task category by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val categoryWithTasks = genTaskCategoryWithTasks()
        val taskCategoryRepository =
            FakeTaskCategoryRepository.withCategories(categoryWithTasks)
        val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryDetailUiState.Success(
                category = categoryWithTasks.category,
                tasks = categoryWithTasks.tasks.map { it.toTaskItemUiState() }.toImmutableList(),
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState emits failure when task category not found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskCategoryRepository = FakeTaskCategoryRepository()
            val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Failure)
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskCategoryRepository = spyk<FakeTaskCategoryRepository>()
            every { taskCategoryRepository.getCategoryWithTasksById(any()) } returns flow {
                throw RuntimeException("Test")
            }

            val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Failure)
        }

    @Test
    fun `deleteCategory sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val categoryWithTasks = genTaskCategoryWithTasks()
        val taskCategoryRepository =
            FakeTaskCategoryRepository.withCategories(categoryWithTasks)
        val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            TaskCategoryDetailUiState.Success(
                category = categoryWithTasks.category,
                tasks = categoryWithTasks.tasks.map { it.toTaskItemUiState() }.toImmutableList(),
                userMessage = null,
            )
        )

        viewModel.deleteCategory()

        assertThat(taskCategoryRepository.taskCategories).isEmpty()
        assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryDetailUiState.Deleted)
    }

    @Test
    fun `deleteCategory shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val categoryWithTasks = genTaskCategoryWithTasks()
            val taskCategoryRepository =
                spyk(FakeTaskCategoryRepository.withCategories(categoryWithTasks))
            coEvery { taskCategoryRepository.delete(any()) } throws RuntimeException("Test")

            val viewModel = createTaskCategoryDetailViewModel(taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            viewModel.deleteCategory()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskCategoryDetailUiState.Success(
                    category = categoryWithTasks.category,
                    tasks = categoryWithTasks.tasks.map { it.toTaskItemUiState() }
                        .toImmutableList(),
                    userMessage = R.string.task_category_detail_delete_error,
                )
            )
        }

    private fun Task.toTaskItemUiState() = TaskItemUiState(id = id, name = name, isDone = false)

    private fun genTaskCategoryWithTasks() = TaskCategoryWithTasks(
        TaskCategory(id = 1L, name = loremFaker.lorem.words()),
        generateSequence(
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
        }.take(faker.random.nextInt(bound = 5)).toList(),
    )

    private fun createTaskCategoryDetailViewModel(
        taskCategoryRepository: FakeTaskCategoryRepository,
    ) =
        TaskCategoryDetailViewModel(
            SavedStateHandle(mapOf(NavArguments.ID_KEY to 1L)),
            taskCategoryRepository,
            Clock.systemDefaultZone(),
        )
}
