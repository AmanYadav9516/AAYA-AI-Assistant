package com.aaya.assistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aaya.assistant.data.model.AuditLogItem
import com.aaya.assistant.data.model.MemoryItem
import com.aaya.assistant.data.model.NoteItem
import com.aaya.assistant.data.model.RoutineModel
import com.aaya.assistant.data.model.ScheduledTask
import com.aaya.assistant.data.model.TimetableEntry
import com.aaya.assistant.data.model.VipContact

@Database(
    entities = [
        MemoryItem::class,
        RoutineModel::class,
        VipContact::class,
        NoteItem::class,
        ScheduledTask::class,
        TimetableEntry::class,
        AuditLogItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AayaDatabase : RoomDatabase() {

    abstract fun aayaDao(): AayaDao

    companion object {
        @Volatile
        private var INSTANCE: AayaDatabase? = null

        fun getInstance(context: Context): AayaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AayaDatabase::class.java,
                    "aaya_assistant.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
