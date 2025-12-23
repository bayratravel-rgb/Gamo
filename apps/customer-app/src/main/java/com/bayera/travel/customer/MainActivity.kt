package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize Firebase & Maps safely (Prevent Crash)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        val osmPrefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, osmPrefs)
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            // Live Dark Mode State
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_mode", isSystemDark)) }
            val colors = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colors) {
                val navController = rememberNavController()
                val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_home"

                NavHost(navController = navController, startDestination = startScreen) {
                    // 2. Route to External Files (Clean & Modular)
                    composable("login") { LoginScreen(navController) }
                    composable("super_home") { SuperAppHome(navController) }
                    composable("ride_home") { RideScreen(navController) }
                    composable("delivery_home") { DeliveryScreen(navController) }
                    composable("profile") { ProfileScreen(navController) }
                    composable("history") { HistoryScreen(navController) }
                    
                    // Pass the theme toggle function to Settings
                    composable("settings") { 
                        SettingsScreen(navController) 
                        // Note: To keep it simple for MVP, SettingsScreen saves to Prefs.
                        // Ideally we pass a callback, but for now we rely on the restart/recomposition logic.
                    }
                }
            }
        }
    }
}

// 3. Define the Super App Dashboard HERE (So it's not missing)
@Composable
fun SuperAppHome(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("Activity") },
                    selected = false,
                    onClick = { navController.navigate("history") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Account") },
                    selected = false,
                    onClick = { navController.navigate("profile") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $userName!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Default.Settings, null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Services", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            // SERVICE GRID
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // RIDE CARD
                ServiceCard(
                    title = "Ride",
                    icon = Icons.Default.LocalTaxi,
                    color = Color(0xFFE3F2FD), // Light Blue
                    iconColor = Color(0xFF1E88E5),
                    onClick = { navController.navigate("ride_home") }
                )
                
                // DELIVERY CARD
                ServiceCard(
                    title = "Delivery",
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0xFFFFF3E0), // Light Orange
                    iconColor = Color(0xFFE65100),
                    onClick = { navController.navigate("delivery_home") }
                )
            }
        }
    }
}

@Composable
fun ServiceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, iconColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(160.dp).height(120.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
