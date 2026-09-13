package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_routines")
data class RoutineModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,            // e.g., "College Schedule", "Night Sleep", "Study Time"
    val modeType: String,         // "SLEEP", "CLASS", "STUDY", "DRIVING"
    val startTime: String,        // "HH:mm" e.g., "09:00"
    val endTime: String,          // "HH:mm" e.g., "16:00"
    val daysOfWeek: String,       // e.g., "MON,TUE,WED,THU,FRI"
    val autoSilenceNotifications: Boolean = true,
    val allowVipCalls: Boolean = true,
    val allowEmergencyRepeat: Boolean = true,
    val autoReplySms: Boolean = false,
    val autoReplyTemplate: String = "I'm currently busy. I will call you back soon.",
    val isEnabled: Boolean = true
)
