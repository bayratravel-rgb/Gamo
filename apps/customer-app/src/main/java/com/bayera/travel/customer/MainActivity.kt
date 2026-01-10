package com.bayera.travel.customer

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
            val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
            
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { CustomerLogin(nav) }
                    composable("dash") { CustomerDashboard(nav) }
                }
            }
        }
    }
}

@Composable
fun CustomerLogin(nav: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome to Urbana", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalize your mobility", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        Button(onClick = { if(name.isNotEmpty() && email.contains("@")) {
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().putString("name", name).putString("email", email).apply()
            nav.navigate("dash")
        }}, modifier = Modifier.fillMaxWidth().padding(top=32.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
            Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(nav: NavController) {
    val context = LocalContext.current
    val name = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("name", "User")
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Urbana Mobility", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Hi, $name!", color = Color.LightGray, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = Color.White) }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp))
                    Text("Ride", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(40.dp))
                    Text("Hotels", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
