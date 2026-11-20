package com.example.hydralife.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hydration_goals")
data class HydrationGoalEntity(
    @PrimaryKey
    val dateString: String, // "YYYY-MM-DD"
    val goalMl: Int,
    val completed: Boolean
)
