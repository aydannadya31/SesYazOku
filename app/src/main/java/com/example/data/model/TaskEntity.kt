package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long? = null,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
