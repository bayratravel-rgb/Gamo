package com.bayera.travel.driver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
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
    
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var currentAcceptedTrip by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                var myTrip: Trip? = null
                
                for (child in snapshot.children) {
                    try {
                        val trip = child.getValue(Trip::class.java)
                        if (trip != null) {
                            // If I accepted this trip, show it at the top
                            if (trip.driverId != null && trip.driverId!!.contains(driverName)) {
                                myTrip = trip
                            }
                            // Show available trips
                            if (trip.status == TripStatus.REQUESTED) {
                                trips.add(trip)
                            }
                        }
                    } catch (e: Exception) {}
                }
                activeTrips = trips.reversed()
                currentAcceptedTrip = myTrip
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // HEADER
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hi, $driverName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { 
                        prefs.edit().clear().apply()
                        navController.navigate("login") { popUpTo(0) }
                    }) { Text("Logout", color = Color.Red) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CURRENT JOB (If accepted)
                if (currentAcceptedTrip != null) {
                    Text("🟢 CURRENT JOB", style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    CurrentJobCard(currentAcceptedTrip!!)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // INCOMING LIST
                Text("Incoming Requests", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (activeTrips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Searching...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        items(activeTrips) { trip ->
                            TripCard(trip, "$driverName")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentJobCard(trip: Trip) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), // Light Green Highlight
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Picking up: ${trip.customerId}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("📍 ${trip.pickupLocation.address}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // NAVIGATION BUTTON
            Button(
                onClick = { 
                    // Open Google Maps Navigation
                    val uri = "google.navigation:q=${trip.pickupLocation.lat},${trip.pickupLocation.lng}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.setPackage("com.google.android.apps.maps")
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Google Maps not found", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NAVIGATE TO CUSTOMER")
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip, driverId: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👤 ${trip.customerId}", fontWeight = FontWeight.Bold)
            Text("📍 ${trip.pickupLocation.address}")
            Text("💰 ${trip.price} ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { 
                    val db = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                    db.updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACCEPT RIDE")
            }
        }
    }
}
