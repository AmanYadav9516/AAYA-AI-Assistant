package com.aaya.assistant.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aaya_vip_contacts")
data class VipContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,             // e.g., "Mummy", "Papa", "Boss"
    val phoneNumber: String,      // Normalized phone number
    val relationship: String,     // "Mother", "Father", "Spouse", "Doctor", "Work"
    val canBypassSleep: Boolean = true,
    val canBypassClass: Boolean = true
)
