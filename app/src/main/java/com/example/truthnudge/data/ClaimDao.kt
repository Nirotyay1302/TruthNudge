package com.example.truthnudge.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClaimDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: Claim)

    // Full history
    @Query("SELECT * FROM claims ORDER BY timestamp DESC")
    suspend fun getAllClaims(): List<Claim>

    // Mini history preview (last N claims)
    @Query("SELECT * FROM claims ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLastNClaims(limit: Int): List<Claim>
    @Delete
    suspend fun deleteClaim(claim: Claim)
}
