package com.bayera.travel.driver

// ... (Imports) ...
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.TripStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_dashboard"
            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("super_dashboard") { DriverSuperDashboard(navController) }
                composable("wallet") { WalletScreen(navController) }
            }
        }
    }
}

// ... (Dashboard & Lists code omitted for brevity, assuming standard) ...
@Composable
fun DriverSuperDashboard(navController: NavController) { RideRequestsScreen(navController) } 
// Need to pass NavController or setup screens properly. Assuming simplified for script.

@Composable
fun RideRequestsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Partner") ?: "Partner"
    // Clean Phone ID for Database
    val driverId = prefs.getString("phone", "000")?.filter { it.isDigit() } ?: "000"

    var currentJob by remember { mutableStateOf<Trip?>(null) }
    
    LaunchedEffect(Unit) {
        // Listener Logic...
    }
    
    // ... (Display Logic) ...
    if (currentJob != null) {
        ActiveJobCard(currentJob!!, driverId)
    }
}

@Composable
fun ActiveJobCard(trip: Trip, driverId: String) {
    val context = LocalContext.current
    val db = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
    
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT TRIP", fontWeight = FontWeight.Bold)
            Text("From: ${trip.pickupLocation.address}")
            Text("To: ${trip.dropoffLocation.address}")
            
            if (trip.paymentStatus == "PAID_WALLET") {
                Text("PAID VIA WALLET", color = Color.Green, fontWeight = FontWeight.Bold)
            } else {
                Text("Collect Cash: ${trip.price}", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trip.status == TripStatus.IN_PROGRESS) {
                // --- SEPARATE BUTTONS ---
                if (trip.paymentStatus == "PAID_WALLET") {
                    Button(onClick = { 
                        updateBalance(driverId, trip.price, isCash = false, context)
                        db.child("status").setValue(TripStatus.COMPLETED)
                    }) { Text("COMPLETE TRIP") }
                } else {
                    Button(onClick = { 
                        updateBalance(driverId, trip.price, isCash = true, context)
                        db.child("status").setValue(TripStatus.COMPLETED)
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { 
                        Text("CASH COLLECTED") 
                    }
                }
            }
        }
    }
}

fun updateBalance(driverId: String, amount: Double, isCash: Boolean, context: Context) {
    val driverWalletRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId).child("balance")
    
    driverWalletRef.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val currentBalance = currentData.getValue(Double::class.java) ?: 0.0
            val newBalance = if (isCash) currentBalance - (amount * 0.10) else currentBalance + (amount * 0.90)
            currentData.value = newBalance
            return Transaction.success(currentData)
        }
        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {
            if (e != null) {
                Toast.makeText(context, "Balance Error: ${e.message}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Balance Updated Successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    })
}
