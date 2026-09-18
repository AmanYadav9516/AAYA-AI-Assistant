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

    // Note operations
    @Query("SELECT * FROM aaya_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<com.aaya.assistant.data.model.NoteItem>>

    @Query("SELECT * FROM aaya_notes WHERE category = :category ORDER BY timestamp DESC")
    fun getNotesByCategory(category: String): Flow<List<com.aaya.assistant.data.model.NoteItem>>

    @Query("SELECT * FROM aaya_notes WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getNotesByCategorySync(category: String): List<com.aaya.assistant.data.model.NoteItem>

    @Query("SELECT * FROM aaya_notes WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%'")
    suspend fun searchNotes(query: String): List<com.aaya.assistant.data.model.NoteItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: com.aaya.assistant.data.model.NoteItem): Long

    @Update
    suspend fun updateNote(note: com.aaya.assistant.data.model.NoteItem)

    @Delete
    suspend fun deleteNote(note: com.aaya.assistant.data.model.NoteItem)

    @Query("DELETE FROM aaya_notes WHERE category = 'Shopping' AND (content LIKE '%' || :itemName || '%' OR title LIKE '%' || :itemName || '%')")
    suspend fun removeShoppingItem(itemName: String): Int

    @Query("UPDATE aaya_notes SET isCompleted = 1 WHERE category = 'Shopping' AND (content LIKE '%' || :itemName || '%' OR title LIKE '%' || :itemName || '%')")
    suspend fun markShoppingItemCompleted(itemName: String): Int

    // Scheduled Task operations
    @Query("SELECT * FROM aaya_scheduled_tasks WHERE isExecuted = 0 ORDER BY triggerTimeEpochMs ASC")
    fun getPendingScheduledTasks(): Flow<List<com.aaya.assistant.data.model.ScheduledTask>>

    @Query("SELECT * FROM aaya_scheduled_tasks WHERE isExecuted = 0 ORDER BY triggerTimeEpochMs ASC")
    suspend fun getPendingScheduledTasksSync(): List<com.aaya.assistant.data.model.ScheduledTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTask(task: com.aaya.assistant.data.model.ScheduledTask): Long

    @Query("UPDATE aaya_scheduled_tasks SET isExecuted = 1 WHERE id = :id")
    suspend fun markTaskExecuted(id: Long)

    @Delete
    suspend fun deleteScheduledTask(task: com.aaya.assistant.data.model.ScheduledTask)

    // Timetable operations
    @Query("SELECT * FROM aaya_timetable WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getTimetableForDay(dayOfWeek: String): List<com.aaya.assistant.data.model.TimetableEntry>

    @Query("SELECT * FROM aaya_timetable ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllTimetable(): Flow<List<com.aaya.assistant.data.model.TimetableEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(entry: com.aaya.assistant.data.model.TimetableEntry): Long

    @Delete
    suspend fun deleteTimetable(entry: com.aaya.assistant.data.model.TimetableEntry)

    // Audit Log operations
    @Insert
    suspend fun insertAuditLog(item: com.aaya.assistant.data.model.AuditLogItem): Long

    @Query("SELECT * FROM aaya_audit_log WHERE timestamp >= :sinceEpochMs ORDER BY timestamp DESC")
    suspend fun getAuditLogsSince(sinceEpochMs: Long): List<com.aaya.assistant.data.model.AuditLogItem>

    @Query("SELECT * FROM aaya_audit_log ORDER BY timestamp DESC LIMIT 20")
    fun getRecentAuditLogs(): Flow<List<com.aaya.assistant.data.model.AuditLogItem>>
}
