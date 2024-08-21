package io.github.evaogbe.diswantin.activity.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class ActivityFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initialize sets uiState to success when activityId null`() {
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(SavedStateHandle(), activityRepository)

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Pending)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))
        assertThat(viewModel.nameInput).isEmpty()
    }

    @Test
    fun `initialize fetches activity by id when activityId present`() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        assertThat(viewModel.isNew).isFalse()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Pending)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))
        assertThat(viewModel.nameInput).isEqualTo(activity.name)
    }

    @Test
    fun `initialize sets uiState to failure when repository throws`() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        activityRepository.setThrows(activityRepository::findById, true)
        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Failure)
    }

    @Test
    fun `saveActivity creates activity when activityId null`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(SavedStateHandle(), activityRepository)

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(" ")
        viewModel.saveActivity()

        assertThat(activityRepository.activities).isEmpty()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(name)
        viewModel.saveActivity()

        val activity = activityRepository.activities.single()
        assertThat(activity.name).isEqualTo(name)
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Saved)
    }

    @Test
    fun `saveActivity shows error message when create throws`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(SavedStateHandle(), activityRepository)

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
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(" ")
        viewModel.saveActivity()

        assertThat(activityRepository.activities).containsExactly(activity)
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.updateNameInput(name)
        viewModel.saveActivity()

        assertThat(activityRepository.activities).containsExactly(activity.copy(name = name))
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Saved)
    }

    @Test
    fun `saveActivity shows error message when update throws`() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        activityRepository.setThrows(activityRepository::update, true)
        viewModel.updateNameInput(name)
        viewModel.saveActivity()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = true))
    }

    @Test
    fun `removeActivity sets uiState to removed`() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        viewModel.removeActivity()

        assertThat(activityRepository.activities).isEmpty()
        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Removed)
        assertThat(viewModel.userMessage).isNull()
    }

    @Test
    fun `removeActivity shows error message when remove throws`() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        viewModel.initialize()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))

        activityRepository.setThrows(activityRepository::remove, true)
        viewModel.removeActivity()

        assertThat(viewModel.uiState).isEqualTo(ActivityFormUiState.Success(hasSaveError = false))
        assertThat(viewModel.userMessage).isEqualTo(R.string.activity_form_delete_error)
    }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )
}
