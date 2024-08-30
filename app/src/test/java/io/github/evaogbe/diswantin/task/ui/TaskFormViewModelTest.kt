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
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun `initializes for new when taskId null`() =
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
                    hasSaveError = false,
                )
            )
            assertThat(viewModel.nameInput).isEmpty()
        }

    @Test
    fun `initializes for edit when taskId present`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val deadline = Instant.parse("2024-08-22T21:00:00Z")
            val task = genTask().copy(deadline = deadline)
            val taskRepository = FakeTaskRepository(task)
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
    fun `uiState is failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val task = genTask()
            val taskRepository = FakeTaskRepository(task)
            taskRepository.setThrows(taskRepository::getById, true)

            val viewModel = createTaskFormViewModelForEdit(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Failure)
        }

    @Test
    fun `saveTask creates task when taskId null`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val taskRepository = FakeTaskRepository()
            val viewModel = createTaskFormViewModelForNew(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(" ")
            viewModel.saveTask()

            assertThat(taskRepository.tasks).isEmpty()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineInput(
                ZonedDateTime.parse("2024-08-22T17:00-04:00[America/New_York]")
            )
            viewModel.saveTask()

            val task = taskRepository.tasks.single()
            assertThat(task.name).isEqualTo(name)
            assertThat(task.deadline).isEqualTo(Instant.parse("2024-08-22T21:00:00Z"))
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
                    hasSaveError = true,
                )
            )
        }

    @Test
    fun `saveTask updates task when taskId present`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val taskRepository = FakeTaskRepository(task)
            val viewModel = createTaskFormViewModelForEdit(taskRepository)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(" ")
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(task)
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineInput = null,
                    scheduledAtInput = null,
                    hasSaveError = false,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineInput(
                ZonedDateTime.parse("2024-08-22T17:00:00-04:00[America/New_York]")
            )
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadline = Instant.parse("2024-08-22T21:00:00Z")
                )
            )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val task = genTask()
            val taskRepository = FakeTaskRepository(task)
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
