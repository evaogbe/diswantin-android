package io.github.evaogbe.diswantin.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.evaogbe.diswantin.activity.data.Activity
import io.github.evaogbe.diswantin.activity.data.ActivityDao
import io.github.evaogbe.diswantin.activity.data.ActivityFts

@Database(
    version = 4,
    entities = [Activity::class, ActivityFts::class],
    autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)]
)
@TypeConverters(Converters::class)
abstract class DiswantinDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

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

        fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                DiswantinDatabase::class.java,
                DB_NAME
            ).addMigrations(MIGRATION_1_2).build()
    }
}
