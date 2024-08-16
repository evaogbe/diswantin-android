package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.SavedStateHandle
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isDataClassEqualTo
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches activity by id`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(ActivityDetailUiState.Success(activity = activity))
        }

    @Test
    fun `uiState is failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activityRepository = FakeActivityRepository()
            activityRepository.setThrows(activityRepository::findById, true)

            val viewModel = createViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(ActivityDetailUiState.Failure)
        }

    @Test
    fun `updateActivity sets uiState to updated activity with name input`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(ActivityDetailUiState.Success(activity = activity))

            viewModel.updateActivity(" ")

            assertThat(activityRepository.activities).contains(activity)
            assertThat(viewModel.uiState.value)
                .isEqualTo(ActivityDetailUiState.Success(activity = activity))

            viewModel.updateActivity(name)

            val updatedActivity = activityRepository.activities.single { it.id == activity.id }
            assertThat(updatedActivity).isDataClassEqualTo(activity.copy(name = name))
            assertThat(viewModel.uiState.value)
                .isEqualTo(
                    ActivityDetailUiState.Success(
                        activity = updatedActivity,
                        saveResult = Result.success(Unit),
                        userMessage = R.string.activity_saved_message_edit
                    )
                )
        }

    @Test
    fun `updateActivity sets saveResult to failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(ActivityDetailUiState.Success(activity = activity))

            activityRepository.setThrows(activityRepository::update, true)
            viewModel.updateActivity(name)

            assertThat(activityRepository.activities).contains(activity)
            assertThat(viewModel.uiState.value)
                .isInstanceOf<ActivityDetailUiState.Success>()
                .all {
                    isEqualToIgnoringGivenProperties(
                        ActivityDetailUiState.Success(activity = activity),
                        ActivityDetailUiState.Success::saveResult
                    )
                    prop(ActivityDetailUiState.Success::saveResult)
                        .isNotNull()
                        .transform { it.isFailure }
                        .isTrue()
                }
        }

    @Test
    fun `removeActivity sets uiState to removed`() = runTest(mainDispatcherRule.testDispatcher) {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createViewModel(activityRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value)
            .isEqualTo(ActivityDetailUiState.Success(activity = activity))

        viewModel.removeActivity()

        assertThat(activityRepository.activities).doesNotContain(activity)
        assertThat(viewModel.uiState.value).isEqualTo(ActivityDetailUiState.Removed)
    }

    @Test
    fun `removeActivity shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(ActivityDetailUiState.Success(activity = activity))

            activityRepository.setThrows(activityRepository::remove, true)
            viewModel.removeActivity()

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityDetailUiState.Success(
                    activity = activity,
                    userMessage = R.string.activity_detail_delete_error
                )
            )
        }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createViewModel(activityRepository: ActivityRepository): ActivityDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to 1L))
        return ActivityDetailViewModel(savedStateHandle, activityRepository)
    }
}
