package com.example.truthnudge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "claims") // Ensure "claims" is your desired table name
data class Claim(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L, // CHANGED to Long and 0L
    val text: String,
    val verdict: String,
    val reason: String,
    val source: String,
    val timestamp: Long
)