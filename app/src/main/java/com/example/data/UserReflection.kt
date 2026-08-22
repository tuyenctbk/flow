package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_reflections")
data class UserReflection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "Metacognitive Reflection",
    val content: String,
    val moodRating: Int = 3, // 1: Drained, 2: Restless, 3: Centered, 4: Energized, 5: Sublime Flow
    val frictionNotes: String = "",
    val breakthroughNotes: String = "",
    val tags: String = "General",
    val aiInsight: String = "",
    val linkedSessionId: Int? = null
)
