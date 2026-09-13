package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_memory")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "relationship", "routine", "preference", "fact"
    val key: String,      // e.g., "mother", "college_start", "study_time"
    val value: String,    // e.g., "MUMMY", "09:00", "19:00-21:00"
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)
