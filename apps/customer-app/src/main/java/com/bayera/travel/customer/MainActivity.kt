package com.bayera.travel.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import com.google.firebase.database.FirebaseDatabase
import com.bayera.travel.common.payment.ChapaManager
import org.osmdroid.config.Configuration
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            FirebaseApp.initializeApp(this)
            val osmPrefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
            Configuration.getInstance().load(this, osmPrefs)
            Configuration.getInstance().userAgentValue = packageName
        } catch (e: Exception) {}

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = prefs.getBoolean("dark_mode", isSystemDark)
            val colors = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colors) {
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
}

@Composable
fun PayTripScreen(navController: NavController, tripId: String, amount: Double) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    
    // PERSISTENT STATE FOR DIALOG
    var showConfirm by remember { mutableStateOf(false) }
    var currentTxRef by remember { mutableStateOf("") }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { /* Force user to choose */ },
            title = { Text("Complete Payment") },
            text = { Text("After completing payment in the browser, click 'Yes' to confirm.") },
            confirmButton = {
                TextButton(onClick = {
                    isLoading = true
                    // Verify with Server
                    ChapaManager.verifyPayment(currentTxRef) { success ->
                         android.os.Handler(android.os.Looper.getMainLooper()).post {
                             isLoading = false
                             if (success) {
                                 FirebaseDatabase.getInstance().getReference("trips").child(tripId).child("paymentStatus").setValue("PAID_WALLET")
                                 Toast.makeText(context, "Payment Verified! ✅", Toast.LENGTH_SHORT).show()
                                 showConfirm = false
                                 navController.popBackStack()
                             } else {
                                 Toast.makeText(context, "Payment Not Found. Try Again.", Toast.LENGTH_LONG).show()
                                 // Keep dialog open or close it? Let's keep it open so they can retry or cancel
                                 // Actually, better to close and let them try paying again
                                 showConfirm = false 
                             }
                         }
                    }
                }) { Text("Yes, I Paid") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
             CircularProgressIndicator()
             Spacer(modifier = Modifier.height(16.dp))
             Text("Verifying...")
        } else {
            Icon(Icons.Default.Payment, null, modifier = Modifier.size(64.dp), tint = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Pay for Ride", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("$amount ETB", style = MaterialTheme.typography.displayMedium, color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isLoading = true
                    val txRef = "TRIP-${UUID.randomUUID().toString().take(8)}"
                    currentTxRef = txRef
                    
                    ChapaManager.initializePayment("Yeabkalkassahun21@gmail.com", amount, "User", "Customer", txRef) { url, error ->
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            isLoading = false
                            if (url != null) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                                showConfirm = true // Show dialog ONLY after browser opens
                            } else {
                                Toast.makeText(context, "Failed: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Pay Now")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Pay Cash to Driver") }
        }
    }
}

// ... (SuperAppHome and ServiceCard code - appending to ensure file completeness)
@Composable
fun SuperAppHome(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val balance = prefs.getFloat("wallet_balance", 0.0f)

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
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().height(80.dp).clickable { navController.navigate("wallet") }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5)), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Wallet Balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        Text("$balance ETB", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { navController.navigate("wallet") }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) { Text("Top Up", color = Color(0xFF1E88E5)) }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
