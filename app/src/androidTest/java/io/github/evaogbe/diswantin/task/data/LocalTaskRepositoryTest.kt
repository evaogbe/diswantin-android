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
import java.time.ZoneId

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
    fun currentTaskStream_emitsFirstPlannedTask() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
        )
        val taskListRepository = LocalTaskListRepository(
            db.taskListDao(),
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
        )

        taskRepository.getCurrentTask(scheduledBefore = Instant.parse("2024-08-23T18:00:00Z"))
            .test {
                assertThat(awaitItem()).isNull()

                val task1 =
                    taskRepository.create(
                        NewTaskForm(
                            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                            deadline = null,
                            scheduledAt = null,
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
                            deadline = null,
                            scheduledAt = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem()).isEqualTo(task1)

                val updatedTask2 = taskRepository.update(
                    EditTaskForm(
                        name = task2.name,
                        deadline = Instant.parse("2024-08-23T17:00:00Z"),
                        scheduledAt = task2.scheduledAt,
                        task = task2,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                var updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = task1.name,
                        deadline = task1.deadline,
                        scheduledAt = Instant.parse("2024-08-23T18:00:00Z"),
                        task = task1,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                updatedTask1 = taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        deadline = updatedTask1.deadline,
                        scheduledAt = Instant.parse("2024-08-23T19:00:00Z"),
                        task = updatedTask1,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                val task3 =
                    taskRepository.create(
                        NewTaskForm(
                            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                            deadline = null,
                            scheduledAt = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                val taskListWithTasks = taskListRepository.create(
                    NewTaskListForm(
                        name = loremFaker.lorem.words(),
                        tasks = listOf(task3, updatedTask2),
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(taskListWithTasks.tasks[0])

                taskListRepository.update(
                    EditTaskListForm(
                        name = taskListWithTasks.taskList.name,
                        tasks = listOf(updatedTask1) + taskListWithTasks.tasks,
                        taskListWithTasks = taskListWithTasks
                    )
                )

                assertThat(awaitItem()).isNull()
            }
    }

    @Test
    fun delete_decrementsDepthBetweenParentAndChild() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
        )
        val taskListRepository = LocalTaskListRepository(
            db.taskListDao(),
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler),
        )

        val tasks = List(3) {
            taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    deadline = null,
                    scheduledAt = null,
                    clock = clock,
                )
            )
        }

        val taskListWithTasks =
            taskListRepository.create(
                NewTaskListForm(
                    name = loremFaker.lorem.words(),
                    tasks = tasks,
                )
            )

        taskRepository.remove(taskListWithTasks.tasks[1].id)

        assertThat(taskRepository.getById(taskListWithTasks.tasks[1].id).first()).isNull()
        assertThat(taskListRepository.getById(taskListWithTasks.taskList.id).first().tasks)
            .containsExactly(taskListWithTasks.tasks[0], taskListWithTasks.tasks[2])
    }
}
