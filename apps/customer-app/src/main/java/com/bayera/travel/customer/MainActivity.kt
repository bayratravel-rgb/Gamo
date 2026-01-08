package com.bayera.travel.customer

import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 🛡️ MANUAL FIREBASE INITIALIZATION (TERMUX STABILITY) ---
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel")
                .build()
            FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {
            // Already initialized
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
                    CustomerSuperDashboard()
                }
            }
        }
    }
}

@Composable
fun CustomerSuperDashboard() {
    var screen by remember { mutableStateOf("home") }
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = screen == "home", onClick = { screen = "home" })
                NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            if (screen == "home") {
                DashboardContent { screen = "map" }
            } else {
                MapPlaceholder { screen = "home" }
            }
        }
    }
}

@Composable
fun DashboardContent(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Services", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            ServiceCard("Ride", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Modifier.weight(1f), onRideClick)
            Spacer(modifier = Modifier.width(16.dp))
            ServiceCard("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hotel, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Hotels & Resorts", fontWeight = FontWeight.Bold)
                    Text("Book your stay", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, modifier: Modifier, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = modifier.height(140.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = Color(0xFF1976D2))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MapPlaceholder(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Text("📍 Arba Minch Map Engine Active", modifier = Modifier.align(Alignment.Center))
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.ArrowBack, null)
        }
    }
}
