package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String = "Health",  // "Health", "Mind", "Study", "Work"
    val streak: Int = 0,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String = "", // Format "yyyy-MM-dd"
    val triggerTime: String = "08:00 AM",
    val createdAt: Long = System.currentTimeMillis()
)
