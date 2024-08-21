package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
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
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
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
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_empty))
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
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun navigatesToNewActivityForm_whenFabClicked() {
        var navigateToNewActivityFormClicked = false
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = { navigateToNewActivityFormClicked = true },
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_activity_button))
            .performClick()

        assertThat(navigateToNewActivityFormClicked).isTrue()
    }

    @Test
    fun navigatesToNewActivityForm_whenAddActivityClicked() {
        var navigateToNewActivityFormClicked = false
        val activityRepository = FakeActivityRepository()
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {
                        navigateToNewActivityFormClicked = true
                    },
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_activity_button))
            .performClick()

        assertThat(navigateToNewActivityFormClicked).isTrue()
    }

    @Test
    fun navigatesToActivityForm_whenEditClicked() {
        var navigateToEditActivityFormClicked = false
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = { id ->
                        assertThat(id).isEqualTo(activity.id)
                        navigateToEditActivityFormClicked = true
                    },
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()

        assertThat(navigateToEditActivityFormClicked).isTrue()
    }

    @Test
    fun displaysNextActivityName_whenSkipClicked() {
        val (activity1, activity2) = genActivities(2)
        val activityRepository = FakeActivityRepository(activity1, activity2)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(activity1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()

        composeTestRule.onNodeWithText(activity2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenSkipFailed() {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        activityRepository.setThrows(activityRepository::update, true)
        composeTestRule.onNodeWithText(stringResource(R.string.skip_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_skip_error))
            .assertIsDisplayed()
    }

    @Test
    fun displaysNextActivityName_whenRemoveClicked() {
        val (activity1, activity2) = genActivities(2)
        val activityRepository = FakeActivityRepository(activity1, activity2)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(activity1.name).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringResource(R.string.remove_button)).performClick()

        composeTestRule.onNodeWithText(activity2.name).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenRemoveFailed() {
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    navigateToActivitySearch = {},
                    navigateToNewActivityForm = {},
                    navigateToEditActivityForm = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        activityRepository.setThrows(activityRepository::remove, true)
        composeTestRule.onNodeWithText(stringResource(R.string.remove_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_remove_error))
            .assertIsDisplayed()
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
