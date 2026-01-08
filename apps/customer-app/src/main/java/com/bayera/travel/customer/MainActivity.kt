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
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        val osmPrefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, osmPrefs)
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            MaterialTheme {
                val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_home"
                NavHost(navController = navController, startDestination = startScreen) {
                    composable("login") { LoginScreen(navController) }
                    composable("super_home") { SuperAppHome(navController) }
                    // Placeholders for other screens to prevent navigation crashes
                    composable("ride_home") { Text("Ride Screen") }
                    composable("delivery_home") { Text("Shopping Screen") }
                    composable("wallet") { Text("Wallet Screen") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAppHome(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val balance = 0.0f

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Hi, $userName!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                onClick = { navController.navigate("wallet") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5))
            ) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Wallet Balance", color = Color.White.copy(alpha = 0.8f))
                        Text("$balance ETB", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Services", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ServiceCard("Ride", Icons.Default.LocalTaxi, Color(0xFFE3F2FD), Modifier.weight(1f)) { navController.navigate("ride_home") }
                ServiceCard("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Modifier.weight(1f)) { navController.navigate("delivery_home") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(120.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(48.dp))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    Text("Login Screen Placeholder")
}
