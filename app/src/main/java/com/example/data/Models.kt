package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "donors")
data class Donor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fullName: String,
    val district: String,
    val city: String,
    val bloodGroup: String,
    val gender: String,
    val dob: String,
    val age: Int,
    val mobile: String,
    val email: String,
    val lastDonationDate: String,
    val isAvailable: Boolean
)

@Entity(tableName = "emergency_requests")
data class EmergencyRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bloodGroup: String,
    val hospitalName: String,
    val district: String,
    val contactNumber: String,
    val urgencyLevel: String, // Critical, High, Medium, Low
    val timestamp: Long = System.currentTimeMillis()
)
