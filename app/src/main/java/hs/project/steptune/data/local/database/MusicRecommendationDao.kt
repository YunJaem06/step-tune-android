package hs.project.steptune.data.local.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicRecommendationDao {
    @Upsert
    suspend fun upsert(recommendation: MusicRecommendationEntity)

    @Query("SELECT * FROM music_recommendation ORDER BY generatedAt DESC")
    fun observeAll(): Flow<List<MusicRecommendationEntity>>

    @Query("DELETE FROM music_recommendation")
    suspend fun deleteAll()
}
