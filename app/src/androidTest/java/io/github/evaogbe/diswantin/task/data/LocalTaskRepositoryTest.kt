package io.github.evaogbe.diswantin.task.data

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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task1)

                var updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        note = task1.note,
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = LocalDate.parse("2024-08-24"),
                        startAfterTime = null,
                        categoryId = task1.categoryId,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem()).isNull()

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = updatedTask1.deadlineDate,
                        deadlineTime = updatedTask1.deadlineTime,
                        startAfterDate = LocalDate.parse("2024-08-23"),
                        startAfterTime = null,
                        categoryId = updatedTask1.categoryId,
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

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = updatedTask1.deadlineDate,
                        deadlineTime = updatedTask1.deadlineTime,
                        startAfterDate = LocalDate.parse("2024-08-23"),
                        startAfterTime = LocalTime.parse("13:01"),
                        categoryId = updatedTask1.categoryId,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem()).isNull()

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = updatedTask1.deadlineDate,
                        deadlineTime = updatedTask1.deadlineTime,
                        startAfterDate = LocalDate.parse("2024-08-23"),
                        startAfterTime = LocalTime.parse("13:00"),
                        categoryId = updatedTask1.categoryId,
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

                val task2 =
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

                assertThat(awaitItem()).isEqualTo(task2)

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = null,
                        categoryId = updatedTask1.categoryId,
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

                val updatedTask2 = taskRepository.update(
                    EditTaskForm(
                        name = task2.name,
                        note = task2.note,
                        deadlineDate = LocalDate.parse("2024-08-24"),
                        deadlineTime = LocalTime.parse("00:00"),
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        categoryId = task2.categoryId,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task2,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = null,
                        categoryId = updatedTask1.categoryId,
                        scheduledDate = LocalDate.parse("2024-08-23"),
                        scheduledTime = LocalTime.parse("14:00"),
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = LocalDate.parse("2024-08-23"),
                        scheduledTime = LocalTime.parse("15:00"),
                        categoryId = updatedTask1.categoryId,
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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

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
                        categoryId = updatedTask2.categoryId,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Replace(task3.id),
                        existingTask = updatedTask2,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task3)

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
                        categoryId = task3.categoryId,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Replace(updatedTask1.id),
                        existingTask = task3,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem()).isNull()

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = null,
                        deadlineTime = null,
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        categoryId = updatedTask1.categoryId,
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
                        note = updatedTask2.note,
                        deadlineDate = LocalDate.parse("2024-08-23"),
                        deadlineTime = LocalTime.parse("23:00"),
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        categoryId = updatedTask2.categoryId,
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
                        note = task4.note,
                        deadlineDate = null,
                        deadlineTime = LocalTime.parse("23:00"),
                        startAfterDate = null,
                        startAfterTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        categoryId = task4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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
                        note = updatedTask4.note,
                        deadlineDate = updatedTask4.deadlineDate,
                        deadlineTime = updatedTask4.deadlineTime,
                        startAfterDate = updatedTask4.startAfterDate,
                        startAfterTime = updatedTask4.startAfterTime,
                        scheduledDate = updatedTask4.scheduledDate,
                        scheduledTime = updatedTask4.scheduledTime,
                        categoryId = updatedTask4.categoryId,
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

                taskRepository.skip(updatedTask4.id)

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                val task5 = taskRepository.create(
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
                                step = 2,
                                week = 4,
                            ),
                        ),
                        parentTaskId = null,
                        clock = clock,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        note = updatedTask1.note,
                        deadlineDate = updatedTask1.deadlineDate,
                        deadlineTime = updatedTask1.deadlineTime,
                        startAfterDate = updatedTask1.startAfterDate,
                        startAfterTime = updatedTask1.startAfterTime,
                        scheduledDate = updatedTask1.scheduledDate,
                        scheduledTime = updatedTask1.scheduledTime,
                        categoryId = updatedTask1.categoryId,
                        recurrences = emptyList(),
                        parentUpdateType = PathUpdateType.Replace(task5.id),
                        existingTask = updatedTask1,
                        existingRecurrences = emptyList(),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)
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
                scheduledAfterTime = LocalTime.parse("14:00"),
                startAfterTime = LocalTime.parse("13:00"),
                doneAfter = Instant.parse("2024-08-24T04:00:00Z"),
                skippedAfter = Instant.parse("2024-08-23T04:00:00Z"),
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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task1)

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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

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

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(task2)

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
    fun searchTaskItems_emitsTasksMatchingDeadlineDateRange() = runTest {
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
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
                deadlineDate = LocalDate.parse("2024-08-21"),
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
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
                        week = 4,
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
                        LocalDate.parse("2024-08-22"),
                        LocalDate.parse("2024-08-23"),
                    ),
                ),
            ).first()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingStartAfterDateRange() = runTest {
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
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
                startAfterDate = LocalDate.parse("2024-08-21"),
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
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
                        week = 4,
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
                        LocalDate.parse("2024-08-22"),
                        LocalDate.parse("2024-08-23"),
                    ),
                ),
            ).first()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingScheduledDateRange() = runTest {
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
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
                scheduledDate = LocalDate.parse("2024-08-21"),
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
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
                        week = 4,
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
                        LocalDate.parse("2024-08-22"),
                        LocalDate.parse("2024-08-23"),
                    ),
                ),
            ).first()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
    }

    @Test
    fun searchTaskItems_emitsTasksMatchingNameAndDeadlineDateRange() = runTest {
        val query = loremFaker.verbs.base()
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
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
                deadlineDate = LocalDate.parse("2024-08-21"),
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
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
                        week = 4,
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
                        LocalDate.parse("2024-08-22"),
                        LocalDate.parse("2024-08-23"),
                    ),
                ),
            ).first()
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
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
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
                startAfterDate = LocalDate.parse("2024-08-21"),
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
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
                        week = 4,
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
                        LocalDate.parse("2024-08-22"),
                        LocalDate.parse("2024-08-23"),
                    ),
                ),
            ).first()
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
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
                    ),
                    TaskRecurrence(
                        taskId = 0L,
                        start = LocalDate.parse("2024-08-22"),
                        type = RecurrenceType.Week,
                        step = 1,
                        week = 4,
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
                scheduledDate = LocalDate.parse("2024-08-21"),
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
                        start = LocalDate.parse("2024-08-21"),
                        type = RecurrenceType.DayOfMonth,
                        step = 1,
                        week = 4,
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
                        week = 4,
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
                        LocalDate.parse("2024-08-22"),
                        LocalDate.parse("2024-08-23"),
                    ),
                ),
            ).first()
        ).containsExactly(
            TaskItem(id = task1.id, name = task1.name, recurring = false, doneAt = null),
            TaskItem(id = task2.id, name = task2.name, recurring = true, doneAt = null),
        )
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
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

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

        assertThat(taskRepository.getById(task2.id).first()).isNull()
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
}
