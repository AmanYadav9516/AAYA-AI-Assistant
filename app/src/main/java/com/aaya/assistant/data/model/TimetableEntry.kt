package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_timetable")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val subject: String,   // e.g. "Robotics Lab", "Operating Systems"
    val startTime: String, // e.g. "10:00"
    val endTime: String,   // e.g. "11:30"
    val room: String = "", // e.g. "Block C, Lab 3"
    val notes: String = ""
)
