package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            
            MaterialTheme {
                val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dashboard"
                NavHost(navController = navController, startDestination = startScreen) {
                    composable("login") { Text("Driver Login") }
                    composable("dashboard") { DriverDashboard() }
                }
            }
        }
    }
}

@Composable
fun DriverDashboard() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Partner Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        Text("Waiting for requests...")
    }
}
