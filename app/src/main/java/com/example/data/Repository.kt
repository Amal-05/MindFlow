package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskAndNoteRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao,
    private val habitDao: HabitDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

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

    suspend fun insertHabit(habit: Habit) {
        habitDao.insertHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun updateHabitCompletion(id: Int, isCompleted: Boolean, streak: Int, date: String) {
        habitDao.updateHabitCompletion(id, isCompleted, streak, date)
    }
}
