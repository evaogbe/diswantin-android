package io.github.evaogbe.diswantin.activity.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ActivityDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun displaysActivity() {
        val activity = genActivity().copy(dueAt = Instant.parse("2024-08-23T21:00:00Z"))
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T21:00:00Z"), ZoneId.of("America/New_York"))
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
            activityRepository,
            clock
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(
                    onPopBackStack = {},
                    onEditActivity = {},
                    onSelectChainItem = {},
                    activityDetailViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(activity.name).assertIsDisplayed()
        composeTestRule.onNodeWithText("Friday, August 23, 2024 at 5:00 PM").assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_whenUiFailure() {
        val activityRepository = FakeActivityRepository()
        activityRepository.setThrows(activityRepository::getById, true)

        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to 1L)),
            activityRepository,
            Clock.systemDefaultZone()
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(
                    onPopBackStack = {},
                    onEditActivity = {},
                    onSelectChainItem = {},
                    activityDetailViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.activity_detail_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun popsBackStack_whenActivityRemoved() {
        val activity = genActivity()
        var onPopBackStackCalled = false
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
            activityRepository,
            Clock.systemDefaultZone()
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(
                    onPopBackStack = { onPopBackStackCalled = true },
                    onEditActivity = {},
                    onSelectChainItem = {},
                    activityDetailViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()

        composeTestRule.onNodeWithTag(PendingLayoutTestTag).assertIsDisplayed()
        assertThat(onPopBackStackCalled).isTrue()
    }

    @Test
    fun displaysErrorMessage_whenRemoveActivityFailed() {
        val activity = genActivity()
        val activityRepository = FakeActivityRepository(activity)
        val viewModel = ActivityDetailViewModel(
            SavedStateHandle(mapOf(Destination.ActivityDetail.ID_KEY to activity.id)),
            activityRepository,
            Clock.systemDefaultZone()
        )

        composeTestRule.setContent {
            DiswantinTheme {
                ActivityDetailScreen(
                    onPopBackStack = {},
                    onEditActivity = {},
                    onSelectChainItem = {},
                    activityDetailViewModel = viewModel
                )
            }
        }

        activityRepository.setThrows(activityRepository::remove, true)
        composeTestRule.onNodeWithContentDescription(stringResource(R.string.more_actions_button))
            .performClick()
        composeTestRule.onNodeWithText(stringResource(R.string.delete_button)).performClick()

        composeTestRule.onNodeWithText(stringResource(R.string.activity_detail_delete_error))
            .assertIsDisplayed()
    }

    private fun genActivity() = Activity(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
    )
}
