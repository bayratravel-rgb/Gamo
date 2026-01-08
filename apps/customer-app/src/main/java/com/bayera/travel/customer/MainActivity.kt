package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            val navController = rememberNavController()
            MaterialTheme {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") { CustomerDashboard(navController) }
                    composable("ride") { Text("Ride Map Loading...") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(onClick = { navController.navigate("ride") }, modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2)); Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00)); Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
