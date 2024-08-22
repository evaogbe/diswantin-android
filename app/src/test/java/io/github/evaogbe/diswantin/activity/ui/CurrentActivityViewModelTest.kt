package io.github.evaogbe.diswantin.activity.ui

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentActivityViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches current activity from repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (activity1, activity2) = genActivities(2)
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activityRepository = FakeActivityRepository(activity1, activity2)
            val viewModel = CurrentActivityViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentActivityUiState.Present(currentActivity = activity1))

            val updatedActivity1 = activity1.copy(name = name)
            activityRepository.update(updatedActivity1)

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentActivityUiState.Present(currentActivity = updatedActivity1))
        }

    @Test
    fun `uiState is failure when repository throws`() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        activityRepository.setThrows(activityRepository::currentActivityStream, true)

        val viewModel = CurrentActivityViewModel(activityRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(CurrentActivityUiState.Failure)
    }

    @Test
    fun `removeCurrentActivity removes current activity from repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (activity1, activity2) = genActivities(2)
            val activityRepository = FakeActivityRepository(activity1, activity2)
            val viewModel = CurrentActivityViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentActivityUiState.Present(currentActivity = activity1))

            viewModel.removeCurrentActivity()

            assertThat(activityRepository.activities).doesNotContain(activity1)
            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentActivityUiState.Present(currentActivity = activity2))
        }

    @Test
    fun `removeCurrentActivity does nothing without current activity`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activityRepository = FakeActivityRepository()
            val viewModel = CurrentActivityViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(CurrentActivityUiState.Empty)

            viewModel.removeCurrentActivity()

            assertThat(viewModel.uiState.value).isEqualTo(CurrentActivityUiState.Empty)
            assertThat(viewModel.userMessage).isNull()
        }

    @Test
    fun `removeCurrentActivity shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activity = genActivities(1).single()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = CurrentActivityViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentActivityUiState.Present(currentActivity = activity))

            activityRepository.setThrows(activityRepository::remove, true)
            viewModel.removeCurrentActivity()

            assertThat(viewModel.uiState.value)
                .isEqualTo(CurrentActivityUiState.Present(currentActivity = activity))
            assertThat(viewModel.userMessage).isEqualTo(R.string.current_activity_remove_error)
            assertThat(activityRepository.activities).contains(activity)
        }

    private fun genActivities(count: Int) = generateSequence(
        Activity(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        )
    ) {
        Activity(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1)).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        )
    }.take(count).toList()
}
