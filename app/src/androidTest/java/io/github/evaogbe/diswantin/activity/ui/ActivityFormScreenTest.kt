package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.stringResource
import io.github.evaogbe.diswantin.ui.components.PendingLayoutTestTag
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class ActivityFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysNewActivityTopBar_whenNew() {
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(SavedStateHandle(), activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(popBackStack = {}, activityFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_title_new))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .assertDoesNotExist()
    }

    @Test
    fun displaysEditActivityTopBar_whenEdit() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(popBackStack = {}, activityFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_title_edit))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenUiFailure() {
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to 1L)),
            activityRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(popBackStack = {}, activityFormViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenActivityCreated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var popBackStackClicked = false
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(SavedStateHandle(), activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(
                    popBackStack = { popBackStackClicked = true },
                    activityFormViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.save_button))
            .performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(popBackStackClicked).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForNew() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivityFormViewModel(SavedStateHandle(), activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(popBackStack = {}, activityFormViewModel = viewModel)
            }
        }

        activityRepository.setThrows(activityRepository::create, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.save_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_save_error_new))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenActivityUpdated() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        var popBackStackClicked = false
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(
                    popBackStack = { popBackStackClicked = true },
                    activityFormViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.save_button))
            .performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(popBackStackClicked).isTrue()
    }

    @Test
    fun displaysErrorMessage_withSaveErrorForEdit() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(popBackStack = {}, activityFormViewModel = viewModel)
            }
        }

        activityRepository.setThrows(activityRepository::update, true)
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.save_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_save_error_edit))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenActivityRemoved() {
        var popBackStackClicked = false
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(
                    popBackStack = { popBackStackClicked = true },
                    activityFormViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(popBackStackClicked).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenRemoveFailed() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityFormViewModel(
            SavedStateHandle(mapOf(Destination.EditActivityForm.ID_KEY to activity.id)),
            activityRepository
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityFormScreen(popBackStack = {}, activityFormViewModel = viewModel)
            }
        }

        activityRepository.setThrows(activityRepository::remove, true)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_delete_error))
            .assertIsDisplayed()
    }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )
}
