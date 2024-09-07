package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import io.github.evaogbe.diswantin.R
import io.github.evaogbe.diswantin.data.Result
import io.github.evaogbe.diswantin.task.data.EditTaskForm
import io.github.evaogbe.diswantin.task.data.PathUpdateType
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskDetail
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentTaskViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `uiState fetches current task from repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )

            taskRepository.update(
                EditTaskForm(
                    name = name,
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = task1.deadlineTime,
                    scheduledDate = task1.scheduledDate,
                    scheduledTime = task1.scheduledTime,
                    recurring = task1.recurring,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                )
            )

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1.copy(name = name),
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState emits failure when fetch current task fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTasks(1).single()
            val taskRepository = FakeTaskRepository.withTasks(task)
            taskRepository.setThrows(taskRepository::getCurrentTask, true)

            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(CurrentTaskUiState.Failure)
        }

    @Test
    fun `uiState enables skip with multiple tasks`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )
        }

    @Test
    fun `uiState disables skip with single task`() = runTest(mainDispatcherRule.testDispatcher) {
        val task = genTasks(1).single()
        val taskRepository = FakeTaskRepository.withTasks(task)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertThat(viewModel.uiState.value).isEqualTo(
            CurrentTaskUiState.Present(
                currentTask = task,
                canSkip = false,
                parentTaskOptions = Result.Success(persistentListOf()),
                userMessage = null,
            )
        )
    }

    @Test
    fun `uiState disables skip when fetch task count fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            taskRepository.setThrows(taskRepository::getCount, true)

            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = false,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )
        }

    @Test
    fun `searchTasks fetches parentTaskOptions`() = runTest(mainDispatcherRule.testDispatcher) {
        val query = loremFaker.verbs.base()
        val tasks = generateSequence(
            Task(
                id = 1L,
                createdAt = faker.random.randomPastDate().toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        ) {
            Task(
                id = it.id + 1L,
                createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1))
                    .toInstant(),
                name = "$query ${loremFaker.lorem.words()}",
            )
        }.take(3).toList()
        val taskRepository = FakeTaskRepository.withTasks(tasks)
        val viewModel = createCurrentTaskViewModel(taskRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.searchTasks(query)

        assertThat(viewModel.uiState.value).isEqualTo(
            CurrentTaskUiState.Present(
                currentTask = tasks[0],
                canSkip = true,
                parentTaskOptions = Result.Success(persistentListOf(tasks[1], tasks[2])),
                userMessage = null,
            )
        )
    }

    @Test
    fun `searchTasks shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val query = loremFaker.verbs.base()
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::search, true)
            viewModel.searchTasks(query)

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = true,
                    parentTaskOptions = Result.Failure,
                    userMessage = null,
                )
            )
        }

    @Test
    fun `selectParentTask adds parent task to current task`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )

            viewModel.selectParentTask(task2)

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task2,
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )
        }

    @Test
    fun `selectParentTask shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::addParent, true)
            viewModel.selectParentTask(task2)

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = R.string.current_task_add_parent_error,
                )
            )
        }

    @Test
    fun `markCurrentTaskDone sets current task doneAt`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val (task1, task2) = genTasks(2)
            val taskRepository = FakeTaskRepository.withTasks(task1, task2)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task1,
                    canSkip = true,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )

            viewModel.markCurrentTaskDone()

            assertThat(taskRepository.getTaskDetailById(task1.id).first())
                .isNotNull()
                .prop(TaskDetail::doneAt)
                .isNotNull()
            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task2,
                    canSkip = false,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = null,
                )
            )
        }

    @Test
    fun `markCurrentTaskDone shows error message when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTasks(1).single()
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel = createCurrentTaskViewModel(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value)
                .isEqualTo(
                    CurrentTaskUiState.Present(
                        currentTask = task,
                        canSkip = false,
                        parentTaskOptions = Result.Success(persistentListOf()),
                        userMessage = null,
                    )
                )

            taskRepository.setThrows(taskRepository::markDone, true)
            viewModel.markCurrentTaskDone()

            assertThat(viewModel.uiState.value).isEqualTo(
                CurrentTaskUiState.Present(
                    currentTask = task,
                    canSkip = false,
                    parentTaskOptions = Result.Success(persistentListOf()),
                    userMessage = R.string.current_task_mark_done_error
                )
            )
        }

    private fun genTasks(count: Int) = generateSequence(
        Task(
            id = 1L,
            createdAt = faker.random.randomPastDate().toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    ) {
        Task(
            id = it.id + 1L,
            createdAt = faker.random.randomPastDate(min = it.createdAt.plusMillis(1)).toInstant(),
            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
        )
    }.take(count).toList()

    private fun createCurrentTaskViewModel(taskRepository: FakeTaskRepository) =
        CurrentTaskViewModel(taskRepository, Clock.systemDefaultZone())
}
