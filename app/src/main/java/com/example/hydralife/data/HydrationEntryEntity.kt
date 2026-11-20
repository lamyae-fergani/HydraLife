package com.example.hydralife.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hydration_entries")
data class HydrationEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    val amountMl: Int,
    val session: String, // "Morning", "Afternoon", "Evening"
    val dateString: String // "YYYY-MM-DD"
)
