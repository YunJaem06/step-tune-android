package hs.project.steptune.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_record")
data class DayRecordEntity(
    @PrimaryKey val date: String,
    val steps: Int = 0,
    val goal: Int = 10_000,
    val distanceMeters: Float = 0f,
    val calories: Float = 0f
)

