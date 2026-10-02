package hs.project.steptune.data.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DayRecordDao {

    @Query("SELECT * FROM day_record WHERE date = :date LIMIT 1")
    fun observeDayRecord(date: String): Flow<DayRecordEntity?>

    @Query("SELECT * FROM day_record WHERE date = :date LIMIT 1")
    suspend fun getDayRecord(date: String): DayRecordEntity?

    @Query("SELECT * FROM day_record WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeDayRecordsBetween(startDate: String, endDate: String): Flow<List<DayRecordEntity>>

    @Upsert
    suspend fun upsert(record: DayRecordEntity)

    @Query("DELETE FROM day_record")
    suspend fun deleteAll()
}

