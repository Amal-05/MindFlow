package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val noteText: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // "High", "Medium", "Low"
    val tag: String = "Personal",     // "Work", "Personal", "Ideas", "Urgent"
    val dueDateLong: Long = 0L,       // 0L means no due date
    val createdAt: Long = System.currentTimeMillis()
)
