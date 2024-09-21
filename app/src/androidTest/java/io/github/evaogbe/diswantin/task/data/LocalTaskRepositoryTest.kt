package io.github.evaogbe.diswantin.task.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isDataClassEqualTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.github.evaogbe.diswantin.app.data.DiswantinDatabase
import io.github.serpro69.kfaker.lorem.LoremFaker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
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
            ApplicationProvider.getApplicationContext(),
            DiswantinDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getCurrentTask_emitsFirstPrioritizedTask() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2024-08-23T13:00-04:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task1 =
                    taskRepository.create(
                        NewTaskForm(
                            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                            deadlineDate = null,
                            deadlineTime = null,
                            scheduledDate = null,
                            scheduledTime = null,
                            recurrences = emptyList(),
                            parentTaskId = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task1)

                val task2 =
                    taskRepository.create(
                        NewTaskForm(
                            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                            deadlineDate = null,
                            deadlineTime = null,
                            scheduledDate = null,
                            scheduledTime = null,
                            recurrences = emptyList(),
                            parentTaskId = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem()).isEqualTo(task1)

                val updatedTask2 = taskRepository.update(
                    EditTaskForm(
                        name = task2.name,
                        deadlineDate = LocalDate.parse("2024-08-24"),
                        deadlineTime = LocalTime.parse("00:00"),
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task2,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                var updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = LocalDate.parse("2024-08-23"),
                        scheduledTime = LocalTime.parse("14:00"),
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = LocalDate.parse("2024-08-23"),
                        scheduledTime = LocalTime.parse("15:00"),
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                val task3 =
                    taskRepository.create(
                        NewTaskForm(
                            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                            deadlineDate = null,
                            deadlineTime = null,
                            scheduledDate = null,
                            scheduledTime = null,
                            recurrences = emptyList(),
                            parentTaskId = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                taskRepository.addParent(id = updatedTask2.id, parentId = task3.id)

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task3)

                taskRepository.addParent(id = task3.id, parentId = updatedTask1.id)

                assertThat(awaitItem()).isNull()

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                val task4 = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2024-08-23"),
                                type = RecurrenceType.Day,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task4)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask2.name,
                        deadlineDate = LocalDate.parse("2024-08-23"),
                        deadlineTime = LocalTime.parse("23:00"),
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask2,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                val taskRecurrences4 = taskRepository.getTaskRecurrencesByTaskId(task4.id).first()
                val updatedTask4 = taskRepository.update(
                    EditTaskForm(
                        name = task4.name,
                        deadlineDate = null,
                        deadlineTime = LocalTime.parse("23:00"),
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = taskRecurrences4,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task4,
                        existingRecurrences = taskRecurrences4,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-08-22"),
                                type = RecurrenceType.Day,
                                step = 2,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRecurrences4,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-08-21"),
                                type = RecurrenceType.Day,
                                step = 2,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRecurrences4,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-08-14"),
                                type = RecurrenceType.Week,
                                step = 1,
                                week = 3,
                            ),
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-08-15"),
                                type = RecurrenceType.Week,
                                step = 1,
                                week = 3,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-08-16"),
                                type = RecurrenceType.Week,
                                step = 1,
                                week = 3,
                            ),
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-08-17"),
                                type = RecurrenceType.Week,
                                step = 1,
                                week = 3,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-07-26"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-07-23"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-09-23"),
                                type = RecurrenceType.DayOfMonth,
                                step = 2,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2023-10-23"),
                                type = RecurrenceType.DayOfMonth,
                                step = 2,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)


                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-07-23"),
                                type = RecurrenceType.WeekOfMonth,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2024-07-26"),
                                type = RecurrenceType.WeekOfMonth,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)


                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2023-07-23"),
                                type = RecurrenceType.Year,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask4.name,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = updatedTask4.id,
                                start = LocalDate.parse("2023-08-23"),
                                type = RecurrenceType.Year,
                                step = 1,
                                week = 4,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask4,
                        existingRecurrences = taskRepository
                            .getTaskRecurrencesByTaskId(updatedTask4.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)
            }
    }

    @Test
    fun getCurrentTask_emitsFirstUndoneTask() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(
                today = LocalDate.parse("2024-08-23"),
                scheduledTimeBefore = LocalTime.parse("14:00"),
                doneBefore = Instant.parse("2024-08-24T04:00:00Z"),
                recurringDeadline = ZonedDateTime.parse(
                    "2024-08-24T23:59:59.999-04:00[America/New_York]"
                ),
                week = 4,
            ),
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task1 = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task1)

                val task2 = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task1)

                taskRepository.markDone(task1.id)

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

                val task3 = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

                taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Replace(task3.id),
                        existingTask = task1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

                taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task1.id,
                                start = LocalDate.parse("2024-08-23"),
                                type = RecurrenceType.Day,
                                step = 1,
                                week = 4,
                            )
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task3)

                taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Remove,
                        existingTask = task1,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task1.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

                taskRepository.update(
                    EditTaskForm(
                        name = task3.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Replace(task1.id),
                        existingTask = task3,
                        existingRecurrences = emptyList(),
                    )
                )

                taskRepository.markDone(task2.id)

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task3)
            }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay_when29Feb() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-02-29T05:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2024-02-29T00:00-05:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2023-01-31"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 5,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)

                listOf(
                    LocalDate.parse("2023-03-31") to 5,
                    LocalDate.parse("2023-04-30") to 6,
                    LocalDate.parse("2023-05-31") to 5,
                    LocalDate.parse("2023-06-30") to 5,
                    LocalDate.parse("2023-07-31") to 6,
                    LocalDate.parse("2023-08-31") to 5,
                    LocalDate.parse("2023-09-30") to 5,
                    LocalDate.parse("2023-10-31") to 5,
                    LocalDate.parse("2023-11-30") to 5,
                    LocalDate.parse("2023-12-31") to 6,
                    LocalDate.parse("2020-02-29") to 5,
                ).forEach { (start, week) ->
                    taskRepository.update(
                        EditTaskForm(
                            name = task.name,
                            deadlineDate = task.deadlineDate,
                            deadlineTime = task.deadlineTime,
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrences = listOf(
                                TaskRecurrence(
                                    taskId = task.id,
                                    start = start,
                                    type = RecurrenceType.DayOfMonth,
                                    step = 1,
                                    week = week,
                                ),
                            ),
                            parentUpdateType = PathUpdateType.Keep,
                            existingTask = task,
                            existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                                .first(),
                        )
                    )

                    assertThat(awaitItem(), name = "task starting at $start")
                        .isNotNull()
                        .isDataClassEqualTo(task)
                }
            }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay_when30Apr() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-04-30T04:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2024-04-30T00:00-04:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2023-01-31"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 5,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)

                listOf(
                    LocalDate.parse("2023-03-31") to 5,
                    LocalDate.parse("2023-04-30") to 6,
                    LocalDate.parse("2023-05-31") to 5,
                    LocalDate.parse("2023-06-30") to 5,
                    LocalDate.parse("2023-07-31") to 6,
                    LocalDate.parse("2023-08-31") to 5,
                    LocalDate.parse("2023-09-30") to 5,
                    LocalDate.parse("2023-10-31") to 5,
                    LocalDate.parse("2023-11-30") to 5,
                    LocalDate.parse("2023-12-31") to 6,
                ).forEach { (start, week) ->
                    taskRepository.update(
                        EditTaskForm(
                            name = task.name,
                            deadlineDate = task.deadlineDate,
                            deadlineTime = task.deadlineTime,
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrences = listOf(
                                TaskRecurrence(
                                    taskId = task.id,
                                    start = start,
                                    type = RecurrenceType.DayOfMonth,
                                    step = 1,
                                    week = week,
                                ),
                            ),
                            parentUpdateType = PathUpdateType.Keep,
                            existingTask = task,
                            existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                                .first(),
                        )
                    )

                    assertThat(awaitItem(), name = "task starting at $start")
                        .isNotNull()
                        .isDataClassEqualTo(task)
                }
            }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay_when30Jun() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-06-30T04:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2024-06-30T00:00-04:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2023-01-31"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 5,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)

                listOf(
                    LocalDate.parse("2023-03-31") to 5,
                    LocalDate.parse("2023-04-30") to 6,
                    LocalDate.parse("2023-05-31") to 5,
                    LocalDate.parse("2023-06-30") to 5,
                    LocalDate.parse("2023-07-31") to 6,
                    LocalDate.parse("2023-08-31") to 5,
                    LocalDate.parse("2023-09-30") to 5,
                    LocalDate.parse("2023-10-31") to 5,
                    LocalDate.parse("2023-11-30") to 5,
                    LocalDate.parse("2023-12-31") to 6,
                ).forEach { (start, week) ->
                    taskRepository.update(
                        EditTaskForm(
                            name = task.name,
                            deadlineDate = task.deadlineDate,
                            deadlineTime = task.deadlineTime,
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrences = listOf(
                                TaskRecurrence(
                                    taskId = task.id,
                                    start = start,
                                    type = RecurrenceType.DayOfMonth,
                                    step = 1,
                                    week = week,
                                ),
                            ),
                            parentUpdateType = PathUpdateType.Keep,
                            existingTask = task,
                            existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                                .first(),
                        )
                    )

                    assertThat(awaitItem(), name = "task starting at $start")
                        .isNotNull()
                        .isDataClassEqualTo(task)
                }
            }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay_when30Sep() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-09-30T04:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2024-09-30T00:00-04:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2023-01-31"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 5,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)

                listOf(
                    LocalDate.parse("2023-03-31") to 5,
                    LocalDate.parse("2023-04-30") to 6,
                    LocalDate.parse("2023-05-31") to 5,
                    LocalDate.parse("2023-06-30") to 5,
                    LocalDate.parse("2023-07-31") to 6,
                    LocalDate.parse("2023-08-31") to 5,
                    LocalDate.parse("2023-09-30") to 5,
                    LocalDate.parse("2023-10-31") to 5,
                    LocalDate.parse("2023-11-30") to 5,
                    LocalDate.parse("2023-12-31") to 6,
                ).forEach { (start, week) ->
                    taskRepository.update(
                        EditTaskForm(
                            name = task.name,
                            deadlineDate = task.deadlineDate,
                            deadlineTime = task.deadlineTime,
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrences = listOf(
                                TaskRecurrence(
                                    taskId = task.id,
                                    start = start,
                                    type = RecurrenceType.DayOfMonth,
                                    step = 1,
                                    week = week,
                                ),
                            ),
                            parentUpdateType = PathUpdateType.Keep,
                            existingTask = task,
                            existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                                .first(),
                        )
                    )

                    assertThat(awaitItem(), name = "task starting at $start")
                        .isNotNull()
                        .isDataClassEqualTo(task)
                }
            }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay_when30Nov() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-11-30T05:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2024-11-30T00:00-05:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2023-01-31"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 5,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)

                listOf(
                    LocalDate.parse("2023-03-31") to 5,
                    LocalDate.parse("2023-04-30") to 6,
                    LocalDate.parse("2023-05-31") to 5,
                    LocalDate.parse("2023-06-30") to 5,
                    LocalDate.parse("2023-07-31") to 6,
                    LocalDate.parse("2023-08-31") to 5,
                    LocalDate.parse("2023-09-30") to 5,
                    LocalDate.parse("2023-10-31") to 5,
                    LocalDate.parse("2023-11-30") to 5,
                    LocalDate.parse("2023-12-31") to 6,
                ).forEach { (start, week) ->
                    taskRepository.update(
                        EditTaskForm(
                            name = task.name,
                            deadlineDate = task.deadlineDate,
                            deadlineTime = task.deadlineTime,
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrences = listOf(
                                TaskRecurrence(
                                    taskId = task.id,
                                    start = start,
                                    type = RecurrenceType.DayOfMonth,
                                    step = 1,
                                    week = week,
                                ),
                            ),
                            parentUpdateType = PathUpdateType.Keep,
                            existingTask = task,
                            existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                                .first(),
                        )
                    )

                    assertThat(awaitItem(), name = "task starting at $start")
                        .isNotNull()
                        .isDataClassEqualTo(task)
                }
            }
    }

    @Test
    fun getCurrentTask_emitsTaskRecurringOnDay_whenNotLeapYear() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2023-02-28T05:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(ZonedDateTime.parse("2023-02-28T00:00-05:00[America/New_York]"))
        )
            .test {
                assertThat(awaitItem()).isNull()

                val task = taskRepository.create(
                    NewTaskForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = 0L,
                                start = LocalDate.parse("2022-01-31"),
                                type = RecurrenceType.DayOfMonth,
                                step = 1,
                                week = 6,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)

                listOf(
                    LocalDate.parse("2022-02-28") to 5,
                    LocalDate.parse("2022-03-31") to 5,
                    LocalDate.parse("2022-04-30") to 5,
                    LocalDate.parse("2022-05-31") to 5,
                    LocalDate.parse("2022-06-30") to 5,
                    LocalDate.parse("2022-07-31") to 6,
                    LocalDate.parse("2022-08-31") to 5,
                    LocalDate.parse("2022-09-30") to 5,
                    LocalDate.parse("2022-10-31") to 6,
                    LocalDate.parse("2022-11-30") to 5,
                    LocalDate.parse("2022-12-31") to 5,
                    LocalDate.parse("2020-02-29") to 5,
                ).forEach { (start, week) ->
                    taskRepository.update(
                        EditTaskForm(
                            name = task.name,
                            deadlineDate = task.deadlineDate,
                            deadlineTime = task.deadlineTime,
                            scheduledDate = task.scheduledDate,
                            scheduledTime = task.scheduledTime,
                            recurrences = listOf(
                                TaskRecurrence(
                                    taskId = task.id,
                                    start = start,
                                    type = RecurrenceType.DayOfMonth,
                                    step = 1,
                                    week = week,
                                ),
                            ),
                            parentUpdateType = PathUpdateType.Keep,
                            existingTask = task,
                            existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                                .first(),
                        )
                    )

                    assertThat(awaitItem(), name = "task starting at $start")
                        .isNotNull()
                        .isDataClassEqualTo(task)
                }

                taskRepository.update(
                    EditTaskForm(
                        name = task.name,
                        deadlineDate = task.deadlineDate,
                        deadlineTime = task.deadlineTime,
                        scheduledDate = task.scheduledDate,
                        scheduledTime = task.scheduledTime,
                        recurrences = listOf(
                            TaskRecurrence(
                                taskId = task.id,
                                start = LocalDate.parse("2020-02-29"),
                                type = RecurrenceType.Year,
                                step = 1,
                                week = 5,
                            ),
                        ),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task,
                        existingRecurrences = taskRepository.getTaskRecurrencesByTaskId(task.id)
                            .first(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task)
            }
    }

    @Test
    fun update_connectsTaskPaths() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        val task1 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                deadlineDate = null,
                deadlineTime = null,
                scheduledDate = null,
                scheduledTime = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )
        val task2 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                deadlineDate = null,
                deadlineTime = null,
                scheduledDate = null,
                scheduledTime = null,
                recurrences = emptyList(),
                parentTaskId = task1.id,
                clock = clock,
            )
        )
        val task3 = taskRepository.create(
            NewTaskForm(
                name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                deadlineDate = null,
                deadlineTime = null,
                scheduledDate = null,
                scheduledTime = null,
                recurrences = emptyList(),
                parentTaskId = null,
                clock = clock,
            )
        )

        val updatedTask2 = taskRepository.update(
            EditTaskForm(
                name = task2.name,
                deadlineDate = LocalDate.parse("2024-08-23"),
                deadlineTime = task2.deadlineTime,
                scheduledDate = task2.scheduledDate,
                scheduledTime = task2.scheduledTime,
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
                deadlineDate = updatedTask2.deadlineDate,
                deadlineTime = updatedTask2.deadlineTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
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
                deadlineDate = updatedTask2.deadlineDate,
                deadlineTime = updatedTask2.deadlineTime,
                scheduledDate = updatedTask2.scheduledDate,
                scheduledTime = updatedTask2.scheduledTime,
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
                deadlineDate = updatedTask2.deadlineDate,
                deadlineTime = updatedTask2.deadlineTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
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
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
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
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
                scheduledDate = task3.scheduledDate,
                scheduledTime = task3.scheduledTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
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
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
                scheduledDate = task1.scheduledDate,
                scheduledTime = task1.scheduledTime,
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
                deadlineDate = task1.deadlineDate,
                deadlineTime = task1.deadlineTime,
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
                deadlineDate = task3.deadlineDate,
                deadlineTime = task3.deadlineTime,
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
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        val tasks = List(3) {
            taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    deadlineDate = null,
                    deadlineTime = null,
                    scheduledDate = null,
                    scheduledTime = null,
                    recurrences = emptyList(),
                    parentTaskId = null,
                    clock = clock,
                )
            )
        }

        taskRepository.addParent(id = tasks[1].id, parentId = tasks[0].id)
        taskRepository.addParent(id = tasks[2].id, parentId = tasks[1].id)

        assertThat(taskRepository.getTaskDetailById(tasks[0].id).first()).isEqualTo(
            TaskDetail(
                id = tasks[0].id,
                name = tasks[0].name,
                deadlineDate = tasks[0].deadlineDate,
                deadlineTime = tasks[0].deadlineTime,
                scheduledDate = tasks[0].scheduledDate,
                scheduledTime = tasks[0].scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = null,
                parentName = null,
            )
        )
        assertThat(taskRepository.getTaskDetailById(tasks[1].id).first()).isEqualTo(
            TaskDetail(
                id = tasks[1].id,
                name = tasks[1].name,
                deadlineDate = tasks[1].deadlineDate,
                deadlineTime = tasks[1].deadlineTime,
                scheduledDate = tasks[1].scheduledDate,
                scheduledTime = tasks[1].scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = tasks[0].id,
                parentName = tasks[0].name,
            )
        )
        assertThat(taskRepository.getTaskDetailById(tasks[2].id).first()).isEqualTo(
            TaskDetail(
                id = tasks[2].id,
                name = tasks[2].name,
                deadlineDate = tasks[2].deadlineDate,
                deadlineTime = tasks[2].deadlineTime,
                scheduledDate = tasks[2].scheduledDate,
                scheduledTime = tasks[2].scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = tasks[1].id,
                parentName = tasks[1].name,
            )
        )

        taskRepository.delete(tasks[1].id)

        assertThat(taskRepository.getById(tasks[1].id).first()).isNull()
        assertThat(taskRepository.getTaskDetailById(tasks[0].id).first()).isEqualTo(
            TaskDetail(
                id = tasks[0].id,
                name = tasks[0].name,
                deadlineDate = tasks[0].deadlineDate,
                deadlineTime = tasks[0].deadlineTime,
                scheduledDate = tasks[0].scheduledDate,
                scheduledTime = tasks[0].scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = null,
                parentName = null,
            )
        )
        assertThat(taskRepository.getTaskDetailById(tasks[2].id).first()).isEqualTo(
            TaskDetail(
                id = tasks[2].id,
                name = tasks[2].name,
                deadlineDate = tasks[2].deadlineDate,
                deadlineTime = tasks[2].deadlineTime,
                scheduledDate = tasks[2].scheduledDate,
                scheduledTime = tasks[2].scheduledTime,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = tasks[0].id,
                parentName = tasks[0].name,
            )
        )
    }
}
