package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual Firebase Init (Ensures it works in Termux)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val opt = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:656f004fce2ecda85af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel").build()
                FirebaseApp.initializeApp(this, opt)
            }
        } catch (e: Exception) {}

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dashboard"

            MaterialTheme {
                NavHost(navController = navController, startDestination = startScreen) {
                    composable("login") { DriverLoginScreen(navController) }
                    composable("dashboard") { DriverDashboardScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun DriverDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "fg") ?: "fg"
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Rides, 1 for Earnings

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Rides") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) },
                    label = { Text("Earnings") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { padding ->
        // SETTING THE LIGHT GREEN BACKGROUND FROM YOUR SCREENSHOT
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFE8F5E9)) 
                .padding(16.dp)
        ) {
            // TOP BAR: Name and Logout
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Hi, $driverName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    prefs.edit().clear().apply()
                    navController.navigate("login") { popUpTo(0) }
                }) {
                    Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                RidesTabContent()
            } else {
                EarningsTabContent()
            }
        }
    }
}

@Composable
fun RidesTabContent() {
    Column {
        Text(
            "Incoming Rides", 
            style = MaterialTheme.typography.headlineMedium, 
            color = Color(0xFF2E7D32), 
            fontWeight = FontWeight.Bold
        )
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // MATCHING THE "Searching..." STATE FROM YOUR SCREENSHOT
            Text("Searching...", color = Color.Gray, fontSize = 18.sp)
        }
    }
}

@Composable
fun EarningsTabContent() {
    Column {
        Text("My Earnings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Earnings", color = Color.White.copy(alpha = 0.8f))
                Text("500.0 ETB", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DriverLoginScreen(navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Partner Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Enter Your Name") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (name.isNotEmpty()) {
                context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE).edit().putString("name", name).apply()
                navController.navigate("dashboard")
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Go Online") }
    }
}
