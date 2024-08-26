package io.github.evaogbe.diswantin.activity.data

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
class LocalActivityRepositoryTest {
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
    fun currentActivityStream_emitsFirstPlannedActivity() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val activityRepository = LocalActivityRepository(
            db.activityDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock
        )

        activityRepository.currentActivityStream.test {
            assertThat(awaitItem()).isNull()

            val activity1 =
                activityRepository.create(
                    NewActivityForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        dueAt = null,
                        scheduledAt = null,
                        prevActivityId = null,
                        clock = clock,
                    )
                )

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(activity1)

            val activity2 =
                activityRepository.create(
                    NewActivityForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        dueAt = null,
                        scheduledAt = null,
                        prevActivityId = null,
                        clock = clock,
                    )
                )

            assertThat(awaitItem()).isEqualTo(activity1)

            val updatedActivity2 = activity2.copy(dueAt = Instant.parse("2024-08-23T17:00:00Z"))
            activityRepository.update(
                EditActivityForm(
                    name = updatedActivity2.name,
                    dueAt = updatedActivity2.dueAt,
                    scheduledAt = updatedActivity2.scheduledAt,
                    oldParentId = null,
                    parentId = null,
                    activity = activity2,
                )
            )

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity2)

            val updatedActivity1 =
                activity1.copy(scheduledAt = Instant.parse("2024-08-23T18:00:00Z"))
            activityRepository.update(
                EditActivityForm(
                    name = updatedActivity1.name,
                    dueAt = updatedActivity1.dueAt,
                    scheduledAt = updatedActivity1.scheduledAt,
                    oldParentId = null,
                    parentId = null,
                    activity = activity1,
                )
            )

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity1)

            activityRepository.update(
                EditActivityForm(
                    name = updatedActivity1.name,
                    dueAt = updatedActivity1.dueAt,
                    scheduledAt = Instant.parse("2024-08-23T19:00:00Z"),
                    oldParentId = null,
                    parentId = null,
                    activity = updatedActivity1,
                )
            )

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity2)

            val activity3 =
                activityRepository.create(
                    NewActivityForm(
                        name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                        dueAt = null,
                        scheduledAt = null,
                        prevActivityId = null,
                        clock = clock,
                    )
                )

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity2)

            activityRepository.update(
                EditActivityForm(
                    name = updatedActivity2.name,
                    dueAt = updatedActivity2.dueAt,
                    scheduledAt = updatedActivity2.scheduledAt,
                    oldParentId = null,
                    parentId = activity3.id,
                    activity = updatedActivity2,
                )
            )

            assertThat(awaitItem()).isNotNull().isDataClassEqualTo(activity3)

            activityRepository.update(
                EditActivityForm(
                    name = activity3.name,
                    dueAt = activity3.dueAt,
                    scheduledAt = activity3.scheduledAt,
                    oldParentId = null,
                    parentId = updatedActivity1.id,
                    activity = activity3,
                )
            )

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun update_removesDescendantsFromParent_whenParentReplaced() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val activityRepository = LocalActivityRepository(
            db.activityDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock
        )

        val activities = List(6) {
            activityRepository.create(
                NewActivityForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    dueAt = null,
                    scheduledAt = null,
                    prevActivityId = null,
                    clock = clock,
                )
            )
        }

        assertThat(activityRepository.getChain(activities[0].id).first())
            .containsExactly(activities[0])
        assertThat(activityRepository.getChain(activities[1].id).first())
            .containsExactly(activities[1])
        assertThat(activityRepository.getChain(activities[2].id).first())
            .containsExactly(activities[2])
        assertThat(activityRepository.getChain(activities[3].id).first())
            .containsExactly(activities[3])
        assertThat(activityRepository.getChain(activities[4].id).first())
            .containsExactly(activities[4])
        assertThat(activityRepository.getChain(activities[5].id).first())
            .containsExactly(activities[5])

        activityRepository.update(
            EditActivityForm(
                name = activities[2].name,
                dueAt = activities[2].dueAt,
                scheduledAt = activities[2].scheduledAt,
                oldParentId = null,
                parentId = activities[1].id,
                activity = activities[2],
            )
        )

        assertThat(activityRepository.getChain(activities[0].id).first())
            .containsExactly(activities[0])
        assertThat(activityRepository.getChain(activities[1].id).first())
            .containsExactly(activities[1], activities[2])
        assertThat(activityRepository.getChain(activities[2].id).first())
            .containsExactly(activities[1], activities[2])
        assertThat(activityRepository.getChain(activities[3].id).first())
            .containsExactly(activities[3])
        assertThat(activityRepository.getChain(activities[4].id).first())
            .containsExactly(activities[4])
        assertThat(activityRepository.getChain(activities[5].id).first())
            .containsExactly(activities[5])

        activityRepository.update(
            EditActivityForm(
                name = activities[1].name,
                dueAt = activities[1].dueAt,
                scheduledAt = activities[1].scheduledAt,
                oldParentId = null,
                parentId = activities[0].id,
                activity = activities[1],
            )
        )

        assertThat(activityRepository.getChain(activities[0].id).first())
            .containsExactly(activities[0], activities[1], activities[2])
        assertThat(activityRepository.getChain(activities[1].id).first())
            .containsExactly(activities[0], activities[1], activities[2])
        assertThat(activityRepository.getChain(activities[2].id).first())
            .containsExactly(activities[0], activities[1], activities[2])
        assertThat(activityRepository.getChain(activities[3].id).first())
            .containsExactly(activities[3])
        assertThat(activityRepository.getChain(activities[4].id).first())
            .containsExactly(activities[4])
        assertThat(activityRepository.getChain(activities[5].id).first())
            .containsExactly(activities[5])

        activityRepository.update(
            EditActivityForm(
                name = activities[3].name,
                dueAt = activities[3].dueAt,
                scheduledAt = activities[3].scheduledAt,
                oldParentId = null,
                parentId = activities[2].id,
                activity = activities[3],
            )
        )

        assertThat(activityRepository.getChain(activities[0].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[1].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[2].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[3].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[4].id).first())
            .containsExactly(activities[4])
        assertThat(activityRepository.getChain(activities[5].id).first())
            .containsExactly(activities[5])

        activityRepository.update(
            EditActivityForm(
                name = activities[5].name,
                dueAt = activities[5].dueAt,
                scheduledAt = activities[5].scheduledAt,
                oldParentId = null,
                parentId = activities[4].id,
                activity = activities[5],
            )
        )

        assertThat(activityRepository.getChain(activities[0].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[1].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[2].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[3].id).first())
            .containsExactly(activities[0], activities[1], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[4].id).first())
            .containsExactly(activities[4], activities[5])
        assertThat(activityRepository.getChain(activities[5].id).first())
            .containsExactly(activities[4], activities[5])

        activityRepository.update(
            EditActivityForm(
                name = activities[2].name,
                dueAt = activities[2].dueAt,
                scheduledAt = activities[2].scheduledAt,
                oldParentId = activities[1].id,
                parentId = activities[5].id,
                activity = activities[2],
            )
        )

        assertThat(activityRepository.getChain(activities[0].id).first())
            .containsExactly(activities[0], activities[1])
        assertThat(activityRepository.getChain(activities[1].id).first())
            .containsExactly(activities[0], activities[1])
        assertThat(activityRepository.getChain(activities[2].id).first())
            .containsExactly(activities[4], activities[5], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[3].id).first())
            .containsExactly(activities[4], activities[5], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[4].id).first())
            .containsExactly(activities[4], activities[5], activities[2], activities[3])
        assertThat(activityRepository.getChain(activities[5].id).first())
            .containsExactly(activities[4], activities[5], activities[2], activities[3])
    }

    @Test
    fun delete_decrementsDepthBetweenParentAndChild() = runTest {
        val clock =
            Clock.fixed(Instant.parse("2024-08-23T17:00:00Z"), ZoneId.of("America/New_York"))
        val activityRepository = LocalActivityRepository(
            db.activityDao(),
            UnconfinedTestDispatcher(testScheduler),
            clock
        )

        val (activity1, activity2, activity3) = List(3) {
            activityRepository.create(
                NewActivityForm(
                    name = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    dueAt = null,
                    scheduledAt = null,
                    prevActivityId = null,
                    clock = clock,
                )
            )
        }

        activityRepository.update(
            EditActivityForm(
                name = activity2.name,
                dueAt = activity2.dueAt,
                scheduledAt = activity2.scheduledAt,
                oldParentId = null,
                parentId = activity1.id,
                activity = activity2,
            )
        )
        activityRepository.update(
            EditActivityForm(
                name = activity3.name,
                dueAt = activity3.dueAt,
                scheduledAt = activity3.scheduledAt,
                oldParentId = null,
                parentId = activity2.id,
                activity = activity3,
            )
        )

        assertThat(activityRepository.getChain(activity1.id).first())
            .containsExactly(activity1, activity2, activity3)
        assertThat(activityRepository.getChain(activity2.id).first())
            .containsExactly(activity1, activity2, activity3)
        assertThat(activityRepository.getChain(activity3.id).first())
            .containsExactly(activity1, activity2, activity3)

        activityRepository.remove(activity2.id)

        assertThat(activityRepository.getById(activity2.id).first()).isNull()
        assertThat(activityRepository.getChain(activity1.id).first())
            .containsExactly(activity1, activity3)
        assertThat(activityRepository.getChain(activity2.id).first()).isEmpty()
        assertThat(activityRepository.getChain(activity3.id).first())
            .containsExactly(activity1, activity3)
    }
}
