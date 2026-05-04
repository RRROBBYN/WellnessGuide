package com.example.wellnessguide.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SymptomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(symptom: SymptomEntity)

    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    fun getAllSymptoms(): LiveData<List<SymptomEntity>>

    @Query("SELECT * FROM symptoms WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getRecentSymptoms(since: Long): List<SymptomEntity>

    @Delete
    suspend fun delete(symptom: SymptomEntity)
}