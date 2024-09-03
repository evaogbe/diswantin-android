package io.github.evaogbe.diswantin.task.ui

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.github.evaogbe.diswantin.task.data.Task
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
import java.time.LocalDate
import java.time.LocalTime
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
            val clock = createClock()
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isTrue()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDateInput = null,
                    deadlineTimeInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                    clock = clock,
                )
            )
            assertThat(viewModel.nameInput).isEmpty()
        }

    @Test
    fun `initializes for edit with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTask().copy(
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = LocalTime.parse("17:00"),
                recurring = true,
            )
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.isNew).isFalse()
            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDateInput = LocalDate.parse("2024-08-22"),
                    deadlineTimeInput = LocalTime.parse("17:00"),
                    scheduledAtInput = null,
                    recurringInput = true,
                    hasSaveError = false,
                    clock = clock,
                )
            )
            assertThat(viewModel.nameInput).isEqualTo(task.name)
        }

    @Test
    fun `uiState emits failure when repository throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val clock = createClock()
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            taskRepository.setThrows(taskRepository::getById, true)

            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock)

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
            val clock =
                Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDateInput = null,
                    deadlineTimeInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                    clock = clock,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineDateInput(LocalDate.parse("2024-08-22"))
            viewModel.updateDeadlineTimeInput(LocalTime.parse("17:00"))
            viewModel.updateRecurringInput(true)
            viewModel.saveTask()

            val task = taskRepository.tasks.single()
            assertThat(task).isEqualToIgnoringGivenProperties(
                Task(
                    createdAt = Instant.parse("2024-08-22T08:00:00Z"),
                    name = name,
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = LocalTime.parse("17:00"),
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
            val clock = createClock()
            val taskRepository = FakeTaskRepository()
            val viewModel = TaskFormViewModel(SavedStateHandle(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::create, true)
            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDateInput = null,
                    deadlineTimeInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = true,
                    clock = clock,
                )
            )
        }

    @Test
    fun `saveTask updates task with taskId`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val task = genTask().copy(
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = LocalTime.parse("21:00"),
            )
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDateInput = LocalDate.parse("2024-08-22"),
                    deadlineTimeInput = LocalTime.parse("21:00"),
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = false,
                    clock = clock,
                )
            )

            viewModel.updateNameInput(name)
            viewModel.updateDeadlineDateInput(null)
            viewModel.updateDeadlineTimeInput(null)
            viewModel.updateScheduledAtInput(
                ZonedDateTime.parse("2024-08-22T17:00:00-04:00[America/New_York]")
            )
            viewModel.saveTask()

            assertThat(taskRepository.tasks).containsExactlyInAnyOrder(
                task.copy(
                    name = name,
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledAt = Instant.parse("2024-08-22T21:00:00Z"),
                )
            )
            assertThat(viewModel.uiState.value).isEqualTo(TaskFormUiState.Saved)
        }

    @Test
    fun `saveTask shows error message when update throws`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
            val clock = createClock()
            val task = genTask()
            val taskRepository = FakeTaskRepository.withTasks(task)
            val viewModel =
                TaskFormViewModel(createSavedStateHandleForEdit(), taskRepository, clock)

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect()
            }

            taskRepository.setThrows(taskRepository::update, true)
            viewModel.updateNameInput(name)
            viewModel.saveTask()

            assertThat(viewModel.uiState.value).isEqualTo(
                TaskFormUiState.Success(
                    deadlineDateInput = null,
                    deadlineTimeInput = null,
                    scheduledAtInput = null,
                    recurringInput = false,
                    hasSaveError = true,
                    clock = clock,
                )
            )
        }

    private fun createClock() =
        Clock.fixed(Instant.parse("2024-08-22T08:00:00Z"), ZoneId.of("America/New_York"))

    private fun genTask() = Task(
        id = 1L,
        createdAt = faker.random.randomPastDate().toInstant(),
        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
    )

    private fun createSavedStateHandleForEdit() =
        SavedStateHandle(mapOf(Destination.ID_KEY to 1L))

}
