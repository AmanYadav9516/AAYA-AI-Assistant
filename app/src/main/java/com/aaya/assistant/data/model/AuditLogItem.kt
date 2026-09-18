package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_audit_log")
data class AuditLogItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // "ALARM", "REMINDER", "NOTE", "MODE", "CALL", "CAMERA", "AI_REQUEST"
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)
