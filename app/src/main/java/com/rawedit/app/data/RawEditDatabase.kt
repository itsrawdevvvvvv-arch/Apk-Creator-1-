package com.rawedit.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class,
        KeyframeSheetEntity::class,
        FixAttemptEntity::class,
        VideoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RawEditDatabase : RoomDatabase() {
    abstract fun dao(): RawEditDao

    companion object {
        @Volatile
        private var INSTANCE: RawEditDatabase? = null

        fun getInstance(context: Context): RawEditDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RawEditDatabase::class.java,
                    "rawedit.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
