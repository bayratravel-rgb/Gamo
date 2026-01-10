package com.bayera.travel.customer

import android.content.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val opt = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel").build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, opt)
        } catch (e: Exception) {}
        setContent {
            val nav = rememberNavController()
            val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dash"
            MaterialTheme {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { LoginUI(nav) }
                    composable("dash") { DashboardUI(nav) }
                }
            }
        }
    }
}

@Composable
fun LoginUI(nav: NavController) {
    val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    var name by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome to Bayera", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Enter your details to start", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White))
        Button(onClick = { if(name.isNotEmpty()) { prefs.edit().putString("name", name).apply(); nav.navigate("dash") } }, modifier = Modifier.fillMaxWidth().padding(top=32.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp)) {
            Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "bb")
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Bayera Travel", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("Hi, $userName!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { prefs.edit().clear().apply(); nav.navigate("login") }) { Icon(Icons.Default.Settings, null) }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Services", fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(modifier = Modifier.fillMaxWidth().padding(top=16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(40.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
