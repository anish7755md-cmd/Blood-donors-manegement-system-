package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Donor
import com.example.data.EmergencyRequest
import com.example.viewmodel.DonorViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Global Constants for dropdown selections
val KarnatakaDistricts = listOf(
    "Bengaluru Urban", "Bengaluru Rural", "Mysuru", "Mandya", "Hassan",
    "Shivamogga", "Chikkamagaluru", "Tumakuru", "Belagavi", "Ballari",
    "Kolar", "Udupi", "Dakshina Kannada", "Dharwad", "Vijayapura",
    "Raichur", "Bidar", "Kalaburagi", "Kodagu", "Bagalkote",
    "Chamarajanagara", "Chikkaballapura", "Chitradurga", "Davanagere", "Gadag",
    "Haveri", "Koppal", "Ramanagara", "Uttara Kannada", "Yadgir", "Vijayanagara"
).sorted()

val BloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
val Genders = listOf("Male", "Female", "Other")

// WhatsApp integration opener helper
fun openWhatsApp(context: Context, phone: String, message: String) {
    var rawPhone = phone.trim()
    if (rawPhone.startsWith("+")) {
        rawPhone = rawPhone.substring(1)
    }
    if (rawPhone.length == 10) {
        rawPhone = "91$rawPhone"
    }
    try {
        val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$rawPhone&text=${android.net.Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp is not installed. Opening draft instead.", Toast.LENGTH_LONG).show()
    }
}

fun callPhone(context: Context, phone: String) {
    try {
        val uri = android.net.Uri.parse("tel:$phone")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch native dialer.", Toast.LENGTH_SHORT).show()
    }
}

fun openSharedFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.blooddonors.karnataka.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share/Save PDF Donor Directory"))
    } catch (e: Exception) {
        Toast.makeText(context, "Opening PDF failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// Custom Reusable Material 3 Dropdown Choice Box
@Composable
fun BloodSystemDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Expand $label"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Overlay clicking sensor
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// SCREEN 1: Home Page (Professional presentation banner, highlights, stats summary, and awareness details)
@Composable
fun HomeScreen(
    viewModel: DonorViewModel,
    darkModeActive: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val donors by viewModel.allDonors.collectAsStateWithLifecycle()
    val availableCount = donors.count { it.isAvailable }
    val alerts by viewModel.allEmergencyRequests.collectAsStateWithLifecycle()

    var preSelectedBloodGroup by remember { mutableStateOf("") }
    var preSelectedDistrict by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            
            // 1. EMERGENCY REQUEST BANNER (Designed HTML high-impact banner)
            if (alerts.isNotEmpty()) {
                val firstEmergency = alerts.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "EMERGENCY REQUEST",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${firstEmergency.bloodGroup} Needed",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Urgent",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = firstEmergency.hospitalName,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Urgency: ${firstEmergency.urgencyLevel}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.setBloodGroupQuery(firstEmergency.bloodGroup)
                                    viewModel.setDistrictQuery(firstEmergency.district)
                                    onNavigateToSearch()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Respond Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Default Hero banner to avoid empty screens
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "KARNATAKA BLOOD PORTAL",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Be A Hero. Save Lives.",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Heart Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Every blood donation saves lives. Instantly search local verified donors list in your district or become a donor yourself.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            OutlinedButton(
                                onClick = onNavigateToRegister,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. QUICK SEARCH DASHBOARD (Matches Designed HTML exactly)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Find Donors",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        
                        TextButton(
                            onClick = {
                                viewModel.clearSearchFilters()
                                onNavigateToSearch()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "View All Karnataka",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Blood group drop select
                        var showBgMenu by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showBgMenu = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Column {
                                    Text(
                                        text = "BLOOD GROUP",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (preSelectedBloodGroup.isEmpty()) "Any Group" else preSelectedBloodGroup,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = showBgMenu,
                                    onDismissRequest = { showBgMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Any Group") },
                                        onClick = {
                                            preSelectedBloodGroup = ""
                                            showBgMenu = false
                                        }
                                    )
                                    BloodGroups.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group) },
                                            onClick = {
                                                preSelectedBloodGroup = group
                                                showBgMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // District drop select
                        var showDistMenu by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable { showDistMenu = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Column {
                                    Text(
                                        text = "DISTRICT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (preSelectedDistrict.isEmpty()) "Any District" else preSelectedDistrict,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = showDistMenu,
                                    onDismissRequest = { showDistMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Any District") },
                                        onClick = {
                                            preSelectedDistrict = ""
                                            showDistMenu = false
                                        }
                                    )
                                    KarnatakaDistricts.forEach { dist ->
                                        DropdownMenuItem(
                                            text = { Text(dist) },
                                            onClick = {
                                                preSelectedDistrict = dist
                                                showDistMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            viewModel.setBloodGroupQuery(preSelectedBloodGroup)
                            viewModel.setDistrictQuery(preSelectedDistrict)
                            onNavigateToSearch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dashboard_search_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Database", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. STATS COUNT TICKERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Registered Donors", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${donors.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Available Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "$availableCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // 4. RECENTLY REGISTERED (Designed HTML donor row style feed)
            Text(
                text = "Recently Registered",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )
            
            if (donors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No donors registered yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val previewDonors = donors.take(3)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        previewDonors.forEachIndexed { index, donor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar Circle
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(Color(0xFFEADDFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = donor.bloodGroup,
                                        color = Color(0xFF21005D),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = donor.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${donor.city} • ${if (donor.isAvailable) "Available" else "Busy"}",
                                        fontSize = 11.sp,
                                        color = if (donor.isAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = if (donor.isAvailable) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { callPhone(context, donor.mobile) },
                                        modifier = Modifier
                                            .background(Color(0xFFD1E1FF), CircleShape)
                                            .size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Quick Call",
                                            tint = Color(0xFF1B3D82),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val bloodRequestMsg = "URGENT BLOOD REQUEST: " +
                                                    "Dear ${donor.fullName}, we found your profile on the Karnataka Blood Donors portal. " +
                                                    "We are in need of ${donor.bloodGroup} blood group urgently. Are you available for donation? " +
                                                    "Please respond! Thank you."
                                            openWhatsApp(context, donor.mobile, bloodRequestMsg)
                                        },
                                        modifier = Modifier
                                            .background(Color(0xFFE5FFEA), CircleShape)
                                            .size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Message,
                                            contentDescription = "Quick WhatsApp",
                                            tint = Color(0xFF114E18),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            if (index < previewDonors.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 5. THE AWARENESS SECTION
            Text(
                text = "Why Donate Blood?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )

            listOf(
                CardData("Save Lives in Karnataka", "Every year, thousands of patients in districts like Bengaluru, Mysuru, and Mangaluru encounter extreme shortages. One single donation can save up to 3 precious lives.", Icons.Default.FavoriteBorder),
                CardData("Maintain Blood Stocks", "Karnataka public health relies on spontaneous donors. This PBL portal provides real-time community assistance bypasses for local hospitals.", Icons.Default.LocalHospital),
                CardData("Health Advantages", "Regular donation helps adjust blood viscosity level balances, triggers fast new red blood cell production, and provides regular health checkpoints.", Icons.Default.FitnessCenter)
            ).forEach { card ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp).size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(card.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(card.body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

data class CardData(val title: String, val body: String, val icon: ImageVector)

// SCREEN 2: Search Donor Page (Dynamic donor filtering system with WhatsApp/Call launch)
@Composable
fun SearchScreen(
    viewModel: DonorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val donors by viewModel.filteredDonors.collectAsStateWithLifecycle()
    val allDonorsList by viewModel.allDonors.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val searchBg by viewModel.searchBloodGroup.collectAsStateWithLifecycle()
    val searchDist by viewModel.searchDistrict.collectAsStateWithLifecycle()
    val searchCity by viewModel.searchCity.collectAsStateWithLifecycle()
    val searchAvail by viewModel.searchOnlyAvailable.collectAsStateWithLifecycle()

    var donorToEdit by remember { mutableStateOf<Donor?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 36.dp)
    ) {
        // Collapsible / Outlined search filter panel
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Search & Filters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // Group and District inline filter dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BloodSystemDropdown(
                        label = "Blood Group",
                        options = listOf("") + BloodGroups,
                        selectedOption = searchBg,
                        onOptionSelected = { viewModel.setBloodGroupQuery(it) },
                        modifier = Modifier.weight(1f).testTag("search_blood_group_dropdown")
                    )

                    BloodSystemDropdown(
                        label = "District",
                        options = listOf("") + KarnatakaDistricts,
                        selectedOption = searchDist,
                        onOptionSelected = { viewModel.setDistrictQuery(it) },
                        modifier = Modifier.weight(1.2f).testTag("search_district_dropdown")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // City / Town Search field
                OutlinedTextField(
                    value = searchCity,
                    onValueChange = { viewModel.setCityQuery(it) },
                    label = { Text("Search by City / Town") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("search_city_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.setOnlyAvailable(!searchAvail) }
                    ) {
                        Checkbox(
                            checked = searchAvail,
                            onCheckedChange = { viewModel.setOnlyAvailable(it) },
                            modifier = Modifier.testTag("search_availability_checkbox")
                        )
                        Text("Show Available Only", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    // Clear Filters Action with smooth animation feedback
                    if (searchBg.isNotEmpty() || searchDist.isNotEmpty() || searchCity.isNotEmpty() || searchAvail) {
                        TextButton(
                            onClick = { viewModel.clearSearchFilters() },
                            modifier = Modifier.testTag("search_clear_button")
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Filters", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Searched count summary badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Emergency Directory Finder",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${donors.size} Matches Found",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("search_counter_badge")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Empty state UI if searched yields zero matches
        if (donors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "No donors",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No compatible donors found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your district or blood group filters.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // LazyColumn to render cards beautifully with hover/ripple effects
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("donor_results_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(donors) { donor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = donor.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${donor.gender}, ${donor.age} yrs",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (donor.email.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(donor.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // Highlighting Blood Group in beautiful Red badge
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                        .size(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = donor.bloodGroup,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Location Metadata
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, sizeResource = 14.dp, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${donor.city}, ${donor.district}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Availability Badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (donor.isAvailable) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (donor.isAvailable) "Available" else "Busy",
                                        color = if (donor.isAvailable) Color(0xFF2E7D32) else Color.DarkGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (donor.lastDonationDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Last donation: ${donor.lastDonationDate}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Communication Actions & Optional Administrative Options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Call Button
                                    IconButton(
                                        onClick = { callPhone(context, donor.mobile) },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call Donor", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }

                                    // WhatsApp Button
                                    IconButton(
                                        onClick = {
                                            val bloodRequestMsg = "URGENT BLOOD REQUEST: " +
                                                    "Dear ${donor.fullName}, we found your profile on the Karnataka Blood Donors portal. " +
                                                    "We are in need of ${donor.bloodGroup} blood group urgently. Are you available for donation? " +
                                                    "Please respond! Thank you."
                                            openWhatsApp(context, donor.mobile, bloodRequestMsg)
                                        },
                                        modifier = Modifier
                                            .background(Color(0xFF2E7D32).copy(alpha = 0.12f), CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "WhatsApp Donor", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Admin Tools (Visible ONLY if authorized Admin is logged in!)
                                if (isAdmin) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { donorToEdit = donor },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape)
                                                .size(36.dp),
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteDonor(donor)
                                                Toast.makeText(context, "Donor record deleted successfully", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                                                .size(36.dp),
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Profile", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Overlay Dialog for administrative donor modifications
    donorToEdit?.let { donor ->
        EditDonorDialog(
            donor = donor,
            onDismiss = { donorToEdit = null },
            onSave = { updatedDonor ->
                viewModel.updateDonor(
                    id = updatedDonor.id,
                    fullName = updatedDonor.fullName,
                    district = updatedDonor.district,
                    city = updatedDonor.city,
                    bloodGroup = updatedDonor.bloodGroup,
                    gender = updatedDonor.gender,
                    dob = updatedDonor.dob,
                    mobile = updatedDonor.mobile,
                    email = updatedDonor.email,
                    lastDonationDate = updatedDonor.lastDonationDate,
                    isAvailable = updatedDonor.isAvailable
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) {
                        donorToEdit = null
                    }
                }
            }
        )
    }
}

// Icon Resourcing fallback size
@Composable
fun RowScope.Icon(imageVector: ImageVector, contentDescription: String?, sizeResource: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(sizeResource))
}

// SCREEN 3: Registration module form (Includes automatic age calculation and validation safeguards)
@Composable
fun RegisterScreen(
    viewModel: DonorViewModel,
    onSuccessNavToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Bengaluru Urban") }
    var city by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var gender by remember { mutableStateOf("Male") }
    var dob by remember { mutableStateOf("") } // Date stored: YYYY-MM-DD
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var lastDonation by remember { mutableStateOf("") } // yyyy-MM-dd or blank
    var isAvailable by remember { mutableStateOf(true) }

    // On-the-fly calculated Age live value
    val liveAge = remember(dob) {
        if (dob.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            viewModel.calculateAge(dob)
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 56.dp)
    ) {
        Text(
            text = "Donor Registration Form",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Register as an active donor and join Karnataka's premier student-led PBL blood initiative.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Text Fields infilled structure
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("register_name_input"),
                    singleLine = true
                )

                // Dropdowns for districts and blood group
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BloodSystemDropdown(
                        label = "Blood Group *",
                        options = BloodGroups,
                        selectedOption = bloodGroup,
                        onOptionSelected = { bloodGroup = it },
                        modifier = Modifier.weight(1f).testTag("register_blood_group_dropdown")
                    )

                    BloodSystemDropdown(
                        label = "Gender *",
                        options = Genders,
                        selectedOption = gender,
                        onOptionSelected = { gender = it },
                        modifier = Modifier.weight(1f).testTag("register_gender_dropdown")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BloodSystemDropdown(
                        label = "Karnataka District *",
                        options = KarnatakaDistricts,
                        selectedOption = district,
                        onOptionSelected = { district = it },
                        modifier = Modifier.weight(1.2f).testTag("register_district_dropdown")
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Town *") },
                        modifier = Modifier.weight(1f).testTag("register_city_input"),
                        singleLine = true
                    )
                }

                // Birthday + Live Age side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD) *") },
                        placeholder = { Text("e.g. 1998-05-24") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.weight(1.4f).testTag("register_dob_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Display reactive live calculated age automatically!
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(56.dp)
                            .offset(y = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = liveAge?.toString() ?: "--",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    placeholder = { Text("10 digit contact") },
                    leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("register_mobile_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("register_email_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lastDonation,
                    onValueChange = { lastDonation = it },
                    label = { Text("Last Donation Date (Optional)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("register_last_donation_input"),
                    singleLine = true
                )

                // Availability status toggle switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Availability Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Deactivate if you have donated blood recently or are temporarily busy.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        modifier = Modifier.testTag("register_availability_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit form block
        Button(
            onClick = {
                viewModel.registerDonor(
                    fullName = name,
                    district = district,
                    city = city,
                    bloodGroup = bloodGroup,
                    gender = gender,
                    dob = dob,
                    mobile = mobile,
                    email = email,
                    lastDonationDate = lastDonation,
                    isAvailable = isAvailable
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        // Clear forms instantly on success
                        name = ""
                        city = ""
                        dob = ""
                        mobile = ""
                        email = ""
                        lastDonation = ""
                        isAvailable = true
                        onSuccessNavToSearch()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("register_submit_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Register Donor Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// SCREEN 4: Emergency Blood Requests Module (Allows posting urgent hospital needs with alerts)
@Composable
fun EmergencyScreen(
    viewModel: DonorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val requests by viewModel.allEmergencyRequests.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    var bloodGroup by remember { mutableStateOf("O+") }
    var hospitalName by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Bengaluru Urban") }
    var contactNumber by remember { mutableStateOf("") }
    var urgencyLevel by remember { mutableStateOf("Critical") }

    val urgencies = listOf("Critical", "High", "Medium", "Normal")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            text = "Emergency Requests Alert Board",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Post local hospital emergencies instantly. Nearby matches can find alerts and contact hospitals directly.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Emergency poster accordion/card
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            var isExpanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Post New Emergency Alert", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand poster"
                    )
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BloodSystemDropdown(
                            label = "Needed Blood *",
                            options = BloodGroups,
                            selectedOption = bloodGroup,
                            onOptionSelected = { bloodGroup = it },
                            modifier = Modifier.weight(1f).testTag("emergency_blood_dropdown")
                        )

                        BloodSystemDropdown(
                            label = "Urgency Level *",
                            options = urgencies,
                            selectedOption = urgencyLevel,
                            onOptionSelected = { urgencyLevel = it },
                            modifier = Modifier.weight(1.2f).testTag("emergency_urgency_dropdown")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = hospitalName,
                        onValueChange = { hospitalName = it },
                        label = { Text("Hospital Name & Details *") },
                        leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("emergency_hospital_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BloodSystemDropdown(
                            label = "Hospital District *",
                            options = KarnatakaDistricts,
                            selectedOption = district,
                            onOptionSelected = { district = it },
                            modifier = Modifier.weight(1.1f).testTag("emergency_district_dropdown")
                        )

                        OutlinedTextField(
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            label = { Text("Contact Number *") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f).testTag("emergency_contact_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.postEmergencyRequest(
                                bloodGroup = bloodGroup,
                                hospitalName = hospitalName,
                                district = district,
                                contactNumber = contactNumber,
                                urgencyLevel = urgencyLevel
                            ) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    // Reset & collapse form on successful submission
                                    hospitalName = ""
                                    contactNumber = ""
                                    isExpanded = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("emergency_post_submit_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Live Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Active Patient Requests Board", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No active requests",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No Active Emergency Alerts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("All hospital demands are satisfied. System is calm.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("emergency_alerts_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(requests) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Dynamic Urgency Color coding
                                        val badgeColor = when (req.urgencyLevel) {
                                            "Critical" -> Color.Red
                                            "High" -> Color(0xFFE65100)
                                            "Medium" -> Color(0xFFF57C00)
                                            else -> Color.Gray
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = req.urgencyLevel.uppercase(),
                                                color = badgeColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Formatted Date Posted
                                        val displayDate = remember(req.timestamp) {
                                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                                            sdf.format(Date(req.timestamp))
                                        }

                                        Text(text = "Posted: $displayDate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = req.hospitalName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, sizeResource = 13.dp, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(req.district, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // Required Blood Big Indicator
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                        .size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = req.bloodGroup,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { callPhone(context, req.contactNumber) },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                            .size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Dial", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val contactMsg = "EMERGENCY AID ALERT: " +
                                                    "Hello, I found your emergency post regarding $urgencyLevel need of ${req.bloodGroup} " +
                                                    "blood at ${req.hospitalName}, matches list. How can we coordinate to help immediately?"
                                            openWhatsApp(context, req.contactNumber, contactMsg)
                                        },
                                        modifier = Modifier
                                            .background(Color(0xFF2E7D32).copy(alpha = 0.12f), CircleShape)
                                            .size(34.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "WhatsApp", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                    }
                                }

                                if (isAdmin) {
                                    TextButton(
                                        onClick = {
                                            viewModel.deleteEmergencyRequest(req)
                                            Toast.makeText(context, "Emergency request closed.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.testTag("emergency_delete_btn")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 5: Admin Login and Dashboard Hub (Unlocks system metrics and exports reports seamlessly)
@Composable
fun DashboardAndLoginScreen(
    viewModel: DonorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val adminError by viewModel.adminError.collectAsStateWithLifecycle()

    val donors by viewModel.allDonors.collectAsStateWithLifecycle()
    val requests by viewModel.allEmergencyRequests.collectAsStateWithLifecycle()

    // Login Form parameters
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showConfirmWipe by remember { mutableStateOf(false) }

    if (!isAdmin) {
        // RENDER SECURE ADMIN LOGIN PANEL (Exact engineering guidelines: Admin ID: Blooddonorssystem & Password: blooddonor@123)
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                            .size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Admin Secure Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Authorize access to view analytics & modify listings",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Display errors cleanly if login fails
                    adminError?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    OutlinedTextField(
                        value = loginId,
                        onValueChange = { loginId = it },
                        label = { Text("Admin ID") },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("admin_id_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("admin_pw_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.performAdminLogin(loginId, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_login_submit"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Verify Authentication", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // ADMIN AUTHORIZED METRICS DASHBOARD
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 56.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Admin Board",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Real-time blood repository metrics dashboard",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sign out key
                IconButton(
                    onClick = { viewModel.performAdminLogout() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape)
                        .testTag("admin_logout_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action: Extract report triggers download shared channel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Official Report Compiler", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Extract complete registered donor directory list.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.downloadReport(context) { success, file, message ->
                                if (success && file != null) {
                                    openSharedFile(context, file)
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("report_compile_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Report", fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATS TILES
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardStatCard(
                    title = "Total Donors",
                    count = donors.size,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )

                DashboardStatCard(
                    title = "Available Donors",
                    count = donors.count { it.isAvailable },
                    color = Color(0xFF2E7D32),
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Blood group analytics distribution
            Text("Blood Group Statistics", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    BloodGroups.forEach { bg ->
                        val mathCount = donors.count { it.bloodGroup == bg }
                        val pct = if (donors.isNotEmpty()) mathCount.toFloat() / donors.size else 0f
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = bg, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(42.dp))
                            // Dynamic Linear Progress bar representing charts
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "$mathCount", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Karnataka District Leaderboard stats
            Text("Karnataka District Distribution", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val groupDistricts = donors.groupBy { it.district }.mapValues { it.value.size }.toList().sortedByDescending { it.second }.take(4)
                if (groupDistricts.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No location demographics recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        groupDistricts.forEachIndexed { rank, pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                            .size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${rank + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(pair.first, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }

                                Text("${pair.second} donors", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent donor registrations feed list
            Text("Recent Registered Users", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val feed = donors.takeLast(3).reversed()
                if (feed.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No records in directory yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        feed.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                                            .size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.fullName.first().toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.fullName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("${item.city}, ${item.district}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                    Text(item.bloodGroup, fontWeight = FontWeight.Black, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // DATABASE MAINTENANCE CONTROLS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permanent System Purge Controls",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wiping the database disables dummy data generation permanently. Only user-entered records will remain in the live blood repository.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!showConfirmWipe) {
                                    showConfirmWipe = true
                                } else {
                                    viewModel.clearDatabaseCompletely()
                                    showConfirmWipe = false
                                    Toast.makeText(context, "All previous data purged permanently!", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showConfirmWipe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("wipe_db_btn")
                        ) {
                            Text(
                                text = if (showConfirmWipe) "CONFIRM PURGE?" else "Wipe All Previous Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.restoreDemoTemplate()
                                showConfirmWipe = false
                                Toast.makeText(context, "Demo template records reloaded!", Toast.LENGTH_LONG).show()
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("restore_db_btn")
                        ) {
                            Text(
                                text = "Restore Template",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text(
                text = "$count",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

// Dialog Composable to perform editing securely of a Donor from Admin View
@Composable
fun EditDonorDialog(
    donor: Donor,
    onDismiss: () -> Unit,
    onSave: (Donor) -> Unit
) {
    var name by remember { mutableStateOf(donor.fullName) }
    var district by remember { mutableStateOf(donor.district) }
    var city by remember { mutableStateOf(donor.city) }
    var bloodGroup by remember { mutableStateOf(donor.bloodGroup) }
    var gender by remember { mutableStateOf(donor.gender) }
    var dob by remember { mutableStateOf(donor.dob) }
    var mobile by remember { mutableStateOf(donor.mobile) }
    var email by remember { mutableStateOf(donor.email) }
    var lastDonation by remember { mutableStateOf(donor.lastDonationDate) }
    var isAvailable by remember { mutableStateOf(donor.isAvailable) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Modify Donor Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BloodSystemDropdown(
                        label = "Blood Group",
                        options = BloodGroups,
                        selectedOption = bloodGroup,
                        onOptionSelected = { bloodGroup = it },
                        modifier = Modifier.weight(1f)
                    )

                    BloodSystemDropdown(
                        label = "Gender",
                        options = Genders,
                        selectedOption = gender,
                        onOptionSelected = { gender = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BloodSystemDropdown(
                        label = "Karnataka District",
                        options = KarnatakaDistricts,
                        selectedOption = district,
                        onOptionSelected = { district = it },
                        modifier = Modifier.weight(1.1f)
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City / Town") },
                        modifier = Modifier.weight(1.0f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lastDonation,
                    onValueChange = { lastDonation = it },
                    label = { Text("Last Donation Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available for donations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val modified = donor.copy(
                                fullName = name,
                                district = district,
                                city = city,
                                bloodGroup = bloodGroup,
                                gender = gender,
                                dob = dob,
                                mobile = mobile,
                                email = email,
                                lastDonationDate = lastDonation,
                                isAvailable = isAvailable
                            )
                            onSave(modified)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Update Database")
                    }
                }
            }
        }
    }
}
