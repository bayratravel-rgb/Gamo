package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
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
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dashboard"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("dashboard") { DashboardScreen(navController) }
            }
        }
    }
}

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    val carInfo = "${prefs.getString("car_model", "")} (${prefs.getString("license_plate", "")})"
    
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var declinedTripIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                for (child in snapshot.children) {
                    try {
                        val trip = child.getValue(Trip::class.java)
                        // STRICT FILTER: Only show REQUESTED trips
                        if (trip != null && trip.status == TripStatus.REQUESTED) {
                            trips.add(trip)
                        }
                    } catch (e: Exception) {}
                }
                activeTrips = trips.reversed() // Newest first
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val visibleTrips = activeTrips.filter { !declinedTripIds.contains(it.tripId) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hi, $driverName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { 
                        prefs.edit().clear().apply()
                        navController.navigate("login") { popUpTo(0) }
                    }) { Text("Logout", color = Color.Red) }
                }
                
                Text("Available Rides (${visibleTrips.size})", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                if (visibleTrips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Searching for rides...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        items(visibleTrips) { trip ->
                            TripCard(
                                trip = trip, 
                                driverDetails = "$driverName - $carInfo",
                                onDecline = { declinedTripIds = declinedTripIds + trip.tripId }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip, driverDetails: String, onDecline: () -> Unit) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👤 ${trip.customerId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("📍 From: ${trip.pickupLocation.address}")
            Text("🏁 To: ${trip.dropoffLocation.address}")
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💰 ${trip.price} ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                Text("⏱️ ${trip.estimatedTime} min")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) { Text("DECLINE") }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(
                    onClick = { 
                        // --- TRANSACTION LOGIC (Prevents Conflict) ---
                        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                        
                        tripRef.runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                val currentTrip = mutableData.getValue(Trip::class.java)
                                if (currentTrip == null) {
                                    return Transaction.success(mutableData)
                                }
                                
                                // Only accept if it's STILL 'REQUESTED'
                                if (currentTrip.status == TripStatus.REQUESTED) {
                                    currentTrip.status = TripStatus.ACCEPTED
                                    currentTrip.driverId = driverDetails // Assign ME
                                    mutableData.value = currentTrip
                                    return Transaction.success(mutableData)
                                } else {
                                    // Too late! Someone else took it.
                                    return Transaction.abort()
                                }
                            }

                            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                                if (committed) {
                                    Toast.makeText(context, "Ride Secured! Go pick them up.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Too late! Ride already taken.", Toast.LENGTH_LONG).show()
                                }
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) { Text("ACCEPT") }
            }
        }
    }
}
