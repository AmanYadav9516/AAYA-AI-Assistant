package com.aaya.assistant.data.local

import androidx.room.*
import com.aaya.assistant.data.model.MemoryItem
import com.aaya.assistant.data.model.RoutineModel
import com.aaya.assistant.data.model.VipContact
import kotlinx.coroutines.flow.Flow

@Dao
interface AayaDao {

    // Memory operations
    @Query("SELECT * FROM aaya_memory ORDER BY timestamp DESC")
    fun getAllMemory(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM aaya_memory WHERE category = :category")
    suspend fun getMemoryByCategory(category: String): List<MemoryItem>

    @Query("SELECT * FROM aaya_memory WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryItem): Long

    @Delete
    suspend fun deleteMemory(item: MemoryItem)

    @Query("DELETE FROM aaya_memory WHERE `key` = :key")
    suspend fun deleteMemoryByKey(key: String)

    // Routine operations
    @Query("SELECT * FROM aaya_routines ORDER BY id ASC")
    fun getAllRoutines(): Flow<List<RoutineModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineModel): Long

    @Update
    suspend fun updateRoutine(routine: RoutineModel)

    @Delete
    suspend fun deleteRoutine(routine: RoutineModel)

    @Query("SELECT * FROM aaya_routines WHERE isEnabled = 1")
    suspend fun getActiveRoutines(): List<RoutineModel>

    // VIP Contact operations
    @Query("SELECT * FROM aaya_vip_contacts ORDER BY name ASC")
    fun getAllVipContacts(): Flow<List<VipContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVipContact(contact: VipContact): Long

    @Delete
    suspend fun deleteVipContact(contact: VipContact)

    @Query("SELECT * FROM aaya_vip_contacts WHERE phoneNumber LIKE '%' || :phoneNumber || '%' LIMIT 1")
    suspend fun findVipByPhone(phoneNumber: String): VipContact?
}
