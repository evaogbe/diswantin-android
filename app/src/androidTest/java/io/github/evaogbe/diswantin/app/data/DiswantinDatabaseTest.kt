package io.github.evaogbe.diswantin.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isPositive
import assertk.assertions.isTrue
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.lorem.LoremFaker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiswantinDatabaseTest {
    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DiswantinDatabase::class.java,
    )

    private val loremFaker = LoremFaker()

    private val faker = Faker()

    @Test
    fun testMigration_1_2() {
        val initialId = faker.random.nextUUID()
        val initialCreatedAt = faker.random.randomPastDate().toInstant().toEpochMilli()
        val initialName = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"

        val initialDb = migrationTestHelper.createDatabase(DiswantinDatabase.DB_NAME, 1)
        initialDb.execSQL(
            "INSERT INTO Activity (id, created_at, name) VALUES (?, ?, ?)",
            arrayOf(initialId, initialCreatedAt, initialName)
        )
        initialDb.close()

        val migratedDb =
            migrationTestHelper.runMigrationsAndValidate(
                DiswantinDatabase.DB_NAME,
                2,
                true,
                DiswantinDatabase.MIGRATION_1_2
            )
        migratedDb.query("SELECT * FROM activity").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getLong(0)).isPositive()
            assertThat(stmt.getLong(1)).isEqualTo(initialCreatedAt)
            assertThat(stmt.getString(2)).isEqualTo(initialName)
        }
        migratedDb.query("SELECT * FROM activity_fts").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getString(0)).isEqualTo(initialName)
        }
        migratedDb.close()
    }
}
