package io.github.evaogbe.diswantin.task.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isDataClassEqualTo
import assertk.assertions.isEmpty
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
            UnconfinedTestDispatcher(testScheduler)
        )

        taskRepository.getCurrentTask(scheduledBefore = Instant.parse("2024-08-23T18:00:00Z"))
            .test {
                assertThat(awaitItem()).isNull()

                val task1 =
                    taskRepository.create(
                        NewTaskForm(
                            name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                            dueAt = null,
                            scheduledAt = null,
                            prevTaskId = null,
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
                            dueAt = null,
                            scheduledAt = null,
                            prevTaskId = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem()).isEqualTo(task1)

                val updatedTask2 = task2.copy(dueAt = Instant.parse("2024-08-23T17:00:00Z"))
                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask2.name,
                        dueAt = updatedTask2.dueAt,
                        scheduledAt = updatedTask2.scheduledAt,
                        oldParentId = null,
                        parentId = null,
                        task = task2,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                val updatedTask1 = task1.copy(scheduledAt = Instant.parse("2024-08-23T18:00:00Z"))
                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        dueAt = updatedTask1.dueAt,
                        scheduledAt = updatedTask1.scheduledAt,
                        oldParentId = null,
                        parentId = null,
                        task = task1,
                    )
                )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask1)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask1.name,
                        dueAt = updatedTask1.dueAt,
                        scheduledAt = Instant.parse("2024-08-23T19:00:00Z"),
                        oldParentId = null,
                        parentId = null,
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
                            dueAt = null,
                            scheduledAt = null,
                            prevTaskId = null,
                            clock = clock,
                        )
                    )

                assertThat(awaitItem())
                    .isNotNull()
                    .isDataClassEqualTo(updatedTask2)

                taskRepository.update(
                    EditTaskForm(
                        name = updatedTask2.name,
                        dueAt = updatedTask2.dueAt,
                        scheduledAt = updatedTask2.scheduledAt,
                        oldParentId = null,
                        parentId = task3.id,
                        task = updatedTask2,
                    )
                )

                assertThat(awaitItem()).isNotNull().isDataClassEqualTo(task3)

                taskRepository.update(
                    EditTaskForm(
                        name = task3.name,
                        dueAt = task3.dueAt,
                        scheduledAt = task3.scheduledAt,
                        oldParentId = null,
                        parentId = updatedTask1.id,
                        task = task3,
                    )
                )

                assertThat(awaitItem()).isNull()
            }
    }

    @Test
    fun update_removesDescendantsFromParent_whenParentReplaced() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler)
        )

        val tasks = List(6) {
            taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    dueAt = null,
                    scheduledAt = null,
                    prevTaskId = null,
                    clock = clock,
                )
            )
        }

        assertThat(taskRepository.getChain(tasks[0].id).first())
            .containsExactly(tasks[0])
        assertThat(taskRepository.getChain(tasks[1].id).first())
            .containsExactly(tasks[1])
        assertThat(taskRepository.getChain(tasks[2].id).first())
            .containsExactly(tasks[2])
        assertThat(taskRepository.getChain(tasks[3].id).first())
            .containsExactly(tasks[3])
        assertThat(taskRepository.getChain(tasks[4].id).first())
            .containsExactly(tasks[4])
        assertThat(taskRepository.getChain(tasks[5].id).first())
            .containsExactly(tasks[5])

        taskRepository.update(
            EditTaskForm(
                name = tasks[2].name,
                dueAt = tasks[2].dueAt,
                scheduledAt = tasks[2].scheduledAt,
                oldParentId = null,
                parentId = tasks[1].id,
                task = tasks[2],
            )
        )

        assertThat(taskRepository.getChain(tasks[0].id).first())
            .containsExactly(tasks[0])
        assertThat(taskRepository.getChain(tasks[1].id).first())
            .containsExactly(tasks[1], tasks[2])
        assertThat(taskRepository.getChain(tasks[2].id).first())
            .containsExactly(tasks[1], tasks[2])
        assertThat(taskRepository.getChain(tasks[3].id).first())
            .containsExactly(tasks[3])
        assertThat(taskRepository.getChain(tasks[4].id).first())
            .containsExactly(tasks[4])
        assertThat(taskRepository.getChain(tasks[5].id).first())
            .containsExactly(tasks[5])

        taskRepository.update(
            EditTaskForm(
                name = tasks[1].name,
                dueAt = tasks[1].dueAt,
                scheduledAt = tasks[1].scheduledAt,
                oldParentId = null,
                parentId = tasks[0].id,
                task = tasks[1],
            )
        )

        assertThat(taskRepository.getChain(tasks[0].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2])
        assertThat(taskRepository.getChain(tasks[1].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2])
        assertThat(taskRepository.getChain(tasks[2].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2])
        assertThat(taskRepository.getChain(tasks[3].id).first())
            .containsExactly(tasks[3])
        assertThat(taskRepository.getChain(tasks[4].id).first())
            .containsExactly(tasks[4])
        assertThat(taskRepository.getChain(tasks[5].id).first())
            .containsExactly(tasks[5])

        taskRepository.update(
            EditTaskForm(
                name = tasks[3].name,
                dueAt = tasks[3].dueAt,
                scheduledAt = tasks[3].scheduledAt,
                oldParentId = null,
                parentId = tasks[2].id,
                task = tasks[3],
            )
        )

        assertThat(taskRepository.getChain(tasks[0].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[1].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[2].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[3].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[4].id).first())
            .containsExactly(tasks[4])
        assertThat(taskRepository.getChain(tasks[5].id).first())
            .containsExactly(tasks[5])

        taskRepository.update(
            EditTaskForm(
                name = tasks[5].name,
                dueAt = tasks[5].dueAt,
                scheduledAt = tasks[5].scheduledAt,
                oldParentId = null,
                parentId = tasks[4].id,
                task = tasks[5],
            )
        )

        assertThat(taskRepository.getChain(tasks[0].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[1].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[2].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[3].id).first())
            .containsExactly(tasks[0], tasks[1], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[4].id).first())
            .containsExactly(tasks[4], tasks[5])
        assertThat(taskRepository.getChain(tasks[5].id).first())
            .containsExactly(tasks[4], tasks[5])

        taskRepository.update(
            EditTaskForm(
                name = tasks[2].name,
                dueAt = tasks[2].dueAt,
                scheduledAt = tasks[2].scheduledAt,
                oldParentId = tasks[1].id,
                parentId = tasks[5].id,
                task = tasks[2],
            )
        )

        assertThat(taskRepository.getChain(tasks[0].id).first())
            .containsExactly(tasks[0], tasks[1])
        assertThat(taskRepository.getChain(tasks[1].id).first())
            .containsExactly(tasks[0], tasks[1])
        assertThat(taskRepository.getChain(tasks[2].id).first())
            .containsExactly(tasks[4], tasks[5], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[3].id).first())
            .containsExactly(tasks[4], tasks[5], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[4].id).first())
            .containsExactly(tasks[4], tasks[5], tasks[2], tasks[3])
        assertThat(taskRepository.getChain(tasks[5].id).first())
            .containsExactly(tasks[4], tasks[5], tasks[2], tasks[3])
    }

    @Test
    fun delete_decrementsDepthBetweenParentAndChild() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val taskRepository = LocalTaskRepository(
            db.taskDao(),
            UnconfinedTestDispatcher(testScheduler)
        )

        val (task1, task2, task3) = List(3) {
            taskRepository.create(
                NewTaskForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    dueAt = null,
                    scheduledAt = null,
                    prevTaskId = null,
                    clock = clock,
                )
            )
        }

        taskRepository.update(
            EditTaskForm(
                name = task2.name,
                dueAt = task2.dueAt,
                scheduledAt = task2.scheduledAt,
                oldParentId = null,
                parentId = task1.id,
                task = task2,
            )
        )
        taskRepository.update(
            EditTaskForm(
                name = task3.name,
                dueAt = task3.dueAt,
                scheduledAt = task3.scheduledAt,
                oldParentId = null,
                parentId = task2.id,
                task = task3,
            )
        )

        assertThat(taskRepository.getChain(task1.id).first())
            .containsExactly(task1, task2, task3)
        assertThat(taskRepository.getChain(task2.id).first())
            .containsExactly(task1, task2, task3)
        assertThat(taskRepository.getChain(task3.id).first())
            .containsExactly(task1, task2, task3)

        taskRepository.remove(task2.id)

        assertThat(taskRepository.getById(task2.id).first()).isNull()
        assertThat(taskRepository.getChain(task1.id).first())
            .containsExactly(task1, task3)
        assertThat(taskRepository.getChain(task2.id).first()).isEmpty()
        assertThat(taskRepository.getChain(task3.id).first())
            .containsExactly(task1, task3)
    }
}
