package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityRepository
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.stringResource
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class ActivityDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysActivityName() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityDetailViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(popBackStack = {}, activityDetailViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(activity.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenUiFailure() {
        val activityRepository = FakeActivityRepository()
        activityRepository.setThrows(activityRepository::findById, true)

        val viewModel = createActivityDetailViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(popBackStack = {}, activityDetailViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.activity_detail_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun displaysSavedActivityName_whenSaveClicked() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityDetailViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(popBackStack = {}, activityDetailViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_edit))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_edit))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }


    @Test
    fun displaysErrorMessage_withUpdateActivityError() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityDetailViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(popBackStack = {}, activityDetailViewModel = viewModel)
            }
        }

        activityRepository.setThrows(activityRepository::update, true)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_save_error_edit))
            .assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun popsBackStack_whenDeleteClicked() {
        var popBackStackClicked = false
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = createActivityDetailViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(
                    popBackStack = { popBackStackClicked = true },
                    activityDetailViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()
        composeTestRule.waitUntilDoesNotExist(hasText(activity.name))

        assertThat(popBackStackClicked).isTrue()
    }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )

    private fun createActivityDetailViewModel(
        activityRepository: ActivityRepository
    ): ActivityDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to 1L))
        val activityDetailViewModel = ActivityDetailViewModel(savedStateHandle, activityRepository)
        return activityDetailViewModel
    }
}
