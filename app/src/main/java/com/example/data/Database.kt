package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Int, isCompleted: Boolean)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Query("UPDATE habits SET isCompletedToday = :isCompleted, streak = :streak, lastCompletedDate = :date WHERE id = :id")
    suspend fun updateHabitCompletion(id: Int, isCompleted: Boolean, streak: Int, date: String)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUser(): UserProfile?

    @Query("SELECT * FROM user_profiles")
    fun getAllUsersFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)

    @Query("UPDATE user_profiles SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE user_profiles SET isLoggedIn = 1 WHERE email = :email")
    suspend fun setLoginStatus(email: String)

    @Delete
    suspend fun deleteUser(user: UserProfile)
}

@Database(entities = [Task::class, Note::class, Habit::class, UserProfile::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
    abstract fun userProfileDao(): UserProfileDao
}
