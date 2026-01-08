package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🛡️ CORRECT MANUAL INITIALIZATION
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {}

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    CustomerDashboardUI()
                }
            }
        }
    }
}

@Composable
fun CustomerDashboardUI() {
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
        Column(modifier = Modifier.padding(p).fillMaxSize().padding(20.dp)) {
            Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
            Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
            Spacer(modifier = Modifier.height(30.dp))
            Text("Services", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                ServiceCardUI("Ride", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                ServiceCardUI("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCardUI(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, modifier: Modifier) {
    Card(modifier = modifier.height(130.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(35.dp), tint = Color(0xFF1976D2))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}
