package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.task.data.Tag
import io.github.evaogbe.diswantin.task.data.TagRepository
import io.github.evaogbe.diswantin.task.data.TaggedTask
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeDatabase
import io.github.evaogbe.diswantin.testing.FakeTagRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.snackbar.UserMessage
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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

@OptIn(ExperimentalCoroutinesApi::class)
class TagFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new without tagId`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createTagFormViewModelForNew()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

        advanceUntilIdle()

        assertThat(viewModel.isNew).isTrue()
        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = "",
                newTasks = persistentListOf(),
                isEditing = true,
                taskOptions = persistentListOf(),
                changed = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `initializes for edit with tagId`() = runTest(mainDispatcherRule.testDispatcher) {
        val tag = genTag()
        val tasks = genTasks()
        val viewModel = createTagFormViewModelForEdit { db ->
            tasks.forEach(db::insertTask)
            db.insertTag(tag, tasks.map(Task::id).toSet())
        }

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.isNew).isFalse()
        assertThat(viewModel.existingTaskPagingData.asSnapshot()).isEqualTo(
            tasks.map { it.toTaggedTask(isTagged = true) },
        )
        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = tag.name,
                newTasks = persistentListOf(),
                isEditing = false,
                taskOptions = persistentListOf(),
                changed = false,
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val exception = RuntimeException("Test")
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val tagRepository = spyk(FakeTagRepository(db))
            every { tagRepository.getById(any()) } returns flow {
                throw exception
            }

            val viewModel = createTagFormViewModelForEdit(tagRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Failure(exception))
        }

    @Test
    fun `uiState emits failure when tag for tag id not found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createTagFormViewModelForEdit {}

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isInstanceOf<TagFormUiState.Failure>()
        }

    @Test
    fun `uiState emits changed when new form and name changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val viewModel = createTagFormViewModelForNew()

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = "",
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )

            viewModel.updateName(name)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when new form and tasks added`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tasks = genTasks()
            val viewModel = createTagFormViewModelForNew { db ->
                tasks.forEach(db::insertTask)
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = "",
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )

            tasks.forEach {
                viewModel.addTask(it.toTaggedTask(isTagged = false))
            }
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = "",
                    newTasks = tasks.map { it.toTaggedTask(isTagged = false) }.toImmutableList(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and name changed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = genTag()
            val tasks = genTasks()
            val viewModel = createTagFormViewModelForEdit { db ->
                tasks.forEach(db::insertTask)
                db.insertTag(tag, tasks.map(Task::id).toSet())
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = tag.name,
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )

            viewModel.updateName("")
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = "",
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and task added`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = genTag()
            val tasks = genTasks()
            val newTask = tasks.last().toTaggedTask(isTagged = false)
            val viewModel = createTagFormViewModelForEdit { db ->
                tasks.forEach(db::insertTask)
                db.insertTag(tag, tasks.dropLast(1).map(Task::id).toSet())
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = tag.name,
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )

            viewModel.addTask(newTask)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = tag.name,
                    newTasks = persistentListOf(newTask),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits changed when edit form and task removed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val tag = genTag()
            val tasks = genTasks()
            val removedTask = tasks.last().toTaggedTask(isTagged = true)
            val viewModel = createTagFormViewModelForEdit { db ->
                tasks.forEach(db::insertTask)
                db.insertTag(tag, tasks.map(Task::id).toSet())
            }

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = tag.name,
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = null,
                )
            )
            assertThat(viewModel.existingTaskPagingData.asSnapshot()).isEqualTo(
                tasks.map { it.toTaggedTask(isTagged = true) },
            )

            viewModel.removeTask(removedTask)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = tag.name,
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )
            assertThat(viewModel.existingTaskPagingData.asSnapshot()).isEqualTo(
                tasks.dropLast(1).map { it.toTaggedTask(isTagged = true) },
            )
        }

    @Test
    fun `searchTasks sets task options`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val tasks = List(3) {
            Task(
                id = it + 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        }
        val tag = genTag()
        val viewModel = createTagFormViewModelForEdit { db ->
            tasks.forEach(db::insertTask)
            db.insertTag(tag, setOf(tasks[0].id))
        }

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

        advanceUntilIdle()
        viewModel.searchTasks(query)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = tag.name,
                newTasks = persistentListOf(),
                isEditing = false,
                taskOptions = persistentListOf(
                    tasks[1].toTaggedTask(isTagged = false),
                    tasks[2].toTaggedTask(isTagged = false),
                ),
                changed = false,
                userMessage = null,
            )
        )

        viewModel.removeTask(tasks[0].toTaggedTask(isTagged = true))
        advanceUntilIdle()
        viewModel.searchTasks(query)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = tag.name,
                newTasks = persistentListOf(),
                isEditing = false,
                taskOptions = persistentListOf(
                    tasks[0].toTaggedTask(isTagged = true),
                    tasks[1].toTaggedTask(isTagged = false),
                    tasks[2].toTaggedTask(isTagged = false),
                ),
                changed = true,
                userMessage = null,
            )
        )

        viewModel.addTask(tasks[1].toTaggedTask(isTagged = false))
        advanceUntilIdle()
        viewModel.searchTasks(query)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = tag.name,
                newTasks = persistentListOf(tasks[1].toTaggedTask(isTagged = false)),
                isEditing = false,
                taskOptions = persistentListOf(
                    tasks[0].toTaggedTask(isTagged = true),
                    tasks[2].toTaggedTask(isTagged = false),
                ),
                changed = true,
                userMessage = null,
            )
        )

        viewModel.addTask(tasks[0].toTaggedTask(isTagged = true))
        advanceUntilIdle()
        viewModel.searchTasks(query)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = tag.name,
                newTasks = persistentListOf(
                    tasks[1].toTaggedTask(isTagged = false),
                    tasks[0].toTaggedTask(isTagged = true),
                ),
                isEditing = false,
                taskOptions = persistentListOf(tasks[2].toTaggedTask(isTagged = false)),
                changed = true,
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val db = FakeDatabase()
            val taskRepository = spyk(FakeTaskRepository(db))
            every { taskRepository.searchTaggedTasks(any(), any(), any()) } returns flow {
                throw RuntimeException("Test")
            }

            val tagRepository = FakeTagRepository(db)
            val viewModel = createTagFormViewModelForNew(tagRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()
            viewModel.searchTasks(query)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = "",
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = false,
                    userMessage = UserMessage.String(R.string.search_task_options_error),
                )
            )
        }

    @Test
    fun `saveTag creates tag without tagId`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = loremFaker.lorem.words()
        val tasks = genTasks()
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
        }
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = FakeTagRepository(db)
        val viewModel = createTagFormViewModelForNew(tagRepository, taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = "",
                newTasks = persistentListOf(),
                isEditing = true,
                taskOptions = persistentListOf(),
                changed = false,
                userMessage = null,
            )
        )

        viewModel.updateName(name)
        tasks.forEach {
            viewModel.addTask(it.toTaggedTask(isTagged = false))
        }
        advanceUntilIdle()
        viewModel.saveTag()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Saved)

        val tag = tagRepository.tags.single()
        assertThat(tag.name).isEqualTo(name)
        assertThat(
            taskRepository.getTaggedTasksByTagId(tag.id).asSnapshot()
        ).isEqualTo(tasks.map { it.toTaggedTask(isTagged = true) })
    }

    @Test
    fun `saveTag shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val db = FakeDatabase()
            val taskRepository = FakeTaskRepository(db)
            val tagRepository = spyk(FakeTagRepository(db))
            coEvery { tagRepository.create(any()) } throws RuntimeException("Test")

            val viewModel = createTagFormViewModelForNew(tagRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()
            viewModel.updateName(name)
            advanceUntilIdle()
            viewModel.saveTag()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(),
                    isEditing = true,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = UserMessage.String(R.string.tag_form_save_error_new),
                )
            )
        }

    @Test
    fun `saveTag updates tag with tagId`() = runTest(mainDispatcherRule.testDispatcher) {
        val name = loremFaker.lorem.words()
        val tag = genTag()
        val tasks = genTasks()
        val db = FakeDatabase().apply {
            tasks.forEach(::insertTask)
            insertTag(tag, tasks.map(Task::id).toSet())
        }
        val taskRepository = FakeTaskRepository(db)
        val tagRepository = FakeTagRepository(db)
        val viewModel = createTagFormViewModelForEdit(tagRepository, taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.existingTaskPagingData.asSnapshot()).isEqualTo(
            tasks.map { it.toTaggedTask(isTagged = true) },
        )
        assertThat(viewModel.uiState.value).isEqualTo(
            TagFormUiState.Success(
                name = tag.name,
                newTasks = persistentListOf(),
                isEditing = false,
                taskOptions = persistentListOf(),
                changed = false,
                userMessage = null,
            )
        )

        viewModel.updateName(name)
        advanceUntilIdle()
        viewModel.saveTag()
        advanceUntilIdle()

        val updatedTag = tagRepository.getById(tag.id).first()
        val updatedTasks = taskRepository.getTaggedTasksByTagId(tag.id).asSnapshot()
        assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Saved)
        assertThat(updatedTag).isEqualTo(tag.copy(name = name))
        assertThat(updatedTasks).isEqualTo(tasks.map { it.toTaggedTask(isTagged = true) })
    }

    @Test
    fun `saveTag shows error message with update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = loremFaker.lorem.words()
            val tag = genTag()
            val tasks = genTasks()
            val db = FakeDatabase().apply {
                tasks.forEach(::insertTask)
                insertTag(tag, tasks.map(Task::id).toSet())
            }
            val taskRepository = FakeTaskRepository(db)
            val tagRepository = spyk(FakeTagRepository(db))
            coEvery { tagRepository.update(any()) } throws RuntimeException("Test")

            val viewModel = createTagFormViewModelForEdit(tagRepository, taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TagFormUiState.Pending)

            advanceUntilIdle()
            viewModel.updateName(name)
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = null,
                )
            )

            viewModel.saveTag()
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(
                TagFormUiState.Success(
                    name = name,
                    newTasks = persistentListOf(),
                    isEditing = false,
                    taskOptions = persistentListOf(),
                    changed = true,
                    userMessage = UserMessage.String(R.string.tag_form_save_error_edit),
                )
            )
        }

    private fun genTag() = Tag(id = 1L, name = loremFaker.lorem.words())

    private fun genTasks() = List(3) {
        Task(
            id = it + 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${it + 1}. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    }

    private fun Task.toTaggedTask(isTagged: Boolean) =
        TaggedTask(id = id, name = name, isTagged = isTagged)

    private fun createSavedStateHandleForNew(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every {
            savedStateHandle.toRoute<TagFormRoute>()
        } returns TagFormRoute.new(name = null)
        return savedStateHandle
    }

    private fun createSavedStateHandleForEdit(): SavedStateHandle {
        mockkStatic("androidx.navigation.SavedStateHandleKt")
        val savedStateHandle = mockk<SavedStateHandle>()
        every {
            savedStateHandle.toRoute<TagFormRoute>()
        } returns TagFormRoute.edit(id = 1L)
        return savedStateHandle
    }

    private fun createTagFormViewModelForNew(
        initDatabase: (FakeDatabase) -> Unit = {}
    ): TagFormViewModel {
        val db = FakeDatabase().also(initDatabase)
        return createTagFormViewModelForNew(
            FakeTagRepository(db),
            FakeTaskRepository(db),
        )
    }

    private fun createTagFormViewModelForNew(
        tagRepository: TagRepository,
        taskRepository: TaskRepository,
    ) = TagFormViewModel(
        createSavedStateHandleForNew(),
        tagRepository,
        taskRepository,
    )

    private fun createTagFormViewModelForEdit(
        initDatabase: (FakeDatabase) -> Unit
    ): TagFormViewModel {
        val db = FakeDatabase().also(initDatabase)
        return createTagFormViewModelForEdit(
            FakeTagRepository(db),
            FakeTaskRepository(db),
        )
    }

    private fun createTagFormViewModelForEdit(
        tagRepository: TagRepository,
        taskRepository: TaskRepository,
    ) = TagFormViewModel(
        createSavedStateHandleForEdit(),
        tagRepository,
        taskRepository,
    )
}
