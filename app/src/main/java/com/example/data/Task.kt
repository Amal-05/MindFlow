package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val noteText: String = "",
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // "Low", "Medium", "High", "Critical"
    val tag: String = "Personal",     // "Urgent", "Exam", "Project", "Family", "Personal"
    val category: String = "Personal", // "Personal", "Work", "Study", "Shopping", "Health", "Finance"
    val dueDateLong: Long = 0L,       // 0L means no due date
    val dueTime: String = "12:00 PM",
    val status: String = "Pending",   // "Pending", "In Progress", "Completed", "Cancelled", "Overdue"
    val subtasksText: String = "",   // Checklist format checklist: "[ ] Task 1\n[x] Task 2"
    val recurringOption: String = "None", // "None", "Daily", "Weekly", "Monthly", "Yearly"
    val estimatedTimeMinutes: Int = 30,
    val reminderMinutesBefore: Int = -1, // -1 means none, 0 exact, 15 means 15m before, etc.
    val dependencyTaskId: Int = 0,    // 0 means no dependency
    val createdAt: Long = System.currentTimeMillis()
) {
    data class Subtask(val text: String, val isChecked: Boolean)

    fun getSubtasks(): List<Subtask> {
        if (subtasksText.isEmpty()) return emptyList()
        return subtasksText.split("\n").filter { it.isNotBlank() }.map { line ->
            val isChecked = line.startsWith("[x]")
            val text = if (line.startsWith("[x]") || line.startsWith("[ ]")) {
                line.substring(3)
            } else {
                line
            }
            Subtask(text, isChecked)
        }
    }

    fun getProgressPercentage(): Int {
        val list = getSubtasks()
        if (list.isEmpty()) return if (isCompleted) 100 else 0
        val done = list.count { it.isChecked }
        return (done * 100) / list.size
    }

    companion object {
        fun serializeSubtasks(list: List<Subtask>): String {
            return list.joinToString("\n") { "${if (it.isChecked) "[x]" else "[ ]"}${it.text}" }
        }
    }
}
