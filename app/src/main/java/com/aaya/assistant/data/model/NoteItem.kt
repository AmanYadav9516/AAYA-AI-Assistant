package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String,
    val category: String = "Notes", // "Notes", "Shopping", "Ideas", "College", "Personal", "Important"
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
