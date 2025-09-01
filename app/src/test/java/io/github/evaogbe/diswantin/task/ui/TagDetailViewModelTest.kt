package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TagDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches tag by id`() = runTest(mainDispatcherRule.testDispatcher) {
        val tag = genTag()
        val tasks = genTasks()
        val viewModel = createTagDetailViewModel({ db ->
            tasks.forEach(db::insertTask)
            db.insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
        })

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

        advanceUntilIdle()

        val taskSummaries = tasks.map { it.toTaskSummaryUiState() }
        assertThat(viewModel.uiState.value).isEqualTo(
            TagDetailUiState.Success(
                tag = tag,
                userMessage = null,
            )
        )
        assertThat(viewModel.taskSummaryPagingData.asSnapshot()).isEqualTo(taskSummaries)
    }

    @Test
    fun `uiState emits failure when tag not found`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createTagDetailViewModel({})

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf<TagDetailUiState.Failure>()
            .prop(TagDetailUiState.Failure::exception).isInstanceOf<NullPointerException>()
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val tag = genTag()
            val tasks = genTasks()
            val viewModel = createTagDetailViewModel(
                initDatabase = { db ->
                    tasks.forEach(db::insertTask)
                    db.insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
                },
                initTagRepositorySpy = { repository ->
                    every { repository.getById(any()) } returns flow {
                        throw exception
                    }
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagDetailUiState.Failure(exception)
            )
        }

    @Test
    fun `saveTag updates tag`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = loremFaker.lorem.words()
        val tag = genTag()
        val clock = createClock()
        val db = FakeDatabase().apply {
            insertTag(tag = tag)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TagDetailViewModel(
            createSavedStateHandle(),
            tagRepository,
            taskRepository,
            clock,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TagDetailUiState.Success(
                tag = tag,
                userMessage = null,
            )
        )

        viewModel.saveTag(name)
        advanceUntilIdle()

        val updatedTag = tagRepository.getById(tag.id).first()
        assertThat(viewModel.uiState.value).isEqualTo(
            TagDetailUiState.Success(
                tag = tag.copy(name = name),
                userMessage = null,
            )
        )
        assertThat(updatedTag).isNotNull()
            .isEqualToIgnoringGivenProperties(Tag(name = name), Tag::id)
    }

    @Test
    fun `saveTag shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val tag = genTag()
            val viewModel = createTagDetailViewModel(
                initDatabase = { db -> db.insertTag(tag = tag) },
                initTagRepositorySpy = { repository ->
                    coEvery { repository.update(any()) } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagDetailUiState.Success(
                    tag = tag,
                    userMessage = null,
                )
            )

            viewModel.saveTag(name)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagDetailUiState.Success(
                    tag = tag,
                    userMessage = TagDetailUserMessage.EditError,
                )
            )
        }

    @Test
    fun `deleteTag sets uiState to deleted`() = runTest(mainDispatcherRule.testDispatcher) {
        val tag = genTag()
        val tasks = genTasks()
        val clock = createClock()
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
        }
        val taskRepository = FakeTaskRepository(db, clock)
        val tagRepository = FakeTagRepository(db)
        val viewModel = TagDetailViewModel(
            createSavedStateHandle(),
            tagRepository,
            taskRepository,
            clock,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

        advanceUntilIdle()

        val taskSummaries = tasks.map { it.toTaskSummaryUiState() }
        assertThat(viewModel.uiState.value).isEqualTo(
            TagDetailUiState.Success(
                tag = tag,
                userMessage = null,
            )
        )
        assertThat(viewModel.taskSummaryPagingData.asSnapshot()).isEqualTo(taskSummaries)

        viewModel.deleteTag()
        advanceUntilIdle()

        assertThat(tagRepository.tags).isEmpty()
        assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Deleted)
    }

    @Test
    fun `deleteTag shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = genTag()
            val tasks = genTasks()
            val viewModel = createTagDetailViewModel(
                initDatabase = { db ->
                    tasks.forEach(db::insertTask)
                    db.insertTag(tag = tag, taskIds = tasks.map { it.id }.toSet())
                },
                initTagRepositorySpy = { repository ->
                    coEvery { repository.delete(any()) } throws RuntimeException("Test")
                },
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagDetailUiState.Pending)

            advanceUntilIdle()

            val taskSummaries = tasks.map { it.toTaskSummaryUiState() }
            assertThat(viewModel.uiState.value).isEqualTo(
                TagDetailUiState.Success(
                    tag = tag,
                    userMessage = null,
                )
            )
            assertThat(viewModel.taskSummaryPagingData.asSnapshot()).isEqualTo(taskSummaries)

            viewModel.deleteTag()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagDetailUiState.Success(
                    tag = tag,
                    userMessage = TagDetailUserMessage.DeleteError,
                )
            )
            assertThat(viewModel.taskSummaryPagingData.asSnapshot()).isEqualTo(taskSummaries)
        }

    private fun Task.toTaskSummaryUiState() =
        TaskSummaryUiState(id = id, name = name, isDone = false)

    private fun genTag() = Tag(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks() = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    }.take(faker.random.nextInt(bound = 5)).toList()

    private fun createSavedStateHandle(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every {
            savedStateHandle.toRoute<TagDetailRoute>()
        } returns TagDetailRoute(id = 1L)
        return savedStateHandle
    }

    private fun createClock() = Clock.systemDefaultZone()

    private fun createTagDetailViewModel(
        initDatabase: (FakeDatabase) -> Unit,
        initTagRepositorySpy: ((TagRepository) -> Unit)? = null,
    ): TagDetailViewModel {
        val clock = createClock()
        val db = FakeDatabase().also(initDatabase)
        val tagRepository = if (initTagRepositorySpy == null) {
            FakeTagRepository(db)
        } else {
            spyk(FakeTagRepository(db)).also(initTagRepositorySpy)
        }
        val taskRepository = FakeTaskRepository(db, clock)
        return TagDetailViewModel(
            createSavedStateHandle(),
            tagRepository,
            taskRepository,
            clock,
        )
    }
}
