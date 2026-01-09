package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.*
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val opt = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:656f004fce2ecda85af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel").build()
                FirebaseApp.initializeApp(this, opt)
            }
        } catch (e: Exception) {}

        setContent {
            val nav = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dash"

            MaterialTheme {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { DriverLogin(nav) }
                    composable("dash") { DriverDash(nav) }
                }
            }
        }
    }
}

@Composable
fun DriverLogin(nav: androidx.navigation.NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Partner Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Driver Name") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (name.isNotEmpty()) {
                context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE).edit().putString("name", name).apply()
                nav.navigate("dash") { popUpTo("login") { inclusive = true } }
            }
        }, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("Go Online") }
    }
}

@Composable
fun DriverDash(nav: androidx.navigation.NavController) {
    val name = LocalContext.current.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE).getString("name", "Partner")
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bayera Partner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Welcome, $name")
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("500.0 ETB", color = Color.White, style = MaterialTheme.typography.displayMedium)
            }
        }
    }
}
