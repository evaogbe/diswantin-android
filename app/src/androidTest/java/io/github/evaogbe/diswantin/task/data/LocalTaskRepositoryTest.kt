package io.github.evaogbe.diswantin.task.data

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isDataClassEqualTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.evaogbe.diswantin.app.data.DiswantinDatabase
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LocalTaskRepositoryTest {
    private lateinit var db: DiswantinDatabase

    private val loremFaker = LoremFaker()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), DiswantinDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getCurrentTask_emitsTasksInOrder() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task1 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Created at ascending
            var task2 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = Clock.offset(clock, Duration.ofMillis(-1)),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Start after date nulls first
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = task2.deadlineTime,
                    startAfterDate = LocalDate.parse("2024-08-22"),
                    startAfterTime = task2.startAfterTime,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = task2.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Start after date ascending
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = task1.deadlineTime,
                    startAfterDate = LocalDate.parse("2024-08-23"),
                    startAfterTime = task1.startAfterTime,
                    categoryId = task1.categoryId,
                    scheduledDate = task1.scheduledDate,
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Deadline date before start after date
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = LocalDate.parse("2024-08-23"),
                    deadlineTime = task1.deadlineTime,
                    startAfterDate = task1.startAfterDate,
                    startAfterTime = task1.startAfterTime,
                    categoryId = task1.categoryId,
                    scheduledDate = task1.scheduledDate,
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Deadline date ascending
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = LocalDate.parse("2024-08-22"),
                    deadlineTime = task2.deadlineTime,
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = task2.startAfterTime,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = task2.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Scheduled date before deadline date
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = null,
                    deadlineTime = task1.deadlineTime,
                    startAfterDate = null,
                    startAfterTime = task1.startAfterTime,
                    categoryId = task1.categoryId,
                    scheduledDate = LocalDate.parse("2024-08-23"),
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Scheduled date ascending
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = null,
                    deadlineTime = task2.deadlineTime,
                    startAfterDate = null,
                    startAfterTime = task2.startAfterTime,
                    categoryId = task2.categoryId,
                    scheduledDate = LocalDate.parse("2024-08-22"),
                    scheduledTime = task2.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Scheduled date before recurring
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = null,
                    deadlineTime = task2.deadlineTime,
                    startAfterDate = null,
                    startAfterTime = task2.startAfterTime,
                    categoryId = task2.categoryId,
                    scheduledDate = null,
                    scheduledTime = task2.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task2.id,
                            start = LocalDate.parse("2024-08-23"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Recurring without start after time before non-recurring
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = task1.deadlineTime,
                    startAfterDate = task1.startAfterDate,
                    startAfterTime = task1.startAfterTime,
                    categoryId = task1.categoryId,
                    scheduledDate = null,
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Non-recurring before recurring with start after time
            var taskRecurrences2 = taskRepository.getTaskRecurrencesByTaskId(task2.id).first()
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = task2.deadlineTime,
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = LocalTime.parse("12:59"),
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = task2.scheduledTime,
                    recurrences = taskRecurrences2,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Start after time ascending
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = task1.deadlineTime,
                    startAfterDate = task1.startAfterDate,
                    startAfterTime = LocalTime.parse("13:00"),
                    categoryId = task1.categoryId,
                    scheduledDate = null,
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Default deadline time for recurring task is max time
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = LocalTime.parse("23:59"),
                    startAfterDate = task1.startAfterDate,
                    startAfterTime = task1.startAfterTime,
                    categoryId = task1.categoryId,
                    scheduledDate = null,
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Deadline time ascending
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = LocalTime.parse("23:58"),
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = task2.startAfterTime,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = task2.scheduledTime,
                    recurrences = taskRecurrences2,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Deadline time before start after time
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = task1.deadlineDate,
                    deadlineTime = LocalTime.parse("23:58"),
                    startAfterDate = task1.startAfterDate,
                    startAfterTime = null,
                    categoryId = task1.categoryId,
                    scheduledDate = task1.scheduledDate,
                    scheduledTime = task1.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Overdue tasks before not due tasks
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = LocalTime.parse("14:00"),
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = LocalTime.parse("13:00"),
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = task2.scheduledTime,
                    recurrences = taskRecurrences2,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Scheduled time before deadline time
            task1 = taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    categoryId = task1.categoryId,
                    scheduledDate = LocalDate.parse("2024-08-23"),
                    scheduledTime = LocalTime.parse("12:59"),
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Scheduled time ascending
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = null,
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = null,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = LocalTime.parse("12:58"),
                    recurrences = taskRecurrences2,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Earlier scheduled date with later scheduled time first
            var task3 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = LocalDate.parse("2024-08-22"),
                    scheduledTime = LocalTime.parse("13:01"),
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task3)

            // Parent task before child task
            task3 = taskRepository.update(
                EditTaskForm(
                    name = task3.name,
                    note = task3.note,
                    deadlineDate = task3.deadlineDate,
                    deadlineTime = task3.deadlineTime,
                    startAfterDate = task3.startAfterDate,
                    startAfterTime = task3.startAfterTime,
                    categoryId = task3.categoryId,
                    scheduledDate = task3.scheduledDate,
                    scheduledTime = task3.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Replace(task2.id),
                    existingTask = task3,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            // Scheduled time ordered by ancestor
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = null,
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = null,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = LocalTime.parse("13:00"),
                    recurrences = taskRecurrences2,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            // Parent not occurring on day does not block child task
            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = task2.deadlineTime,
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = task2.startAfterTime,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = task2.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task2.id,
                            start = LocalDate.parse("2024-08-22"),
                            type = RecurrenceType.Day,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )
            taskRecurrences2 = taskRepository.getTaskRecurrencesByTaskId(task2.id).first()

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task3)

            // Child task not occurring on day does not affect parent task priority
            taskRepository.update(
                EditTaskForm(
                    name = task3.name,
                    note = task3.note,
                    deadlineDate = null,
                    deadlineTime = task3.deadlineTime,
                    startAfterDate = null,
                    startAfterTime = task3.startAfterTime,
                    categoryId = task3.categoryId,
                    scheduledDate = null,
                    scheduledTime = task3.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task3.id,
                            start = LocalDate.parse("2024-08-22"),
                            type = RecurrenceType.Day,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task3,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            task2 = taskRepository.update(
                EditTaskForm(
                    name = task2.name,
                    note = task2.note,
                    deadlineDate = task2.deadlineDate,
                    deadlineTime = null,
                    startAfterDate = task2.startAfterDate,
                    startAfterTime = null,
                    categoryId = task2.categoryId,
                    scheduledDate = task2.scheduledDate,
                    scheduledTime = LocalTime.parse("12:59"),
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task2.id,
                            start = LocalDate.parse("2024-08-22"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task2,
                    existingRecurrences = taskRecurrences2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)
        }
    }

    @Test
    fun getCurrentTask_doesNotEmitTask_whenBeforeScheduled() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = LocalDate.parse("2024-08-24"),
                    scheduledTime = task.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = LocalDate.parse("2024-08-23"),
                    scheduledTime = task.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = LocalTime.parse("13:01"),
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = LocalTime.parse("13:00"),
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = LocalDate.parse("2024-08-22"),
                    scheduledTime = LocalTime.parse("13:01"),
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = null,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = null,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = null,
                    scheduledTime = LocalTime.parse("13:01"),
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-23"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNull()

            val taskRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id).first()
            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = LocalTime.parse("13:00"),
                    recurrences = taskRecurrences,
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRecurrences,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)
        }
    }

    @Test
    fun getCurrentTask_doesNotEmitTask_whenBeforeStartAfter() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = LocalDate.parse("2024-08-24"),
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = LocalDate.parse("2024-08-23"),
                    startAfterTime = null,
                    categoryId = task.categoryId,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = LocalDate.parse("2024-08-23"),
                    startAfterTime = LocalTime.parse("13:01"),
                    categoryId = task.categoryId,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = LocalDate.parse("2024-08-23"),
                    startAfterTime = LocalTime.parse("13:00"),
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = null,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = null,
                    startAfterTime = null,
                    categoryId = task.categoryId,
                    scheduledDate = null,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-24"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNull()

            val taskRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id).first()
            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-23"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRecurrences,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)
        }
    }

    @Test
    fun getCurrentTask_doesNotEmitTask_whenSkipped() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task1 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2024-08-23"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            taskRepository.skip(task1.id)

            assertThat(awaitItem()).isNull()

            val task2 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = task1.id,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)
        }
    }

    @Test
    fun getCurrentTask_emitsFirstUndoneTask() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(
            CurrentTaskParams(
                today = LocalDate.parse("2024-08-23"),
                currentTime = LocalTime.parse("13:00"),
                startOfToday = Instant.parse("2024-08-24T04:00:00Z"),
                endOfToday = ZonedDateTime.parse(
                    "2024-08-24T23:59:59.999-04:00[America/New_York]"
                ),
            ),
        ).test {
            assertThat(awaitItem()).isNull()

            val task1 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            val task2 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task1)

            taskRepository.markDone(task1.id)

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            val task3 = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = task1.categoryId,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Replace(task3.id),
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = task1.categoryId,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task1.id,
                            start = LocalDate.parse("2024-08-23"),
                            type = RecurrenceType.Day,
                            step = 1,
                        )
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task1,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task3)

            taskRepository.update(
                EditTaskForm(
                    name = task1.name,
                    note = task1.note,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = task1.categoryId,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Remove,
                    existingTask = task1,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task1.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            taskRepository.update(
                EditTaskForm(
                    name = task3.name,
                    note = task3.note,
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = task3.categoryId,
                    recurrences = emptyList(),
                    parentUpdateType = PathUpdateType.Replace(task1.id),
                    existingTask = task3,
                    existingRecurrences = emptyList(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task2)

            taskRepository.markDone(task2.id)

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task3)
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2024-08-22"),
                            type = RecurrenceType.Day,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-22"),
                            type = RecurrenceType.Day,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-21"),
                            type = RecurrenceType.Day,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnWeek() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2024-08-16"),
                            type = RecurrenceType.Week,
                            step = 1,
                        ),
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2024-08-15"),
                            type = RecurrenceType.Week,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-15"),
                            type = RecurrenceType.Week,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-16"),
                            type = RecurrenceType.Week,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-16"),
                            type = RecurrenceType.Week,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-08-09"),
                            type = RecurrenceType.Week,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-07-23"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2023-07-23"),
                            type = RecurrenceType.DayOfMonth,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2023-06-23"),
                            type = RecurrenceType.DayOfMonth,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth_whenNonLeapYear() = runTest {
        val now = ZonedDateTime.parse("2023-02-28T00:00-05:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2022-02-28"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            listOf(
                LocalDate.parse("2022-01-31"),
                LocalDate.parse("2022-03-31"),
                LocalDate.parse("2022-04-30"),
                LocalDate.parse("2022-05-31"),
                LocalDate.parse("2022-06-30"),
                LocalDate.parse("2022-07-31"),
                LocalDate.parse("2022-08-31"),
                LocalDate.parse("2022-09-30"),
                LocalDate.parse("2022-10-31"),
                LocalDate.parse("2022-11-30"),
                LocalDate.parse("2022-12-31"),
                LocalDate.parse("2020-02-29"),
            ).forEach { start ->
                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        categoryId = task.categoryId,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = start,
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem(), name = "task starting at $start").isNotNull()
                    .isDataClassEqualTo(task)
            }
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth_whenLeapYear() = runTest {
        val now = ZonedDateTime.parse("2024-02-29T00:00-05:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-01-31"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            listOf(
                LocalDate.parse("2023-03-31"),
                LocalDate.parse("2023-04-30"),
                LocalDate.parse("2023-05-31"),
                LocalDate.parse("2023-06-30"),
                LocalDate.parse("2023-07-31"),
                LocalDate.parse("2023-08-31"),
                LocalDate.parse("2023-09-30"),
                LocalDate.parse("2023-10-31"),
                LocalDate.parse("2023-11-30"),
                LocalDate.parse("2023-12-31"),
                LocalDate.parse("2020-02-29"),
            ).forEach { start ->
                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        categoryId = task.categoryId,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = start,
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem(), name = "task starting at $start").isNotNull()
                    .isDataClassEqualTo(task)
            }
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth_when30Apr() = runTest {
        val now = ZonedDateTime.parse("2024-04-30T00:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-01-31"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            listOf(
                LocalDate.parse("2023-03-31"),
                LocalDate.parse("2023-04-30"),
                LocalDate.parse("2023-05-31"),
                LocalDate.parse("2023-06-30"),
                LocalDate.parse("2023-07-31"),
                LocalDate.parse("2023-08-31"),
                LocalDate.parse("2023-09-30"),
                LocalDate.parse("2023-10-31"),
                LocalDate.parse("2023-11-30"),
                LocalDate.parse("2023-12-31"),
            ).forEach { start ->
                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        categoryId = task.categoryId,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = start,
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem(), name = "task starting at $start").isNotNull()
                    .isDataClassEqualTo(task)
            }
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth_when30Jun() = runTest {
        val now = ZonedDateTime.parse("2024-06-30T00:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-01-31"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            listOf(
                LocalDate.parse("2023-03-31"),
                LocalDate.parse("2023-04-30"),
                LocalDate.parse("2023-05-31"),
                LocalDate.parse("2023-06-30"),
                LocalDate.parse("2023-07-31"),
                LocalDate.parse("2023-08-31"),
                LocalDate.parse("2023-09-30"),
                LocalDate.parse("2023-10-31"),
                LocalDate.parse("2023-11-30"),
                LocalDate.parse("2023-12-31"),
            ).forEach { start ->
                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        categoryId = task.categoryId,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = start,
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem(), name = "task starting at $start").isNotNull()
                    .isDataClassEqualTo(task)
            }
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth_when30Sep() = runTest {
        val now = ZonedDateTime.parse("2024-09-30T00:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-01-31"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            listOf(
                LocalDate.parse("2023-03-31"),
                LocalDate.parse("2023-04-30"),
                LocalDate.parse("2023-05-31"),
                LocalDate.parse("2023-06-30"),
                LocalDate.parse("2023-07-31"),
                LocalDate.parse("2023-08-31"),
                LocalDate.parse("2023-09-30"),
                LocalDate.parse("2023-10-31"),
                LocalDate.parse("2023-11-30"),
                LocalDate.parse("2023-12-31"),
            ).forEach { start ->
                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        categoryId = task.categoryId,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = start,
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem(), name = "task starting at $start").isNotNull()
                    .isDataClassEqualTo(task)
            }
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDayOfMonth_when30Nov() = runTest {
        val now = ZonedDateTime.parse("2024-11-30T00:00-05:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-01-31"),
                            type = RecurrenceType.DayOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            listOf(
                LocalDate.parse("2023-03-31"),
                LocalDate.parse("2023-04-30"),
                LocalDate.parse("2023-05-31"),
                LocalDate.parse("2023-06-30"),
                LocalDate.parse("2023-07-31"),
                LocalDate.parse("2023-08-31"),
                LocalDate.parse("2023-09-30"),
                LocalDate.parse("2023-10-31"),
                LocalDate.parse("2023-11-30"),
                LocalDate.parse("2023-12-31"),
            ).forEach { start ->
                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        note = task.note,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        startAfterDate = task.startAfterDate,
                        startAfterTime = task.startAfterTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        categoryId = task.categoryId,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = start,
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem(), name = "task starting at $start").isNotNull()
                    .isDataClassEqualTo(task)
            }
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnWeekOfMonth() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2024-07-26"),
                            type = RecurrenceType.WeekOfMonth,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-07-26"),
                            type = RecurrenceType.WeekOfMonth,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-06-28"),
                            type = RecurrenceType.WeekOfMonth,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2024-07-23"),
                            type = RecurrenceType.WeekOfMonth,
                            step = 1,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnYear() = runTest {
        val now = ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            var task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2023-08-23"),
                            type = RecurrenceType.Year,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2023-08-23"),
                            type = RecurrenceType.Year,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNull()

            task = taskRepository.update(
                EditTaskForm(
                    name = task.name,
                    note = task.note,
                    deadlineDate = task.deadlineDate,
                    deadlineTime = task.deadlineTime,
                    startAfterDate = task.startAfterDate,
                    startAfterTime = task.startAfterTime,
                    categoryId = task.categoryId,
                    scheduledDate = task.scheduledDate,
                    scheduledTime = task.scheduledTime,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = task.id,
                            start = LocalDate.parse("2022-08-23"),
                            type = RecurrenceType.Year,
                            step = 2,
                        ),
                    ),
                    parentUpdateType = PathUpdateType.Keep,
                    existingTask = task,
                    existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                        .first(),
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)
        }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnYear_whenNotLeapYear() = runTest {
        val now = ZonedDateTime.parse("2023-02-28T00:00-05:00[America/New_York]")
        val clock = Clock.fixed(now.toInstant(), now.zone)
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        taskRepository.getCurrentTask(CurrentTaskParams(now)).test {
            assertThat(awaitItem()).isNull()

            val task = taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    note = "",
                    deadlineDate = null,
                    deadlineTime = null,
                    startAfterDate = null,
                    startAfterTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    categoryId = null,
                    recurrences = listOf(
                        TaskRecurrence(
                            taskId = 0L,
                            start = LocalDate.parse("2020-02-29"),
                            type = RecurrenceType.Year,
                            step = 1,
                        ),
                    ),
                    parentTaskId = null,
                    clock = clock,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task)
        }
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingDeadlineDateRange() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = LocalTime.parse("12:00"),
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-20"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = LocalDate.parse("2024-08-20"),
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = LocalTime.parse("12:00"),
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = LocalTime.parse("12:00"),
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    deadlineDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingStartAfterDateRange() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = LocalDate.parse("2024-08-22"),
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-20"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = LocalDate.parse("2024-08-20"),
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    startAfterDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingScheduledDateRange() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = LocalDate.parse("2024-08-22"),
                scheduledTime = LocalTime.MIN,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = LocalTime.parse("12:00"),
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-20"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = LocalDate.parse("2024-08-20"),
                scheduledTime = LocalTime.MIN,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = LocalTime.parse("12:00"),
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    scheduledDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingDoneDateRange() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task1.id,
                doneAt = Instant.parse("2024-08-21T17:00:00Z"),
            ),
        )

        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task2.id,
                doneAt = Instant.parse("2024-08-22T17:00:00Z"),
            ),
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task2.id,
                doneAt = Instant.parse("2024-08-23T17:00:00Z"),
            ),
        )

        val task3 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task3.id,
                doneAt = Instant.parse("2024-08-20T17:00:00Z"),
            ),
        )

        val task4 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task4.id,
                doneAt = Instant.parse("2024-08-23T17:00:00Z"),
            ),
        )

        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    doneDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(
                id = task1.id,
                name = task1.name,
                recurring = false,
                doneAt = Instant.parse("2024-08-21T17:00:00Z"),
            ),
            TaskItem(
                id = task2.id,
                name = task2.name,
                recurring = true,
                doneAt = Instant.parse("2024-08-23T17:00:00Z"),
            ),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingRecurringDate() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "1. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "2. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-15"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-16"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task3 = taskRepository.create(
            NewTaskForm(
                name = "3. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task4 = taskRepository.create(
            NewTaskForm(
                name = "4. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-26"),
                        type = RecurrenceType.WeekOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task5 = taskRepository.create(
            NewTaskForm(
                name = "5. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2023-08-23"),
                        type = RecurrenceType.Year,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        taskRepository.create(
            NewTaskForm(
                name = "6. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "7. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "8. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-24"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "9. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-14"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-15"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "10. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "11. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-26"),
                        type = RecurrenceType.WeekOfMonth,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "12. ${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2023-08-23"),
                        type = RecurrenceType.Year,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    recurrenceDate = LocalDate.parse(
                        "2024-08-23"
                    )
                )
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = true, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
            TaskItem(id = task3.id, name = task3.name, recurring = true, doneAt = null),
            TaskItem(id = task4.id, name = task4.name, recurring = true, doneAt = null),
            TaskItem(id = task5.id, name = task5.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingNameAndDeadlineDateRange() = runTest {
        val query = loremFaker.verbs.base()
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = LocalTime.parse("12:00"),
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-20"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "0",
                note = "",
                deadlineDate = LocalDate.parse("2024-08-22"),
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = LocalDate.parse("2024-08-20"),
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = LocalTime.parse("12:00"),
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = LocalTime.parse("12:00"),
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    name = query,
                    deadlineDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingNameAndStartAfterDateRange() = runTest {
        val query = loremFaker.verbs.base()
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = LocalDate.parse("2024-08-22"),
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-20"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "0",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = LocalDate.parse("2024-08-22"),
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = LocalDate.parse("2024-08-20"),
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = LocalTime.parse("12:00"),
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    name = query,
                    startAfterDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingNameAndScheduledDateRange() = runTest {
        val query = loremFaker.verbs.base()
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = LocalDate.parse("2024-08-22"),
                scheduledTime = LocalTime.MIN,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = LocalTime.parse("12:00"),
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-20"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "0",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = LocalDate.parse("2024-08-22"),
                scheduledTime = LocalTime.MIN,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = LocalDate.parse("2024-08-20"),
                scheduledTime = LocalTime.MIN,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = LocalTime.parse("12:00"),
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = LocalTime.parse("12:00"),
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    name = query,
                    scheduledDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingNameAndDoneDateRange() = runTest {
        val query = loremFaker.verbs.base()
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task1.id,
                doneAt = Instant.parse("2024-08-21T17:00:00Z"),
            ),
        )

        val task2 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task2.id,
                doneAt = Instant.parse("2024-08-22T17:00:00Z"),
            ),
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task2.id,
                doneAt = Instant.parse("2024-08-23T17:00:00Z"),
            ),
        )

        val task3 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task3.id,
                doneAt = Instant.parse("2024-08-20T17:00:00Z"),
            ),
        )

        val task4 = taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task4.id,
                doneAt = Instant.parse("2024-08-23T17:00:00Z"),
            ),
        )

        val task5 = taskRepository.create(
            NewTaskForm(
                name = "0",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        db.taskDao().insertCompletion(
            TaskCompletion(
                taskId = task5.id,
                doneAt = Instant.parse("2024-08-21T17:00:00Z"),
            ),
        )

        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "$query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-23"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    name = query,
                    doneDateRange = Pair(
                        LocalDate.parse("2024-08-21"),
                        LocalDate.parse("2024-08-22"),
                    ),
                ),
            ).asSnapshot()
        ).containsExactly(
            TaskItem(
                id = task1.id,
                name = task1.name,
                recurring = false,
                doneAt = Instant.parse("2024-08-21T17:00:00Z"),
            ),
            TaskItem(
                id = task2.id,
                name = task2.name,
                recurring = true,
                doneAt = Instant.parse("2024-08-23T17:00:00Z"),
            ),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksNameAndMatchingRecurringDate() = runTest {
        val query = loremFaker.verbs.base()
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "1. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "2. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-15"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-16"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task3 = taskRepository.create(
            NewTaskForm(
                name = "3. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task4 = taskRepository.create(
            NewTaskForm(
                name = "4. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-26"),
                        type = RecurrenceType.WeekOfMonth,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task5 = taskRepository.create(
            NewTaskForm(
                name = "5. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2023-08-23"),
                        type = RecurrenceType.Year,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        taskRepository.create(
            NewTaskForm(
                name = "6. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "7. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "8. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-24"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "9. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-14"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-15"),
                        type = RecurrenceType.Week,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "10. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-23"),
                        type = RecurrenceType.DayOfMonth,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "11. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-07-26"),
                        type = RecurrenceType.WeekOfMonth,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "12. $query ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2023-08-23"),
                        type = RecurrenceType.Year,
                        step = 2,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )
        taskRepository.create(
            NewTaskForm(
                name = "0",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = listOf(
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Day,
                        step = 1,
                    ),
                ),
                parentTaskId = null,
                clock = clock,
            )
        )

        assertThat(
            taskRepository.searchTaskItems(
                TaskSearchCriteria(
                    name = query, recurrenceDate = LocalDate.parse(
                        "2024-08-23"
                    )
                )
            ).asSnapshot()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = true, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
            TaskItem(id = task3.id, name = task3.name, recurring = true, doneAt = null),
            TaskItem(id = task4.id, name = task4.name, recurring = true, doneAt = null),
            TaskItem(id = task5.id, name = task5.name, recurring = true, doneAt = null),
        )
    }


    @Test
    fun update_connectsTaskPaths() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = task1.id,
                clock = clock,
            )
        )
        val task3 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )

        val updatedTask2 = taskRepository.update(
            EditTaskForm(
                name = task2.name,
                note = task2.note,
                deadlineDate = LocalDate.parse("2024-08-23"),
                deadlineTime = task2.deadlineTime,
                startAfterDate = task2.startAfterDate,
                startAfterTime = task2.startAfterTime,
                scheduledDate = task2.scheduledDate,
                scheduledTime = task2.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Keep,
                existingTask = task2,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(updatedTask2.id).first()).isEqualTo(
            TaskDetail(
                id = updatedTask2.id,
                name = updatedTask2.name,
                note = updatedTask2.note,
                deadlineDate = updatedTask2.deadlineDate,
                deadlineTime = updatedTask2.deadlineTime,
                startAfterDate = updatedTask2.startAfterDate,
                startAfterTime = updatedTask2.startAfterTime,
                scheduledDate = updatedTask2.scheduledDate,
                scheduledTime = updatedTask2.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = task1.id,
                parentName = task1.name,
            )
        )

        taskRepository.update(
            EditTaskForm(
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Replace(updatedTask2.id),
                existingTask = task3,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = updatedTask2.id,
                parentName = updatedTask2.name,
            )
        )

        taskRepository.update(
            EditTaskForm(
                name = updatedTask2.name,
                note = updatedTask2.note,
                deadlineDate = updatedTask2.deadlineDate,
                deadlineTime = updatedTask2.deadlineTime,
                startAfterDate = updatedTask2.startAfterDate,
                startAfterTime = updatedTask2.startAfterTime,
                scheduledDate = updatedTask2.scheduledDate,
                scheduledTime = updatedTask2.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Remove,
                existingTask = updatedTask2,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(updatedTask2.id).first()).isEqualTo(
            TaskDetail(
                id = updatedTask2.id,
                name = updatedTask2.name,
                note = updatedTask2.note,
                deadlineDate = updatedTask2.deadlineDate,
                deadlineTime = updatedTask2.deadlineTime,
                startAfterDate = updatedTask2.startAfterDate,
                startAfterTime = updatedTask2.startAfterTime,
                scheduledDate = updatedTask2.scheduledDate,
                scheduledTime = updatedTask2.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = null,
                parentName = null,
            )
        )

        taskRepository.update(
            EditTaskForm(
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Replace(task1.id),
                existingTask = task3,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = task1.id,
                parentName = task1.name,
            )
        )

        taskRepository.update(
            EditTaskForm(
                name = task1.name,
                note = task1.note,
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                startAfterDate = task1.startAfterDate,
                startAfterTime = task1.startAfterTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Replace(task3.id),
                existingTask = task1,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(task1.id).first()).isEqualTo(
            TaskDetail(
                id = task1.id,
                name = task1.name,
                note = task1.note,
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                startAfterDate = task1.startAfterDate,
                startAfterTime = task1.startAfterTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = task3.id,
                parentName = task3.name,
            )
        )
        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = null,
                parentName = null,
            )
        )

        taskRepository.update(
            EditTaskForm(
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Replace(updatedTask2.id),
                existingTask = task3,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = updatedTask2.id,
                parentName = updatedTask2.name,
            )
        )

        taskRepository.update(
            EditTaskForm(
                name = task1.name,
                note = task1.note,
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                startAfterDate = task1.startAfterDate,
                startAfterTime = task1.startAfterTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
                categoryId = null,
                recurrences = emptyList(),
                parentUpdateType = PathUpdateType.Replace(updatedTask2.id),
                existingTask = task1,
                existingRecurrences = emptyList(),
            )
        )

        assertThat(taskRepository.getTaskDetailById(task1.id).first()).isEqualTo(
            TaskDetail(
                id = task1.id,
                name = task1.name,
                note = task1.note,
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                startAfterDate = task1.startAfterDate,
                startAfterTime = task1.startAfterTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = updatedTask2.id,
                parentName = updatedTask2.name,
            )
        )
        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = updatedTask2.id,
                parentName = updatedTask2.name,
            )
        )
    }

    @Test
    fun delete_decrementsDepthBetweenParentAndChild() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = createLocalTaskRepository(clock, testScheduler)

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )

        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = task1.id,
                clock = clock,
            )
        )

        val task3 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                note = "",
                deadlineDate = null,
                deadlineTime = null,
                startAfterDate = null,
                startAfterTime = null,
                scheduledDate = null,
                scheduledTime = null,
                categoryId = null,
                recurrences = emptyList(),
                parentTaskId = task2.id,
                clock = clock,
            )
        )

        assertThat(taskRepository.getTaskDetailById(task1.id).first()).isEqualTo(
            TaskDetail(
                id = task1.id,
                name = task1.name,
                note = task1.note,
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                startAfterDate = task1.startAfterDate,
                startAfterTime = task1.startAfterTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = null,
                parentName = null,
            )
        )
        assertThat(taskRepository.getTaskDetailById(task2.id).first()).isEqualTo(
            TaskDetail(
                id = task2.id,
                name = task2.name,
                note = task2.note,
                deadlineDate = task2.deadlineDate,
                deadlineTime = task2.deadlineTime,
                startAfterDate = task2.startAfterDate,
                startAfterTime = task2.startAfterTime,
                scheduledDate = task2.scheduledDate,
                scheduledTime = task2.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = task1.id,
                parentName = task1.name,
            )
        )
        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = task2.id,
                parentName = task2.name,
            )
        )

        taskRepository.delete(task2.id)

        assertThat(taskRepository.getTaskDetailById(task2.id).first()).isNull()
        assertThat(taskRepository.getTaskDetailById(task1.id).first()).isEqualTo(
            TaskDetail(
                id = task1.id,
                name = task1.name,
                note = task1.note,
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                startAfterDate = task1.startAfterDate,
                startAfterTime = task1.startAfterTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = null,
                parentName = null,
            )
        )
        assertThat(taskRepository.getTaskDetailById(task3.id).first()).isEqualTo(
            TaskDetail(
                id = task3.id,
                name = task3.name,
                note = task3.note,
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                startAfterDate = task3.startAfterDate,
                startAfterTime = task3.startAfterTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = task1.id,
                parentName = task1.name,
            )
        )
    }

    private fun createLocalTaskRepository(
        clock: Clock, testScheduler: TestCoroutineScheduler? = null
    ) = LocalTaskRepository(
        db.taskDao(),
        UnconfinedTestDispatcher(testScheduler),
        clock,
    )
}
