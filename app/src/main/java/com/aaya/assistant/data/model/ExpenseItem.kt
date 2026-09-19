package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_expenses")
data class ExpenseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String, // "Food", "Travel", "College", "Shopping", "Bills", "General"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
