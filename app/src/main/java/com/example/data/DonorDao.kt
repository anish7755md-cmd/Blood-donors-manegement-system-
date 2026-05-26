package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DonorDao {
    // Donor operations
    @Query("SELECT * FROM donors ORDER BY fullName ASC")
    fun getAllDonors(): Flow<List<Donor>>

    @Query("SELECT * FROM donors WHERE id = :id LIMIT 1")
    suspend fun getDonorById(id: Int): Donor?

    @Query("SELECT * FROM donors WHERE mobile = :mobile LIMIT 1")
    suspend fun getDonorByPhone(mobile: String): Donor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonor(donor: Donor): Long

    @Update
    suspend fun updateDonor(donor: Donor)

    @Delete
    suspend fun deleteDonor(donor: Donor)

    @Query("DELETE FROM donors WHERE id = :id")
    suspend fun deleteDonorById(id: Int)

    @Query("DELETE FROM donors")
    suspend fun deleteAllDonors()

    // Emergency blood requests operations
    @Query("SELECT * FROM emergency_requests ORDER BY timestamp DESC")
    fun getAllEmergencyRequests(): Flow<List<EmergencyRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyRequest(request: EmergencyRequest): Long

    @Delete
    suspend fun deleteEmergencyRequest(request: EmergencyRequest)

    @Query("DELETE FROM emergency_requests")
    suspend fun deleteAllEmergencyRequests()
}
