package com.bayera.travel.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler // FIXED: Import Added
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.bayera.travel.common.payment.ChapaManager
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_home"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("super_home") { SuperAppHome(navController) }
                composable("ride_home") { RideScreen(navController) }
                composable("delivery_home") { ShoppingScreen(navController) }
                composable("hotel_home") { HotelScreen(navController) }
                composable("profile") { ProfileScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
                composable("history") { HistoryScreen(navController) }
                composable("wallet") { WalletScreen(navController) }
                
                composable("pay_trip/{tripId}/{amount}") { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
                    val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                    PayTripScreen(navController, tripId, amount)
                }
            }
        }
    }
}

@Composable
fun PayTripScreen(navController: NavController, tripId: String, amount: Double) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Payment, null, modifier = Modifier.size(64.dp), tint = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Pay for Ride", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("$amount ETB", style = MaterialTheme.typography.displayMedium, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isLoading = true
                val txRef = "TRIP-${UUID.randomUUID().toString().take(8)}"
                
                ChapaManager.initializePayment("customer@bayera.com", amount, "User", "Bayera", txRef) { url, error ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        isLoading = false
                        if (url != null) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                            
                            // MARK PAID
                            FirebaseDatabase.getInstance().getReference("trips").child(tripId)
                                .child("paymentStatus").setValue("PAID_WALLET")
                            
                            Toast.makeText(context, "Payment Processing...", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Pay Now")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }) { Text("Pay Cash to Driver") }
    }
}

@Composable
fun SuperAppHome(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"

    // Lock Back Button
    BackHandler(enabled = true) { /* Do Nothing */ }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = { navController.navigate("history") })
                NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = { navController.navigate("profile") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $userName!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Default.Settings, null) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Services", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ServiceCard("Ride", Icons.Default.LocalTaxi, Color(0xFFE3F2FD), Color(0xFF1E88E5)) { navController.navigate("ride_home") }
                ServiceCard("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFE65100)) { navController.navigate("delivery_home") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().height(100.dp).clickable { navController.navigate("hotel_home") }, colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Hotels & Resorts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                        Text("Book your stay", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, iconColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.width(160.dp).height(120.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
