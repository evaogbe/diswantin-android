package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryWithTasks
import io.github.evaogbe.diswantin.testing.FakeTaskCategoryRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCategoryListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches task categories`() = runTest(mainDispatcherRule.testDispatcher) {
        val categories = List(faker.random.nextInt(bound = 5)) {
            TaskCategory(id = it + 1L, name = "$it. ${loremFaker.lorem.words()}")
        }
        val taskCategoryRepository = FakeTaskCategoryRepository.withCategories(categories.map {
            TaskCategoryWithTasks(it, emptyList())
        })
        val viewModel = TaskCategoryListViewModel(taskCategoryRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value)
            .isEqualTo(TaskCategoryListUiState.Success(categories = categories.toImmutableList()))
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskCategoryRepository = FakeTaskCategoryRepository()
            taskCategoryRepository.setThrows(taskCategoryRepository::categoryListStream, true)

            val viewModel = TaskCategoryListViewModel(taskCategoryRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskCategoryListUiState.Failure)
        }
}
