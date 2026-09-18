package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val taskType: String, // "CALL_REMINDER", "STUDY_REMINDER", "TIMETABLE_REMINDER", "CUSTOM_REMINDER"
    val targetData: String = "", // e.g., contact name or phone number
    val triggerTimeEpochMs: Long,
    val isExecuted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
