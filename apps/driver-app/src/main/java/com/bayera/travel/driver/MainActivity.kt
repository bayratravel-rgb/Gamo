package com.bayera.travel.driver

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
            val context = LocalContext.current
            var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
            val myDriverId = "DRV-KEBEDE-001"

            LaunchedEffect(Unit) {
                val database = FirebaseDatabase.getInstance()
                val tripsRef = database.getReference("trips")

                tripsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val trips = mutableListOf<Trip>()
                        for (child in snapshot.children) {
                            try {
                                val trip = child.getValue(Trip::class.java)
                                // Only show REQUESTED trips
                                if (trip != null && trip.status == TripStatus.REQUESTED) {
                                    trips.add(trip)
                                }
                            } catch (e: Exception) {
                                // Ignore bad data
                            }
                        }
                        activeTrips = trips
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚖 Driver Dashboard", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeTrips.isEmpty()) {
                            Text("Waiting for requests...", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            LazyColumn {
                                items(activeTrips) { trip ->
                                    TripCard(trip, myDriverId)
                                }
                            }
                        }
                    }
                }
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
            Text("📍 Pickup: ${trip.pickupLocation.address}", fontWeight = FontWeight.Bold)
            Text("💰 Fare: ${trip.price} ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { 
                    // 1. Show Toast immediately to prove button works
                    Toast.makeText(context, "Accepting Ride...", Toast.LENGTH_SHORT).show()

                    // 2. Database Update Logic
                    val database = FirebaseDatabase.getInstance()
                    val tripRef = database.getReference("trips").child(trip.tripId)
                    
                    // We update the fields manually to be 100% safe
                    val updates = hashMapOf<String, Any>(
                        "status" to "ACCEPTED",
                        "driverId" to driverId
                    )
                    
                    tripRef.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "✅ Success! Client Notified.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            // If this shows, we have a permission/internet issue
                            Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACCEPT RIDE")
            }
        }
    }
}
