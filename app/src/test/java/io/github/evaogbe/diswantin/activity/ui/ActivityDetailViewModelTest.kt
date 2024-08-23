package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches activity by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = genActivity()
        val clock = Clock.systemDefaultZone()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
            activityRepository,
            clock
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            ActivityDetailUiState.Success(
                activity = activity,
                userMessage = null,
                clock = clock
            )
        )
    }

    @Test
    fun `uiState is failure when repository throws`() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = genActivity()
        val clock = Clock.systemDefaultZone()
        val activityRepository = FakeActivityRepository(activity)
        activityRepository.setThrows(activityRepository::findById, true)

        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
            activityRepository,
            clock
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(ActivityDetailUiState.Failure)
    }

    @Test
    fun `removeActivity sets uiState to removed`() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = genActivity()
        val clock = Clock.systemDefaultZone()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
            activityRepository,
            clock
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            ActivityDetailUiState.Success(
                activity = activity,
                userMessage = null,
                clock = clock
            )
        )

        viewModel.removeActivity()

        assertThat(viewModel.uiState.value).isEqualTo(ActivityDetailUiState.Removed)
    }

    @Test
    fun `removeActivity shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activity = genActivity()
            val clock = Clock.systemDefaultZone()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = ActivityDetailViewModel(
                SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
                activityRepository,
                clock
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            activityRepository.setThrows(activityRepository::remove, true)
            viewModel.removeActivity()

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityDetailUiState.Success(
                    activity = activity,
                    userMessage = R.string.activity_detail_delete_error,
                    clock = clock
                )
            )
        }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )
}
