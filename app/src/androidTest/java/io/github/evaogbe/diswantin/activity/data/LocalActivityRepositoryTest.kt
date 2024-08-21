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
    fun skipFlow() = runTest {
        val activityRepository =
            LocalActivityRepository(db.activityDao(), UnconfinedTestDispatcher(testScheduler))

        activityRepository.currentActivityStream.test {
            assertThat(awaitItem()).isNull()

            val activity1 =
                activityRepository.create(
                    "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    null
                )

            val currentActivity1 = awaitItem()
            assertThat(currentActivity1)
                .isNotNull()
                .isDataClassEqualTo(activity1)

            val activity2 =
                activityRepository.create(
                    "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
                    null
                )

            assertThat(awaitItem()).isEqualTo(currentActivity1)

            val updatedActivity1 = currentActivity1!!.copy(skippedAt = Instant.now())
            activityRepository.update(updatedActivity1)

            val currentActivity2 = awaitItem()
            assertThat(currentActivity2)
                .isNotNull()
                .isDataClassEqualTo(activity2)

            val updatedActivity2 = currentActivity2!!.copy(skippedAt = Instant.now())
            activityRepository.update(updatedActivity2)

            val currentActivity3 = awaitItem()
            assertThat(currentActivity3)
                .isNotNull()
                .isDataClassEqualTo(updatedActivity1)

            val updatedActivity3 = currentActivity3!!.copy(skippedAt = Instant.now())
            activityRepository.update(updatedActivity3)

            assertThat(awaitItem())
                .isNotNull()
                .isDataClassEqualTo(updatedActivity2)
        }
    }
}
