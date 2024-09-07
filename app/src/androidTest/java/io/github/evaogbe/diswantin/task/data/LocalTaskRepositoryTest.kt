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
    fun currentTaskStream_emitsFirstPrioritizedTask() = runTest {
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
                            recurring = false,
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
                            recurring = false,
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
                        recurring = task2.recurring,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task2,
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
                        recurring = task1.recurring,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task1,
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
                        recurring = updatedTask1.recurring,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask1,
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
                            recurring = false,
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
                        recurring = false,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask1,
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
                        recurring = true,
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
                        recurring = false,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = updatedTask2,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                val updatedTask4 = taskRepository.update(
                    EditTaskForm(
                        name = task4.name,
                        deadlineDate = LocalDate.parse("2024-08-23"),
                        deadlineTime = LocalTime.parse("23:00"),
                        scheduledDate = null,
                        scheduledTime = null,
                        recurring = true,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task4,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask4)
            }
    }

    @Test
    fun currentTaskStream_emitsFirstUndoneTask() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock,
        )

        taskRepository.getCurrentTask(
            CurrentTaskParams(
                scheduledDateBefore = LocalDate.parse("2024-08-23"),
                scheduledTimeBefore = LocalTime.parse("14:00"),
                doneBefore = Instant.parse("2024-08-24T04:00:00Z"),
                recurringDeadline = ZonedDateTime.parse("2024-08-24T23:59:59.999-04:00[America/New_York]"),
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
                        recurring = false,
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
                        recurring = false,
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

                val updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        deadlineDate = null,
                        deadlineTime = null,
                        scheduledDate = null,
                        scheduledTime = null,
                        recurring = true,
                        parentUpdateType = PathUpdateType.Keep,
                        existingTask = task1,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)
            }
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
                    recurring = false,
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
                recurring = tasks[0].recurring,
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
                recurring = tasks[1].recurring,
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
                recurring = tasks[2].recurring,
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
                recurring = tasks[0].recurring,
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
                recurring = tasks[2].recurring,
                doneAt = null,
                categoryId = null,
                categoryName = null,
                parentId = tasks[0].id,
                parentName = tasks[0].name,
            )
        )
    }
}
