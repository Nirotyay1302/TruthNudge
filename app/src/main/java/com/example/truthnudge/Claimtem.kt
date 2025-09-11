package com.example.truthnudge

data class ClaimItem(
    val id: Long, // Add the ID from the database entity
    val text: String, // This can be the formatted display text
    val timestamp: Long
)