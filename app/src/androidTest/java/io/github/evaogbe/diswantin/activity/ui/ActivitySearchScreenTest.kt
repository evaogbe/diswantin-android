package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performTextInput
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalTestApi::class)
class ActivitySearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysSearchResults_withSearchResults() {
        val query = loremFaker.verbs.base()
        val activities = List(3) {
            faker.randomClass.randomClassInstance<Activity> {
                typeGenerator<Instant> { faker.random.randomPastDate().toInstant() }
                typeGenerator<String> { "$query ${loremFaker.lorem.unique.words()}" }
            }
        }
        val activityRepository = FakeActivityRepository(activities)
        val viewModel = ActivitySearchViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivitySearchScreen(
                    popBackStack = {},
                    navigateToActivityDetail = {},
                    activitySearchViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.search_activities_placeholder),
            useUnmergedTree = true
        ).onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(hasText(activities[0].name))
        composeTestRule.onNodeWithText(activities[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(activities[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(activities[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutSearchResults() {
        val query = loremFaker.verbs.base()
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivitySearchViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivitySearchScreen(
                    popBackStack = {},
                    navigateToActivityDetail = {},
                    activitySearchViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(
            stringResource(R.string.search_activities_placeholder),
            useUnmergedTree = true
        ).onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(
            hasText(stringResource(R.string.search_results_empty))
        )
    }

    @Test
    fun displayErrorMessage_whenUiFailure() {
        val query = loremFaker.verbs.base()
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivitySearchViewModel(activityRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                ActivitySearchScreen(
                    popBackStack = {},
                    navigateToActivityDetail = {},
                    activitySearchViewModel = viewModel
                )
            }
        }

        activityRepository.setThrows(activityRepository::search, true)
        composeTestRule.onNodeWithText(
            stringResource(R.string.search_activities_placeholder),
            useUnmergedTree = true
        ).onParent()
            .performTextInput(query)

        composeTestRule.waitUntilExactlyOneExists(
            hasText(stringResource(R.string.search_activities_error))
        )
    }
}
