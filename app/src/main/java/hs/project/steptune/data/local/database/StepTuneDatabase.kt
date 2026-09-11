package hs.project.steptune.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DayRecordEntity::class, MusicRecommendationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class StepTuneDatabase : RoomDatabase() {

    abstract fun dayRecordDao(): DayRecordDao
    abstract fun musicRecommendationDao(): MusicRecommendationDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `music_recommendation` (" +
                        "`recommendationId` TEXT NOT NULL, " +
                        "`recordDate` TEXT NOT NULL, " +
                        "`todayStepCount` INTEGER NOT NULL, " +
                        "`recent7DayAverage` REAL NOT NULL, " +
                        "`recordedDayCount` INTEGER NOT NULL, " +
                        "`differenceFromAverage` REAL NOT NULL, " +
                        "`changeRatePercent` REAL, " +
                        "`activityLevel` TEXT NOT NULL, " +
                        "`musicMoods` TEXT NOT NULL, " +
                        "`genres` TEXT NOT NULL, " +
                        "`durationMinutes` INTEGER NOT NULL, " +
                        "`reason` TEXT NOT NULL, " +
                        "`youtubeSearchQuery` TEXT, " +
                        "`spotifySearchQuery` TEXT, " +
                        "`generatedAt` TEXT NOT NULL, " +
                        "PRIMARY KEY(`recommendationId`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `music_recommendation` ADD COLUMN `trackTitle` TEXT"
                )
                database.execSQL(
                    "ALTER TABLE `music_recommendation` ADD COLUMN `trackArtist` TEXT"
                )
                database.execSQL(
                    "ALTER TABLE `music_recommendation` ADD COLUMN `trackSearchQuery` TEXT"
                )
            }
        }
    }
}

