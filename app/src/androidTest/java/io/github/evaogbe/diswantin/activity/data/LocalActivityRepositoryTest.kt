package io.github.evaogbe.diswantin.activity.data

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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

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
    fun currentActivityStream_sortsByDueDate() = runTest {
        val activityRepository =
            LocalActivityRepository(db.activityDao(), UnconfinedTestDispatcher(testScheduler))

        activityRepository.currentActivityStream.test {
            assertThat(awaitItem()).isNull()

            val activity1 =
                activityRepository.create(
                    "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    null
                )

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(activity1)

            val activity2 =
                activityRepository.create(
                    "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    null
                )

            assertThat(awaitItem()).isEqualTo(activity1)

            val updatedActivity2 = activity2.copy(dueAt = Instant.parse("2024-08-23T17:00:00Z"))
            activityRepository.update(updatedActivity2)

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity2)

            val updatedActivity1 = activity1.copy(dueAt = Instant.parse("2024-08-22T20:00:00Z"))
            activityRepository.update(updatedActivity1)

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity1)
        }
    }
}
