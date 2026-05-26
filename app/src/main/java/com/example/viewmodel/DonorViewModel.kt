package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Donor
import com.example.data.DonorRepository
import com.example.data.EmergencyRequest
import com.example.utils.PdfGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DonorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DonorRepository
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DonorRepository(db.donorDao())
    }

    // Reactively collect lists from standard Room tables
    val allDonors: StateFlow<List<Donor>> = repository.allDonors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEmergencyRequests: StateFlow<List<EmergencyRequest>> = repository.allEmergencyRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search filters
    private val _searchBloodGroup = MutableStateFlow("")
    val searchBloodGroup: StateFlow<String> = _searchBloodGroup

    private val _searchDistrict = MutableStateFlow("")
    val searchDistrict: StateFlow<String> = _searchDistrict

    private val _searchCity = MutableStateFlow("")
    val searchCity: StateFlow<String> = _searchCity

    private val _searchOnlyAvailable = MutableStateFlow(false)
    val searchOnlyAvailable: StateFlow<Boolean> = _searchOnlyAvailable

    // Combined filtered donors based on state
    val filteredDonors: StateFlow<List<Donor>> = combine(
        allDonors,
        _searchBloodGroup,
        _searchDistrict,
        _searchCity,
        _searchOnlyAvailable
    ) { donors, bg, dist, city, onlyAvail ->
        donors.filter { donor ->
            val matchBg = bg.isEmpty() || donor.bloodGroup.equals(bg, ignoreCase = true)
            val matchDist = dist.isEmpty() || donor.district.equals(dist, ignoreCase = true)
            val matchCity = city.isEmpty() || donor.city.contains(city, ignoreCase = true)
            val matchAvail = !onlyAvail || donor.isAvailable
            matchBg && matchDist && matchCity && matchAvail
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin authorization state
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError

    fun setBloodGroupQuery(query: String) { _searchBloodGroup.value = query }
    fun setDistrictQuery(query: String) { _searchDistrict.value = query }
    fun setCityQuery(query: String) { _searchCity.value = query }
    fun setOnlyAvailable(available: Boolean) { _searchOnlyAvailable.value = available }

    fun clearSearchFilters() {
        _searchBloodGroup.value = ""
        _searchDistrict.value = ""
        _searchCity.value = ""
        _searchOnlyAvailable.value = false
    }

    // Admin login validation (Matches EXACT engineering requirements: Admin ID: Blooddonorssystem / Password: blooddonor@123)
    fun performAdminLogin(idInput: String, passwordInput: String): Boolean {
        _adminError.value = null
        if (idInput == "Blooddonorssystem" && passwordInput == "blooddonor@123") {
            _isAdmin.value = true
            return true
        } else {
            _adminError.value = "Invalid Admin ID or Password. Please try again."
            _isAdmin.value = false
            return false
        }
    }

    fun performAdminLogout() {
        _isAdmin.value = false
        _adminError.value = null
    }

    // Safe auto calculated age based on BirthDate string pattern: YYYY-MM-DD
    fun calculateAge(dobString: String): Int {
        if (dobString.isBlank()) return 0
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dob = format.parse(dobString) ?: return 0
            val dobCal = Calendar.getInstance().apply { time = dob }
            val today = Calendar.getInstance()
            var calculatedAge = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                calculatedAge--
            }
            calculatedAge.coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    // Registration and duplicate validation validation flow
    fun registerDonor(
        fullName: String,
        district: String,
        city: String,
        bloodGroup: String,
        gender: String,
        dob: String, // String: yyyy-MM-dd
        mobile: String,
        email: String,
        lastDonationDate: String,
        isAvailable: Boolean,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        // Required field checks
        if (fullName.isBlank() || district.isBlank() || city.isBlank() || bloodGroup.isBlank() || gender.isBlank() || dob.isBlank() || mobile.isBlank()) {
            onResult(false, "Please fill in all mandatory fields.")
            return
        }

        // Phone Validation (must be numeric & at least 10 digits)
        if (!mobile.matches(Regex("^[0-9]{10,12}$"))) {
            onResult(false, "Please enter a valid 10-12 digit mobile number.")
            return
        }

        // Email verification if non-blank
        if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onResult(false, "Please provide a valid email address.")
            return
        }

        // Run async validations checks on Room DB
        viewModelScope.launch {
            try {
                val existingDonor = repository.getDonorByPhone(mobile)
                if (existingDonor != null) {
                    onResult(false, "A donor with this mobile number ($mobile) is already registered!")
                    return@launch
                }

                // Add calculated age
                val age = calculateAge(dob)
                val newDonor = Donor(
                    fullName = fullName.trim(),
                    district = district,
                    city = city.trim(),
                    bloodGroup = bloodGroup,
                    gender = gender,
                    dob = dob,
                    age = age,
                    mobile = mobile.trim(),
                    email = email.trim(),
                    lastDonationDate = lastDonationDate,
                    isAvailable = isAvailable
                )

                val id = repository.insertDonor(newDonor)
                if (id > 0) {
                    onResult(true, "Donor '$fullName' registered successfully!")
                } else {
                    onResult(false, "Failed to register donor. Database error.")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    // Update single donor
    fun updateDonor(
        id: Int,
        fullName: String,
        district: String,
        city: String,
        bloodGroup: String,
        gender: String,
        dob: String,
        mobile: String,
        email: String,
        lastDonationDate: String,
        isAvailable: Boolean,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        if (fullName.isBlank() || district.isBlank() || city.isBlank() || bloodGroup.isBlank() || gender.isBlank() || dob.isBlank() || mobile.isBlank()) {
            onResult(false, "Please fill in all mandatory fields.")
            return
        }

        if (!mobile.matches(Regex("^[0-9]{10,12}$"))) {
            onResult(false, "Please enter a valid 10-12 digit mobile number.")
            return
        }

        viewModelScope.launch {
            try {
                // Confirm number does not conflict with another donor ID
                val existingDonor = repository.getDonorByPhone(mobile)
                if (existingDonor != null && existingDonor.id != id) {
                    onResult(false, "Another registered donor already uses this mobile number!")
                    return@launch
                }

                val age = calculateAge(dob)
                val updatedDonor = Donor(
                    id = id,
                    fullName = fullName.trim(),
                    district = district,
                    city = city.trim(),
                    bloodGroup = bloodGroup,
                    gender = gender,
                    dob = dob,
                    age = age,
                    mobile = mobile.trim(),
                    email = email.trim(),
                    lastDonationDate = lastDonationDate,
                    isAvailable = isAvailable
                )

                repository.updateDonor(updatedDonor)
                onResult(true, "Donor profile modified successfully!")
            } catch (e: Exception) {
                onResult(false, "Update failed: ${e.localizedMessage}")
            }
        }
    }

    fun deleteDonor(donor: Donor) {
        viewModelScope.launch {
            repository.deleteDonor(donor)
        }
    }

    // Post emergency request validation flow
    fun postEmergencyRequest(
        bloodGroup: String,
        hospitalName: String,
        district: String,
        contactNumber: String,
        urgencyLevel: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        if (bloodGroup.isBlank() || hospitalName.isBlank() || district.isBlank() || contactNumber.isBlank() || urgencyLevel.isBlank()) {
            onResult(false, "All details are required to post an emergency blood alert.")
            return
        }

        if (!contactNumber.matches(Regex("^[0-9]{10,12}$"))) {
            onResult(false, "Please deliver a valid 10-12 digit emergency contact number.")
            return
        }

        viewModelScope.launch {
            try {
                val req = EmergencyRequest(
                    bloodGroup = bloodGroup,
                    hospitalName = hospitalName.trim(),
                    district = district,
                    contactNumber = contactNumber.trim(),
                    urgencyLevel = urgencyLevel
                )
                val id = repository.insertEmergencyRequest(req)
                if (id > 0) {
                    onResult(true, "Emergency request posted successfully!")
                } else {
                    onResult(false, "Failed to submit request to database.")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    fun deleteEmergencyRequest(request: EmergencyRequest) {
        viewModelScope.launch {
            repository.deleteEmergencyRequest(request)
        }
    }

    // Report generation intent launcher
    fun downloadReport(context: Context, onResult: (success: Boolean, file: File?, message: String) -> Unit) {
        viewModelScope.launch {
            val list = allDonors.value
            if (list.isEmpty()) {
                onResult(false, null, "Cannot generate report: No donor reports found in the directory.")
                return@launch
            }

            val file = PdfGenerator.generateDonorReport(context, list)
            if (file != null) {
                onResult(true, file, "PDF Report successfully generated at cache.")
            } else {
                onResult(false, null, "Failed to compile PDF Report.")
            }
        }
    }

    // Clear db structure and pre-populate with mock details for Karnataka presentation
    fun seedSampleKarnatakaDonors() {
        viewModelScope.launch {
            try {
                val isSeedDisabled = prefs.getBoolean("seed_disabled", false)
                if (isSeedDisabled) {
                    Log.d("DonorViewModel", "Seeding is disabled permanently by admin action.")
                    return@launch
                }
                val currentList = repository.allDonors.first()
                if (currentList.isEmpty()) {
                    val list = listOf(
                        Donor(fullName = "Ramesh Kumar", district = "Bengaluru Urban", city = "Jayanagar", bloodGroup = "O+", gender = "Male", dob = "1994-05-12", age = 32, mobile = "9876543210", email = "ramesh@gmail.com", lastDonationDate = "2026-01-20", isAvailable = true),
                        Donor(fullName = "Priya Shetty", district = "Dakshina Kannada", city = "Mangaluru", bloodGroup = "B+", gender = "Female", dob = "1997-11-23", age = 28, mobile = "9845112233", email = "priya.shetty@yahoo.com", lastDonationDate = "2025-12-15", isAvailable = true),
                        Donor(fullName = "Anand Hegde", district = "Udupi", city = "Kundapura", bloodGroup = "A-", gender = "Male", dob = "1989-08-05", age = 36, mobile = "9448003344", email = "anand_hegde@hotmail.com", lastDonationDate = "2026-03-01", isAvailable = false),
                        Donor(fullName = "Suresh Gowda", district = "Mysuru", city = "Gokulam", bloodGroup = "AB+", gender = "Male", dob = "2001-03-30", age = 25, mobile = "9980556677", email = "suresh.g@gmail.com", lastDonationDate = "2025-10-10", isAvailable = true),
                        Donor(fullName = "Kavitha Patil", district = "Dharwad", city = "Hubballi", bloodGroup = "O-", gender = "Female", dob = "1993-01-15", age = 33, mobile = "9620889900", email = "kavitha.p@outlook.com", lastDonationDate = "2026-02-18", isAvailable = true),
                        Donor(fullName = "Naveen Naik", district = "Shivamogga", city = "Sagar", bloodGroup = "B-", gender = "Male", dob = "1996-07-22", age = 29, mobile = "8892445566", email = "naveen.n@gmail.com", lastDonationDate = "2025-11-05", isAvailable = true)
                    )
                    list.forEach { repository.insertDonor(it) }

                    val alerts = listOf(
                        EmergencyRequest(bloodGroup = "O-", hospitalName = "Narayana Hrudayalaya, Bengaluru", district = "Bengaluru Urban", contactNumber = "9900887766", urgencyLevel = "Critical"),
                        EmergencyRequest(bloodGroup = "A+", hospitalName = "KMC Hospital, Mangaluru", district = "Dakshina Kannada", contactNumber = "8877665544", urgencyLevel = "High")
                    )
                    alerts.forEach { repository.insertEmergencyRequest(it) }
                }
            } catch (e: Exception) {
                Log.e("DonorViewModel", "Error seeding sample donors database", e)
            }
        }
    }

    // Purge previous details permanently and save new details
    fun clearDatabaseCompletely() {
        viewModelScope.launch {
            try {
                prefs.edit().putBoolean("seed_disabled", true).apply()
                repository.deleteAllDonors()
                repository.deleteAllEmergencyRequests()
                Log.d("DonorViewModel", "Database successfully and completely cleared.")
            } catch (e: Exception) {
                Log.e("DonorViewModel", "Error purging previous database details", e)
            }
        }
    }

    // Reset preference and restore the mock database template if desired
    fun restoreDemoTemplate() {
        viewModelScope.launch {
            try {
                prefs.edit().putBoolean("seed_disabled", false).apply()
                seedSampleKarnatakaDonors()
            } catch (e: Exception) {
                Log.e("DonorViewModel", "Error restoring mock template database details", e)
            }
        }
    }
}
