package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskAndNoteRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(id, isCompleted)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }
}
