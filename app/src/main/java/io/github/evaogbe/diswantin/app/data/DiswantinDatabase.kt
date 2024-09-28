package io.github.evaogbe.diswantin.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.evaogbe.diswantin.data.weekOfMonthField
import io.github.evaogbe.diswantin.task.data.Task
import io.github.evaogbe.diswantin.task.data.TaskCategory
import io.github.evaogbe.diswantin.task.data.TaskCategoryDao
import io.github.evaogbe.diswantin.task.data.TaskCategoryFts
import io.github.evaogbe.diswantin.task.data.TaskCompletion
import io.github.evaogbe.diswantin.task.data.TaskDao
import io.github.evaogbe.diswantin.task.data.TaskFts
import io.github.evaogbe.diswantin.task.data.TaskPath
import io.github.evaogbe.diswantin.task.data.TaskRecurrence
import io.github.evaogbe.diswantin.task.data.TaskSkip
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Database(
    version = 30,
    entities = [
        Task::class,
        TaskFts::class,
        TaskPath::class,
        TaskCategory::class,
        TaskCategoryFts::class,
        TaskCompletion::class,
        TaskRecurrence::class,
        TaskSkip::class,
    ],
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = DiswantinDatabase.Migration4To5::class),
        AutoMigration(from = 7, to = 8, spec = DiswantinDatabase.Migration7to8::class),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = DiswantinDatabase.Migration9to10::class),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 15, to = 16, spec = DiswantinDatabase.Migration15to16::class),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 18, to = 19, spec = DiswantinDatabase.Migration18to19::class),
        AutoMigration(from = 19, to = 20, spec = DiswantinDatabase.Migration19to20::class),
        AutoMigration(from = 21, to = 22, spec = DiswantinDatabase.Migration21to22::class),
        AutoMigration(from = 23, to = 24, spec = DiswantinDatabase.Migration23to24::class),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
    ]
)
@TypeConverters(Converters::class)
abstract class DiswantinDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun taskCategoryDao(): TaskCategoryDao

    @DeleteColumn(tableName = "activity", columnName = "skipped_at")
    class Migration4To5 : AutoMigrationSpec

    @RenameTable(fromTableName = "activity", toTableName = "task")
    @RenameTable(fromTableName = "activity_fts", toTableName = "task_fts")
    @RenameTable(fromTableName = "activity_path", toTableName = "task_path")
    class Migration7to8 : AutoMigrationSpec

    @RenameColumn(tableName = "task", fromColumnName = "due_at", toColumnName = "deadline")
    class Migration9to10 : AutoMigrationSpec

    @DeleteColumn(tableName = "task", columnName = "done_at")
    class Migration15to16 : AutoMigrationSpec

    @DeleteColumn(tableName = "task", columnName = "deadline")
    class Migration18to19 : AutoMigrationSpec

    @RenameTable(fromTableName = "task_list", toTableName = "task_category")
    @RenameColumn(tableName = "task", fromColumnName = "list_id", toColumnName = "category_id")
    class Migration19to20 : AutoMigrationSpec

    @DeleteColumn(tableName = "task", columnName = "scheduled_at")
    class Migration21to22 : AutoMigrationSpec

    @DeleteColumn(tableName = "task", columnName = "recurring")
    class Migration23to24 : AutoMigrationSpec

    companion object {
        const val DB_NAME = "diswantin"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `activity2` (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, created_at INTEGER NOT NULL, name TEXT NOT NULL)")
                db.execSQL("INSERT INTO `activity2` (created_at, name) SELECT created_at, name FROM `Activity`")
                db.execSQL("DROP TABLE `Activity`")
                db.execSQL("ALTER TABLE `activity2` RENAME TO `activity`")

                db.execSQL("DROP TABLE IF EXISTS `activity_fts`")
                db.execSQL("CREATE VIRTUAL TABLE `activity_fts` USING FTS4(`name` TEXT NOT NULL, content=`activity`)")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_activity_fts_BEFORE_UPDATE BEFORE UPDATE ON `activity` BEGIN DELETE FROM `activity_fts` WHERE `docid`=OLD.`rowid`; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_activity_fts_BEFORE_DELETE BEFORE DELETE ON `activity` BEGIN DELETE FROM `activity_fts` WHERE `docid`=OLD.`rowid`; END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_activity_fts_AFTER_UPDATE AFTER UPDATE ON `activity` BEGIN INSERT INTO `activity_fts`(`docid`, `name`) VALUES (NEW.`rowid`, NEW.`name`); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_activity_fts_AFTER_INSERT AFTER INSERT ON `activity` BEGIN INSERT INTO `activity_fts`(`docid`, `name`) VALUES (NEW.`rowid`, NEW.`name`); END")

                db.execSQL("INSERT INTO `activity_fts` (docid, name) SELECT id, name FROM `activity`")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE `activity2` 
                        |( id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
                        |, created_at INTEGER NOT NULL
                        |, name TEXT NOT NULL
                        |, due_at INTEGER DEFAULT NULL
                        |, scheduled_at INTEGER DEFAULT NULL
                        |, CHECK (due_at IS NULL or scheduled_at IS NULL)
                        |)""".trimMargin()
                )
                db.execSQL(
                    """INSERT INTO `activity2` 
                        |(id, created_at, name, due_at) 
                        |SELECT id, created_at, name, due_at FROM `activity`""".trimMargin()
                )
                db.execSQL("DROP TABLE `activity`")
                db.execSQL("ALTER TABLE `activity2` RENAME TO `activity`")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE `activity_path`
                        (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ancestor INTEGER NOT NULL
                            REFERENCES activity (id) ON DELETE CASCADE ON UPDATE CASCADE,
                        descendant INTEGER NOT NULL
                            REFERENCES activity (id) ON DELETE CASCADE ON UPDATE CASCADE,
                        depth INTEGER NOT NULL
                        )"""
                )
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_activity_path_ancestor_descendant`
                    ON `activity_path` (`ancestor`, `descendant`)"""
                )
                db.execSQL(
                    """INSERT INTO `activity_path`
                        (ancestor, descendant, depth)
                        SELECT id, id, 0
                        FROM `activity`"""
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val chainsByHead = mutableMapOf<Long, Set<Long>>()
                db.query(
                    """SELECT `p`.`ancestor`, `p`.`descendant`
                    FROM `task_path` `p`
                    JOIN (
                        SELECT `descendant`, MAX(`depth`) AS `depth`
                        FROM `task_path`
                        GROUP BY `descendant`
                    ) `tail` ON `tail`.`descendant` = `p`.`descendant`
                        AND `tail`.`depth` = `p`.`depth`
                    WHERE `p`.`depth` > 0"""
                )
                    .use { stmt ->
                        while (stmt.moveToNext()) {
                            val ancestor = stmt.getLong(0)
                            val descendant = stmt.getLong(1)
                            chainsByHead.compute(ancestor) { _, v ->
                                v.orEmpty() + setOf(ancestor, descendant)
                            }
                        }
                    }
                chainsByHead.entries.forEachIndexed { i, (_, taskIds) ->
                    val listId = db.insert(
                        "task_list",
                        SQLiteDatabase.CONFLICT_ABORT,
                        ContentValues().apply { put("name", "List ${i + 1}") },
                    )
                    db.execSQL(
                        "UPDATE `task` SET list_id = ? WHERE id IN (${taskIds.joinToString()})",
                        arrayOf(listId)
                    )
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE `task_completion`
                    (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `task_id` INTEGER NOT NULL
                        REFERENCES `task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
                    `done_at` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_task_completion_task_id_done_at`
                    ON `task_completion` (`task_id`, `done_at`)"""
                )
                db.execSQL(
                    """INSERT INTO `task_completion`
                    (task_id, done_at)
                    SELECT `id`, `done_at`
                    FROM `task`
                    WHERE `done_at` IS NOT NULL"""
                )
            }
        }

        fun getMigration17to18(zoneId: ZoneId = ZoneId.systemDefault()) =
            object : Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val taskValues = mutableListOf<String>()
                    db.query("SELECT `id`, `created_at`, `name`, `deadline` FROM `task` WHERE `deadline` IS NOT NULL")
                        .use { stmt ->
                            while (stmt.moveToNext()) {
                                val deadline =
                                    Instant.ofEpochMilli(stmt.getLong(3)).atZone(zoneId)
                                taskValues += "(%d, %d, \"%s\", \"%s\", \"%s\")".format(
                                    stmt.getLong(0),
                                    stmt.getLong(1),
                                    stmt.getString(2),
                                    deadline.toLocalDate(),
                                    deadline.toLocalTime()
                                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                                )
                            }
                        }
                    db.execSQL(
                        """INSERT INTO `task`
                            (`id`, `created_at`, `name`, `deadline_date`, `deadline_time`)
                            VALUES ${taskValues.joinToString()}
                            ON CONFLICT (`id`) DO UPDATE SET
                                `deadline_date` = excluded.`deadline_date`,
                                `deadline_time` = excluded.`deadline_time`"""
                    )
                }
            }

        fun getMigration20to21(zoneId: ZoneId = ZoneId.systemDefault()) =
            object : Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val taskValues = mutableListOf<String>()
                    db.query(
                        """SELECT `id`, `created_at`, `name`, `scheduled_at`
                        FROM `task`
                        WHERE `scheduled_at` IS NOT NULL"""
                    )
                        .use { stmt ->
                            while (stmt.moveToNext()) {
                                val scheduledAt =
                                    Instant.ofEpochMilli(stmt.getLong(3)).atZone(zoneId)
                                taskValues += "(%d, %d, \"%s\", \"%s\", \"%s\")".format(
                                    stmt.getLong(0),
                                    stmt.getLong(1),
                                    stmt.getString(2),
                                    scheduledAt.toLocalDate(),
                                    scheduledAt.toLocalTime()
                                        .format(DateTimeFormatter.ofPattern("HH:mm")),
                                )
                            }
                        }
                    db.execSQL("ALTER TABLE `task` ADD COLUMN `scheduled_date` TEXT")
                    db.execSQL("ALTER TABLE `task` ADD COLUMN `scheduled_time` TEXT")
                    db.execSQL(
                        """INSERT INTO `task`
                            (`id`, `created_at`, `name`, `scheduled_date`, `scheduled_time`)
                            VALUES ${taskValues.joinToString()}
                            ON CONFLICT (`id`) DO UPDATE SET
                                `scheduled_date` = excluded.`scheduled_date`,
                                `scheduled_time` = excluded.`scheduled_time`"""
                    )
                }
            }

        fun getMigration22To23(
            start: LocalDate = LocalDate.now(),
            locale: Locale = Locale.getDefault(),
        ) = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val week = start.get(weekOfMonthField(locale))
                db.execSQL(
                    """CREATE TABLE `task_recurrence` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `task_id` INTEGER NOT NULL
                            REFERENCES `task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
                        `start` TEXT NOT NULL,
                        `type` INTEGER NOT NULL,
                        `step` INTEGER NOT NULL,
                        `week` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE UNIQUE INDEX IF NOT EXISTS `index_task_recurrence_task_id_start`
                    ON `task_recurrence` (`task_id`, `start`)"""
                )
                db.execSQL(
                    """INSERT INTO `task_recurrence`
                    (`task_id`, `start`, `type`, `step`, `week`)
                    SELECT `id`, '$start', 0, 1, $week
                    FROM `task`
                    WHERE `recurring`"""
                )
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """INSERT OR IGNORE INTO `task_category_fts`
                    (`name`)
                    SELECT `name`
                    FROM `task_category`"""
                )
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO `task_category_fts`(`task_category_fts`) VALUES('rebuild')")
            }
        }

        fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                DiswantinDatabase::class.java,
                DB_NAME,
            ).addMigrations(
                MIGRATION_1_2,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_11_12,
                MIGRATION_14_15,
                getMigration17to18(),
                getMigration20to21(),
                getMigration22To23(),
                MIGRATION_26_27,
                MIGRATION_27_28,
            ).build()
    }
}
