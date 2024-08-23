package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime

class ActivityFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initialize sets uiState to success when activityId null`() {
        val activityRepository = FakeActivityRepository()
        val viewModel = createActivityFormViewModelForNew(activityRepository)

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Pending)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))
        assertThat(viewModel.nameInput).isEmpty()
        assertThat(viewModel.dueAtInput).isNull()
    }

    @Test
    fun `initialize fetches activity by id when activityId present`() {
        val dueAt = Instant.parse("2024-08-22T21:00:00Z")
        val activity = genActivity().copy(dueAt = dueAt)
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityFormViewModelForEdit(activityRepository)

        assertThat(viewModel.isNew).isFalse()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Pending)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))
        assertThat(viewModel.nameInput).isEqualTo(activity.name)
        assertThat(viewModel.dueAtInput).isNotNull().transform { it.toInstant() }.isEqualTo(dueAt)
    }

    @Test
    fun `initialize sets uiState to failure when repository throws`() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityFormViewModelForEdit(activityRepository)

        activityRepository.setThrows(activityRepository::findById, true)
        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Failure)
    }

    @Test
    fun `saveActivity creates activity when activityId null`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = createActivityFormViewModelForNew(activityRepository)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(" ")
        viewModel.saveActivity()

        assertThat(activityRepository.activities).isEmpty()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(name)
        viewModel.updateDueAtInput(
            ZonedDateTime.parse("2024-08-22T17:00-04:00[America/New_York]")
        )
        viewModel.saveActivity()

        val activity = activityRepository.activities.single()
        assertThat(activity.name).isEqualTo(name)
        assertThat(activity.dueAt).isEqualTo(Instant.parse("2024-08-22T21:00:00Z"))
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Saved)
    }

    @Test
    fun `saveActivity shows error message when create throws`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = createActivityFormViewModelForNew(activityRepository)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        activityRepository.setThrows(activityRepository::create, true)
        viewModel.updateNameInput(name)
        viewModel.saveActivity()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = true))
    }

    @Test
    fun `saveActivity updates activity when activityId present`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityFormViewModelForEdit(activityRepository)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(" ")
        viewModel.saveActivity()

        assertThat(activityRepository.activities).containsExactly(activity)
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(name)
        viewModel.updateDueAtInput(
            ZonedDateTime.parse("2024-08-22T17:00:00-04:00[America/New_York]")
        )
        viewModel.saveActivity()

        assertThat(activityRepository.activities).containsExactly(
            activity.copy(
                name = name,
                dueAt = Instant.parse("2024-08-22T21:00:00Z")
            )
        )
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Saved)
    }

    @Test
    fun `saveActivity shows error message when update throws`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityFormViewModelForEdit(activityRepository)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        activityRepository.setThrows(activityRepository::update, true)
        viewModel.updateNameInput(name)
        viewModel.saveActivity()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = true))
    }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
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
