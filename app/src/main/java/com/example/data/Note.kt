package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String = "",
    val tag: String = "Personal",     // "Work", "Personal", "Ideas", "Urgent"
    val colorHex: String = "Slate",    // "Slate", "Lavender", "Blue", "Mint", "Peach", "Amber"
    val checklistText: String = "",   // Markdown format checklist: "[ ] Task 1\n[x] Task 2"
    val createdAt: Long = System.currentTimeMillis()
) {
    data class Subtask(val text: String, val isChecked: Boolean)

    fun getSubtasks(): List<Subtask> {
        if (checklistText.isEmpty()) return emptyList()
        return checklistText.split("\n").filter { it.isNotBlank() }.map { line ->
            val isChecked = line.startsWith("[x]")
            val text = if (line.startsWith("[x]") || line.startsWith("[ ]")) {
                line.substring(3)
            } else {
                line
            }
            Subtask(text, isChecked)
        }
    }

    companion object {
        fun serializeSubtasks(list: List<Subtask>): String {
            return list.joinToString("\n") { "${if (it.isChecked) "[x]" else "[ ]"}${it.text}" }
        }
    }
}
