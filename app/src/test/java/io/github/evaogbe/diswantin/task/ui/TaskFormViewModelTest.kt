package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskRepository
import io.github.evaogbe.diswantin.testing.FakeTaskRepository
import io.github.evaogbe.diswantin.testing.MainDispatcherRule
import io.github.evaogbe.diswantin.ui.navigation.Destination
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
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
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new without taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskRepository = FakeTaskRepository()
            val viewModel = createTaskFormViewModelForNew(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isTrue()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                )
            )
            assertThat(viewModel.nameInput).isEmpty()
        }

    @Test
    fun `initializes for edit with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val deadline = Instant.parse("2024-08-22T21:00:00Z")
            val task = genTask().copy(deadline = deadline, recurring = true)
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel = createTaskFormViewModelForEdit(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isInstanceOf<TaskFormUiState.Success>().all {
                isEqualToIgnoringGivenProperties(
                    TaskFormUiState.Success(
                        deadlineInput = null,
                        scheduledAtInput = null,
                        recurringInput = true,
                        hasSaveError = false,
                    ),
                    TaskFormUiState.Success::deadlineInput
                )
                prop(TaskFormUiState.Success::deadlineInput)
                    .isNotNull()
                    .transform { it.toInstant() }
                    .isEqualTo(deadline)
            }
            assertThat(viewModel.nameInput).isEqualTo(task.name)
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            taskRepository.setThrows(taskRepository::getById, true)

            val viewModel = createTaskFormViewModelForEdit(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure)
        }

    @Test
    fun `saveTask creates task without taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(
                SavedStateHandle(),
                taskRepository,
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York")),
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineInput(
                ZonedDateTime.parse("2024-08-22T17:00-04:00[America/New_York]")
            )
            viewModel.updateRecurringInput(true)
            viewModel.saveTask()

            val task = taskRepository.tasks.single()
            assertThat(task).isEqualToIgnoringGivenProperties(
                Task(
                    createdAt = Instant.parse("2024-08-22T08:00:00Z"),
                    name = name,
                    deadline = Instant.parse("2024-08-22T21:00:00Z"),
                    scheduledAt = null,
                    recurring = true,
                    listId = null,
                ),
                Task::id,
            )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when create throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val taskRepository = FakeTaskRepository()
            val viewModel = createTaskFormViewModelForNew(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::create, true)
            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = true,
                )
            )
        }

    @Test
    fun `saveTask updates task with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask().copy(deadline = Instant.parse("2024-08-22T21:00:00Z"))
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel = TaskFormViewModel(
                SavedStateHandle(mapOf(Destination.EditTaskForm.ID_KEY to 1L)),
                taskRepository,
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York")),
            )

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = ZonedDateTime.parse(
                        "2024-08-22T17:00:00-04:00[America/New_York]"
                    ),
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineInput(null)
            viewModel.updateScheduledAtInput(
                ZonedDateTime.parse("2024-08-22T17:00:00-04:00[America/New_York]")
            )
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadline = null,
                    scheduledAt = Instant.parse("2024-08-22T21:00:00Z"),
                )
            )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel = createTaskFormViewModelForEdit(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::update, true)
            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = true,
                )
            )
        }

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
    )

    private fun createTaskFormViewModelForNew(taskRepository: TaskRepository) =
        TaskFormViewModel(SavedStateHandle(), taskRepository, Clock.systemDefaultZone())

    private fun createTaskFormViewModelForEdit(taskRepository: TaskRepository) =
        TaskFormViewModel(
            SavedStateHandle(mapOf(Destination.EditTaskForm.ID_KEY to 1L)),
            taskRepository,
            Clock.systemDefaultZone()
        )
}
