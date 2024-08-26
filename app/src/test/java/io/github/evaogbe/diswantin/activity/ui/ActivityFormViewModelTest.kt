package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.SavedStateHandle
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
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
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initialize sets uiState to success when activityId null`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activityRepository = FakeActivityRepository()
            val viewModel = createActivityFormViewModelForNew(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isTrue()
            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )
            assertThat(viewModel.nameInput).isEmpty()
        }

    @Test
    fun `initialize fetches activity by id when activityId present`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val dueAt = Instant.parse("2024-08-22T21:00:00Z")
            val activity = genActivity().copy(dueAt = dueAt)
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createActivityFormViewModelForEdit(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isInstanceOf<ActivityFormUiState.Success>().all {
                isEqualToIgnoringGivenProperties(
                    ActivityFormUiState.Success(
                        dueAtInput = null,
                        scheduledAtInput = null,
                        canUpdatePrevActivity = false,
                        prevActivity = null,
                        prevActivityOptions = emptyList(),
                        hasSaveError = false,
                    ),
                    ActivityFormUiState.Success::dueAtInput
                )
                prop(ActivityFormUiState.Success::dueAtInput)
                    .isNotNull()
                    .transform { it.toInstant() }
                    .isEqualTo(dueAt)
            }
            assertThat(viewModel.nameInput).isEqualTo(activity.name)
        }

    @Test
    fun `initialize sets uiState to failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            activityRepository.setThrows(activityRepository::getById, true)

            val viewModel = createActivityFormViewModelForEdit(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(ActivityFormUiState.Failure)
        }

    @Test
    fun `saveActivity creates activity when activityId null`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activityRepository = FakeActivityRepository()
            val viewModel = createActivityFormViewModelForNew(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(" ")
            viewModel.saveActivity()

            assertThat(activityRepository.activities).isEmpty()
            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDueAtInput(
                ZonedDateTime.parse("2024-08-22T17:00-04:00[America/New_York]")
            )
            viewModel.saveActivity()

            val activity = activityRepository.activities.single()
            assertThat(activity.name).isEqualTo(name)
            assertThat(activity.dueAt).isEqualTo(Instant.parse("2024-08-22T21:00:00Z"))
            assertThat(viewModel.uiState.value).isEqualTo(ActivityFormUiState.Saved)
        }

    @Test
    fun `saveActivity shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activityRepository = FakeActivityRepository()
            val viewModel = createActivityFormViewModelForNew(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )

            activityRepository.setThrows(activityRepository::create, true)
            viewModel.updateNameInput(name)
            viewModel.saveActivity()

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = true,
                )
            )
        }

    @Test
    fun `saveActivity updates activity when activityId present`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createActivityFormViewModelForEdit(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(" ")
            viewModel.saveActivity()

            assertThat(activityRepository.activities).containsExactlyInAnyOrder(activity)
            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDueAtInput(
                ZonedDateTime.parse("2024-08-22T17:00:00-04:00[America/New_York]")
            )
            viewModel.saveActivity()

            assertThat(activityRepository.activities).containsExactlyInAnyOrder(
                activity.copy(
                    name = name,
                    dueAt = Instant.parse("2024-08-22T21:00:00Z")
                )
            )
            assertThat(viewModel.uiState.value).isEqualTo(ActivityFormUiState.Saved)
        }

    @Test
    fun `saveActivity shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val activity = genActivity()
            val activityRepository = FakeActivityRepository(activity)
            val viewModel = createActivityFormViewModelForEdit(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = false,
                )
            )

            activityRepository.setThrows(activityRepository::update, true)
            viewModel.updateNameInput(name)
            viewModel.saveActivity()

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivityFormUiState.Success(
                    dueAtInput = null,
                    scheduledAtInput = null,
                    canUpdatePrevActivity = false,
                    prevActivity = null,
                    prevActivityOptions = emptyList(),
                    hasSaveError = true,
                )
            )
        }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
    )

    private fun createActivityFormViewModelForNew(activityRepository: ActivityRepository) =
        ActivityFormViewModel(SavedStateHandle(), activityRepository, Clock.systemDefaultZone())

    private fun createActivityFormViewModelForEdit(activityRepository: ActivityRepository) =
        ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to 1L)),
            activityRepository,
            Clock.systemDefaultZone()
        )
}
