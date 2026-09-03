package hs.project.steptune.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DayRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class StepTuneDatabase : RoomDatabase() {

    abstract fun dayRecordDao(): DayRecordDao

    companion object {
        const val DATABASE_NAME = "steptune.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `day_record` " +
                        "ADD COLUMN `measuredAtEpochMillis` INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE `day_record` " +
                        "SET `measuredAtEpochMillis` = COALESCE(" +
                        "CAST(strftime('%s', `date` || ' 23:59:59', 'utc') AS INTEGER) * 1000, " +
                        "0)"
                )
            }
        }
    }
}

