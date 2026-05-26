package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DonorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DonorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-Edge full render experience
        enableEdgeToEdge()

        // Populates standard local Karnataka database with presentation examples
        viewModel.seedSampleKarnatakaDonors()

        setContent {
            // System level theme fallback matching
            val systemDark = isSystemInDarkTheme()
            var darkModeActive by remember { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = darkModeActive) {
                var currentTab by remember { mutableStateOf("home") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        JeevaRakshakaTopBar(
                            darkModeActive = darkModeActive,
                            onToggleDarkMode = { darkModeActive = it }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("main_bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 8.dp
                        ) {
                            val tabs = listOf(
                                NavItem("home", "Home", Icons.Default.Home),
                                NavItem("search", "Search DB", Icons.Default.Search),
                                NavItem("register", "Register", Icons.Default.PersonAdd),
                                NavItem("emergency", "Emergency", Icons.Default.Campaign),
                                NavItem("dashboard", "Dashboard", Icons.Default.Dashboard)
                            )

                            tabs.forEach { tab ->
                                val selected = currentTab == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab.route },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.tertiary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_item_${tab.route}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Beautiful fade-in transit layouts
                    Crossfade(
                        targetState = currentTab,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "ScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            "home" -> HomeScreen(
                                viewModel = viewModel,
                                darkModeActive = darkModeActive,
                                onToggleDarkMode = { darkModeActive = it },
                                onNavigateToSearch = { currentTab = "search" },
                                onNavigateToRegister = { currentTab = "register" }
                            )
                            "search" -> SearchScreen(viewModel = viewModel)
                            "register" -> RegisterScreen(
                                viewModel = viewModel,
                                onSuccessNavToSearch = { currentTab = "search" }
                            )
                            "emergency" -> EmergencyScreen(viewModel = viewModel)
                            "dashboard" -> DashboardAndLoginScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JeevaRakshakaTopBar(
    darkModeActive: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heart-beat / Love Shield Emblem matching SVG
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Blood donors manegement system Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // App Name / Header Title
            Text(
                text = "Blood donors manegement system",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Karnataka Location pill tag matches redesigned HTML badge dynamically
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "KARNATAKA",
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // Convenient Universal theme-switcher icon
            IconButton(
                onClick = { onToggleDarkMode(!darkModeActive) },
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (darkModeActive) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Theme Switcher",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)
