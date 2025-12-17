package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            
            // Auto-login check
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dashboard"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("dashboard") { DashboardScreen() }
            }
        }
    }
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    
    // RETRIEVE SAVED DRIVER DATA
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    val carInfo = "${prefs.getString("car_model", "")} (${prefs.getString("license_plate", "")})"
    
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                for (child in snapshot.children) {
                    try {
                        val trip = child.getValue(Trip::class.java)
                        if (trip != null && trip.status == TripStatus.REQUESTED) {
                            trips.add(trip)
                        }
                    } catch (e: Exception) {}
                }
                activeTrips = trips
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Hi, $driverName", style = MaterialTheme.typography.titleMedium)
                Text("Incoming Requests", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))

                if (activeTrips.isEmpty()) {
                    Text("No requests nearby...", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn {
                        items(activeTrips) { trip ->
                            TripCard(trip, "$driverName - $carInfo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip, driverDetails: String) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👤 ${trip.customerId}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("📍 ${trip.pickupLocation.address}")
            Text("💰 ${trip.price} ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { 
                    val db = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                    // SEND REAL DRIVER DETAILS
                    db.updateChildren(mapOf(
                        "status" to "ACCEPTED",
                        "driverId" to driverDetails
                    ))
                    Toast.makeText(context, "Trip Accepted!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACCEPT RIDE")
            }
        }
    }
}
