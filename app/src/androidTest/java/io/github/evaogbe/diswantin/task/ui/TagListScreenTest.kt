package io.github.evaogbe.diswantin.task.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.stringResource
import io.github.evaogbe.diswantin.ui.theme.DiswantinTheme
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class TagListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val loremFaker = LoremFaker()

    @Test
    fun displaysTagNames_withTags() {
        val tags = List(3) {
            Tag(id = it + 1L, name = loremFaker.lorem.unique.words())
        }
        val db = FakeDatabase().apply {
            tags.forEach(::insertTag)
        }
        val tagRepository = FakeTagRepository(db)
        val viewModel = TagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onAddTag = {},
                    onSelectTag = {},
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
        val viewModel = TagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onAddTag = {},
                    onSelectTag = {},
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

        val viewModel = TagListViewModel(tagRepository)

        composeTestRule.setContent {
            DiswantinTheme {
                TagListScreen(
                    onAddTag = {},
                    onSelectTag = {},
                    tagListViewModel = viewModel,
                )
            }
        }

        composeTestRule.onNodeWithText(stringResource(R.string.tag_list_fetch_error))
            .assertIsDisplayed()
    }
}
