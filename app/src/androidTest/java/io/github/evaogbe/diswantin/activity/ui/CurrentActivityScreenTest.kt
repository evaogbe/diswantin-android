package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test

class CurrentActivityScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysCurrentActivityName_withCurrentActivity() {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(activity.name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutCurrentActivity() {
        val activityRepository = FakeActivityRepository()
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_empty_message))
            .assertIsDisplayed()
    }

    @Test
    fun displayErrorMessage_whenUiFailure() {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        activityRepository.setThrows(activityRepository::currentActivityStream, true)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun displaysActivityForm_whenFabClicked() {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_activity_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_new))
            .assertIsDisplayed()
    }

    @Test
    fun displaysActivityForm_whenAddActivityClicked() {
        val activityRepository = FakeActivityRepository()
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_activity_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_new))
            .assertIsDisplayed()
    }

    @Test
    fun displaysActivityForm_whenEditClicked() {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_edit))
            .assertIsDisplayed()
    }

    @Test
    fun displaysCreatedActivityName_withoutCurrentActivity_whenSaveClicked() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.add_activity_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_new))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_new))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.activity_saved_message_new))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withCreateActivityError() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activityRepository = FakeActivityRepository()
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        activityRepository.setThrows(activityRepository::create, true)
        composeTestRule.onNodeWithText(stringResource(R.string.add_activity_button)).performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_save_error_new))
            .assertIsDisplayed()
    }

    @Test
    fun displaysUpdatedActivityName_whenActivityFormForEditSaveClicked() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_edit))
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(activity.name).assertCountEquals(2)

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent()
            .performTextReplacement(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_form_heading_edit))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(stringResource(R.string.activity_saved_message_edit))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withUpdateActivityError() {
        val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
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

    @Test
    fun displaysNextActivityName_whenSkipClicked() {
        val (activity1, activity2) = genActivities(2)
        val activityRepository = FakeActivityRepository(activity1, activity2)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(activity1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()

        composeTestRule.onNodeWithText(activity2.name).assertIsDisplayed()
    }

    @Test
    fun displaysNextActivityName_whenRemoveClicked() {
        val (activity1, activity2) = genActivities(2)
        val activityRepository = FakeActivityRepository(activity1, activity2)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(onSearch = {}, currentActivityViewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText(activity1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.remove_button)).performClick()

        composeTestRule.onNodeWithText(activity2.name).assertIsDisplayed()
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
