package com.example.data

import kotlinx.coroutines.flow.Flow

class DonorRepository(private val donorDao: DonorDao) {
    val allDonors: Flow<List<Donor>> = donorDao.getAllDonors()
    val allEmergencyRequests: Flow<List<EmergencyRequest>> = donorDao.getAllEmergencyRequests()

    suspend fun getDonorById(id: Int): Donor? {
        return donorDao.getDonorById(id)
    }

    suspend fun getDonorByPhone(mobile: String): Donor? {
        return donorDao.getDonorByPhone(mobile)
    }

    suspend fun insertDonor(donor: Donor): Long {
        return donorDao.insertDonor(donor)
    }

    suspend fun updateDonor(donor: Donor) {
        donorDao.updateDonor(donor)
    }

    suspend fun deleteDonor(donor: Donor) {
        donorDao.deleteDonor(donor)
    }

    suspend fun deleteDonorById(id: Int) {
        donorDao.deleteDonorById(id)
    }

    suspend fun deleteAllDonors() {
        donorDao.deleteAllDonors()
    }

    suspend fun insertEmergencyRequest(request: EmergencyRequest): Long {
        return donorDao.insertEmergencyRequest(request)
    }

    suspend fun deleteEmergencyRequest(request: EmergencyRequest) {
        donorDao.deleteEmergencyRequest(request)
    }

    suspend fun deleteAllEmergencyRequests() {
        donorDao.deleteAllEmergencyRequests()
    }
}
