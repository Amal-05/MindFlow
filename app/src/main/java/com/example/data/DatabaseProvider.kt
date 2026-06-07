package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var dbInstance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return dbInstance ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "notes_tasks_database"
            )
            .fallbackToDestructiveMigration()
            .build()
            dbInstance = instance
            instance
        }
    }

    private var repoInstance: TaskAndNoteRepository? = null

    fun getRepository(context: Context): TaskAndNoteRepository {
        return repoInstance ?: synchronized(this) {
            val db = getDatabase(context)
            val instance = TaskAndNoteRepository(db.taskDao(), db.noteDao())
            repoInstance = instance
            instance
        }
    }
}
