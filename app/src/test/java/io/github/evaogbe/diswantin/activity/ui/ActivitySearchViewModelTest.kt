package io.github.evaogbe.diswantin.activity.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.testing.FakeActivityRepository
import io.github.evaogbe.diswantin.testutils.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.regex.Pattern

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitySearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val faker = Faker()

    @Test
    fun `uiState fetches search results matching the query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val blankQuery = faker.string.regexify(""" *""")
            val query = faker.string.regexify("""\S+""")
            val activities = List(faker.random.nextInt(bound = 5)) {
                faker.randomClass.randomClassInstance<Activity> {
                    typeGenerator<Instant> { faker.random.randomPastDate().toInstant() }
                    typeGenerator<String> {
                        faker.string.regexify(
                            """([^\r\n]* )?${Pattern.quote(query)}[^\r\n]*"""
                                .toRegex(RegexOption.IGNORE_CASE)
                        )
                    }
                }
            }
            val activityRepository = FakeActivityRepository(activities)
            val viewModel = ActivitySearchViewModel(activityRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(ActivitySearchUiState.Initial)

            viewModel.searchActivities(blankQuery)

            assertThat(viewModel.uiState.value).isEqualTo(ActivitySearchUiState.Initial)

            viewModel.searchActivities(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                ActivitySearchUiState.Success(searchResults = activities.toPersistentList())
            )

            viewModel.searchActivities(blankQuery)

            assertThat(viewModel.uiState.value)
                .isEqualTo(ActivitySearchUiState.Success(searchResults = persistentListOf()))
        }

    @Test
    fun `uiState is failure when repository throws`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = faker.string.regexify("""\S+""")
        val activityRepository = FakeActivityRepository()
        val viewModel = ActivitySearchViewModel(activityRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        activityRepository.setThrows(activityRepository::search, true)
        viewModel.searchActivities(query)

        assertThat(viewModel.uiState.value).isEqualTo(ActivitySearchUiState.Failure)
    }
}
