package com.example.truthnudge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "claims")
data class Claim(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val verdict: String,
    val reason: String,
    val source: String,
    val timestamp: Long
)