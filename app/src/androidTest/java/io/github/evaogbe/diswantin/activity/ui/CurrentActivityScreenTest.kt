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
                    onNavigateToSearch = {},
                    onAddActivity = {},
                    onEditActivity = {},
                    onAdviceClick = {},
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
                    onNavigateToSearch = {},
                    onAddActivity = {},
                    onEditActivity = {},
                    onAdviceClick = {},
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
                    onNavigateToSearch = {},
                    onAddActivity = {},
                    onEditActivity = {},
                    onAdviceClick = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.current_activity_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun callsOnAddActivity_whenFabClicked() {
        var onAddActivityCalled = false
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    onNavigateToSearch = {},
                    onAddActivity = { onAddActivityCalled = true },
                    onEditActivity = {},
                    onAdviceClick = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_activity_button))
            .performClick()

        assertThat(onAddActivityCalled).isTrue()
    }

    @Test
    fun callsOnAddActivity_whenAddActivityClicked() {
        var onAddActivityCalled = false
        val activityRepository = FakeActivityRepository()
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    onNavigateToSearch = {},
                    onAddActivity = { onAddActivityCalled = true },
                    onEditActivity = {},
                    onAdviceClick = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.add_activity_button))
            .performClick()

        assertThat(onAddActivityCalled).isTrue()
    }

    @Test
    fun callsOnEditActivity_whenEditClicked() {
        var onEditActivityCalled = false
        val activity = genActivities(1).single()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    onNavigateToSearch = {},
                    onAddActivity = {},
                    onEditActivity = { id ->
                        assertThat(id).isEqualTo(activity.id)
                        onEditActivityCalled = true
                    },
                    onAdviceClick = {},
                    currentActivityViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.edit_button))
            .performClick()

        assertThat(onEditActivityCalled).isTrue()
    }

    @Test
    fun displaysNextActivityName_whenRemoveClicked() {
        val (activity1, activity2) = genActivities(2)
        val activityRepository = FakeActivityRepository(activity1, activity2)
        val viewModel = CurrentActivityViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                CurrentActivityScreen(
                    onNavigateToSearch = {},
                    onAddActivity = {},
                    onEditActivity = {},
                    onAdviceClick = {},
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
                    onNavigateToSearch = {},
                    onAddActivity = {},
                    onEditActivity = {},
                    onAdviceClick = {},
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
