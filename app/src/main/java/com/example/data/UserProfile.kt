package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val email: String,
    val displayName: String,
    val photoUrl: String = "",
    val bio: String = "Designing the future, one task at a time.",
    val emoji: String = "🚀",
    val isLoggedIn: Boolean = false,
    val themeDark: Boolean = true,
    val appLockPin: String = "",
    val notifyIntervalMinutes: Int = 30,
    val joinedAt: Long = System.currentTimeMillis()
)
