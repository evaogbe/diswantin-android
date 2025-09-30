package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.matches
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.snackbar.SnackbarState
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.Clock

class TagListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val faker = Faker()

    private val loremFaker = LoremFaker()

    @Test
    fun displaysTagNames_withTags() {
        val tags = List(3) {
            val createdAt = faker.random.randomPastDate().toInstant()
            Tag(
                id = it + 1L,
                name = loremFaker.lorem.unique.words(),
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
        val db = FakeDatabase().apply {
            tags.forEach(::insertTag)
        }
        val tagRepository = FakeTagRepository(db)
        val viewModel = createTagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onSelectTag = {},
                    showSnackbar = {},
                    fabClicked = false,
                    fabClickHandled = {},
                    tagListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(tags[0].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tags[1].name).assertIsDisplayed()
        composeTestRule.onNodeWithText(tags[2].name).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyMessage_withoutTags() {
        val tagRepository = FakeTagRepository()
        val viewModel = createTagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onSelectTag = {},
                    showSnackbar = {},
                    fabClicked = false,
                    fabClickHandled = {},
                    tagListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.tag_list_empty)).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage_withFailureUi() {
        val tagRepository = spyk<FakeTagRepository>()
        every { tagRepository.tagPagingData } returns flowOf(
            PagingData.from(
                emptyList(),
                LoadStates(
                    refresh = LoadState.Error(RuntimeException("Test")),
                    prepend = LoadState.NotLoading(endOfPaginationReached = false),
                    append = LoadState.NotLoading(endOfPaginationReached = false),
                ),
            )
        )

        val viewModel = createTagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onSelectTag = {},
                    showSnackbar = {},
                    fabClicked = false,
                    fabClickHandled = {},
                    tagListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.tag_list_fetch_error))
            .assertIsDisplayed()
    }

    @Test
    fun handlesFab_whenTagSaved() {
        var fabClicked = true
        val name = loremFaker.lorem.words()
        val tagRepository = FakeTagRepository()
        val viewModel = createTagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onSelectTag = {},
                    showSnackbar = {},
                    fabClicked = fabClicked,
                    fabClickHandled = { fabClicked = false },
                    tagListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        assertThat(fabClicked).isFalse()
        assertThat(tagRepository.tags).isNotEmpty()
    }

    @Test
    fun displaysErrorMessage_withSaveError() {
        var snackbarState: SnackbarState? = null
        val name = loremFaker.lorem.words()
        val tagRepository = spyk<FakeTagRepository>()
        coEvery { tagRepository.create(any()) } throws RuntimeException("Test")

        val viewModel = createTagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onSelectTag = {},
                    showSnackbar = { snackbarState = it },
                    fabClicked = true,
                    fabClickHandled = {},
                    tagListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.name_label), useUnmergedTree = true)
            .onParent().performTextInput(name)
        composeTestRule.onNodeWithText(stringResource(R.string.save_button)).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.waitUntil {
            snackbarState.matches(stringResource(R.string.tag_form_save_error_new))
        }
    }

    private fun createTagListViewModel(tagRepository: TagRepository): TagListViewModel {
        val clock = Clock.systemDefaultZone()
        return TagListViewModel(tagRepository, clock)
    }
}
