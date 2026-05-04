package com.example.wellnessguide.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symptomName: String,
    val severity: String,
    val duration: String,
    val location: String,
    val notes: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isImproving: Boolean = false
)