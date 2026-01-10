package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val opt = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:656f004fce2ecda85af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel").build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, opt)
        } catch (e: Exception) {}
        setContent {
            val nav = rememberNavController()
            val prefs = LocalContext.current.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
            MaterialTheme {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { DriverLoginUI(nav) }
                    composable("dash") { DriverDashboardUI(nav) }
                }
            }
        }
    }
}

@Composable
fun DriverLoginUI(nav: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Partner Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Button(onClick = { if(name.isNotEmpty() && email.contains("@")) {
            context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE).edit().putString("name", name).putString("email", email).apply()
            nav.navigate("dash")
        }}, modifier = Modifier.fillMaxWidth().padding(top=32.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("Go Online") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardUI(nav: NavController) {
    val prefs = LocalContext.current.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val email = prefs.getString("email", "driver@test,com") ?: ""
    val sanitized = email.replace(".", ",")
    var balance by remember { mutableStateOf(0.0) }
    
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("drivers").child(sanitized).child("balance")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 500.0 }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    Scaffold(bottomBar = { NavigationBar(containerColor = Color.White) { 
        NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Rides") }, selected = true, onClick = {})
        NavigationBarItem(icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Earnings") }, selected = false, onClick = {})
    }}) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().background(Color(0xFFE8F5E9)).padding(16.dp)) {
            Text("Hi, Partner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth().height(150.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Earnings", color = Color.White.copy(alpha = 0.8f))
                    Text("${String.format("%.1f", balance)} ETB", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
