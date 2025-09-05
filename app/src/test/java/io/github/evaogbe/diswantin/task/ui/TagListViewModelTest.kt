package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isNull
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TagListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    @Test
    fun `saveTag creates tag`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = loremFaker.lorem.words()
        val now = Instant.parse("2024-08-23T17:00:00Z")
        val clock = Clock.fixed(now, ZoneId.of("America/New_York"))
        val tagRepository = FakeTagRepository()
        val viewModel = TagListViewModel(tagRepository, clock)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.userMessage.collect()
        }

        viewModel.saveTag(name)

        val tag = tagRepository.tags.single()
        assertThat(tag).isEqualToIgnoringGivenProperties(
            Tag(name = name, createdAt = now, updatedAt = now),
            Tag::id,
        )
        assertThat(viewModel.userMessage.value).isNull()
    }

    @Test
    fun `saveTag shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val clock = createClock()
            val tagRepository = spyk<FakeTagRepository>()
            coEvery { tagRepository.create(any()) } throws RuntimeException("Test")

            val viewModel = TagListViewModel(tagRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.userMessage.collect()
            }

            viewModel.saveTag(name)

            assertThat(viewModel.userMessage.value).isEqualTo(TagListUserMessage.CreateError)
        }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
}
