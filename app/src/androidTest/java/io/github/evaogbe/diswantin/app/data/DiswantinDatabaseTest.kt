package io.github.evaogbe.diswantin.app.data

import androidx.core.database.getLongOrNull
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
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

    @Test
    fun testMigration_5_6() {
        val initialId = faker.random.nextLong(min = 1, max = Long.MAX_VALUE)
        val initialCreatedAt = faker.random.randomPastDate().toInstant().toEpochMilli()
        val initialName = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"
        val initialDueAt = faker.random.randomFutureDate().toInstant().toEpochMilli()

        val initialDb = migrationTestHelper.createDatabase(DiswantinDatabase.DB_NAME, 5)
        initialDb.execSQL(
            "INSERT INTO activity (id, created_at, name, due_at) VALUES (?, ?, ?, ?)",
            arrayOf(initialId, initialCreatedAt, initialName, initialDueAt)
        )
        initialDb.close()

        val migratedDb =
            migrationTestHelper.runMigrationsAndValidate(
                DiswantinDatabase.DB_NAME,
                6,
                true,
                DiswantinDatabase.MIGRATION_5_6
            )
        migratedDb.query("SELECT * FROM activity").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(initialId)
            assertThat(stmt.getLong(1)).isEqualTo(initialCreatedAt)
            assertThat(stmt.getString(2)).isEqualTo(initialName)
            assertThat(stmt.getLongOrNull(3)).isEqualTo(initialDueAt)
            assertThat(stmt.getLongOrNull(4)).isNull()
        }
        migratedDb.close()
    }

    @Test
    fun testMigration_6_7() {
        val activityId = faker.random.nextLong(min = 1, max = Long.MAX_VALUE)
        val activityCreatedAt = faker.random.randomPastDate().toInstant().toEpochMilli()
        val activityName = "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}"

        val initialDb = migrationTestHelper.createDatabase(DiswantinDatabase.DB_NAME, 6)
        initialDb.execSQL(
            "INSERT INTO activity (id, created_at, name) VALUES (?, ?, ?)",
            arrayOf(activityId, activityCreatedAt, activityName)
        )
        initialDb.close()

        val migratedDb =
            migrationTestHelper.runMigrationsAndValidate(
                DiswantinDatabase.DB_NAME,
                7,
                true,
                DiswantinDatabase.MIGRATION_6_7
            )
        migratedDb.query("SELECT * FROM activity_path").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getLong(0)).isPositive()
            assertThat(stmt.getLong(1)).isEqualTo(activityId)
            assertThat(stmt.getLong(2)).isEqualTo(activityId)
            assertThat(stmt.getInt(3)).isEqualTo(0)
        }
        migratedDb.close()
    }

    @Test
    fun testMigration_11_12() {
        val taskValues = List(6) {
            arrayOf(
                it + 1L,
                faker.random.randomPastDate().toInstant().toEpochMilli(),
                "${loremFaker.verbs.base()} ${loremFaker.lorem.words()}",
            )
        }
        val taskPathValues = taskValues.map { arrayOf(it[0], it[0], 0) } +
                listOf(
                    arrayOf(taskValues[0][0], taskValues[1][0], 1),
                    arrayOf(taskValues[0][0], taskValues[2][0], 2),
                    arrayOf(taskValues[1][0], taskValues[2][0], 1),
                    arrayOf(taskValues[3][0], taskValues[4][0], 1),
                )

        val initialDb = migrationTestHelper.createDatabase(DiswantinDatabase.DB_NAME, 11)
        taskValues.forEach {
            initialDb.execSQL("INSERT INTO `task` (id, created_at, name) VALUES (?, ?, ?)", it)
        }
        taskPathValues.forEach {
            initialDb.execSQL(
                "INSERT INTO `task_path` (ancestor, descendant, depth) VALUES (?, ?, ?)",
                it,
            )
        }
        initialDb.close()

        val migratedDb =
            migrationTestHelper.runMigrationsAndValidate(
                DiswantinDatabase.DB_NAME,
                12,
                true,
                DiswantinDatabase.MIGRATION_11_12,
            )
        migratedDb.query("SELECT COUNT(*) FROM `task_list`").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(2)
        }
        migratedDb.query("SELECT * FROM `task_list` ORDER BY `id`").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(1)
            assertThat(stmt.getString(1)).isEqualTo("List 1")

            assertThat(stmt.moveToNext()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(2)
            assertThat(stmt.getString(1)).isEqualTo("List 2")
        }
        migratedDb.query("SELECT `id`, `list_id` FROM `task` ORDER BY `id`").use { stmt ->
            assertThat(stmt.moveToFirst()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(1)
            assertThat(stmt.getLong(1)).isEqualTo(1)

            assertThat(stmt.moveToNext()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(2)
            assertThat(stmt.getLong(1)).isEqualTo(1)

            assertThat(stmt.moveToNext()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(3)
            assertThat(stmt.getLong(1)).isEqualTo(1)

            assertThat(stmt.moveToNext()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(4)
            assertThat(stmt.getLong(1)).isEqualTo(2)

            assertThat(stmt.moveToNext()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(5)
            assertThat(stmt.getLong(1)).isEqualTo(2)

            assertThat(stmt.moveToNext()).isTrue()
            assertThat(stmt.getLong(0)).isEqualTo(6)
            assertThat(stmt.getLongOrNull(1)).isNull()
        }
        migratedDb.close()
    }
}
