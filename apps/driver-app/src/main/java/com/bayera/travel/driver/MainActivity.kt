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

            LaunchedEffect(Unit) {
                val database = FirebaseDatabase.getInstance()
                val tripsRef = database.getReference("trips")

                tripsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val trips = mutableListOf<Trip>()
                        for (child in snapshot.children) {
                            try {
                                val trip = child.getValue(Trip::class.java)
                                // --- DEBUG: REMOVED STATUS FILTER ---
                                // Showing ALL trips to verify connection
                                if (trip != null) {
                                    trips.add(trip)
                                }
                            } catch (e: Exception) {
                                // If parsing fails, ignore
                            }
                        }
                        // Sort by newest first (reverse)
                        activeTrips = trips.reversed()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "DB Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Incoming Requests", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeTrips.isEmpty()) {
                            Text("Waiting for requests...", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            LazyColumn {
                                items(activeTrips) { trip ->
                                    TripCard(trip)
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
fun TripCard(trip: Trip) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👤 ${trip.customerId}", fontWeight = FontWeight.Bold)
            Text("📍 ${trip.pickupLocation.address}")
            
            // Show Status for Debugging
            Text("Status: ${trip.status}", color = Color.Gray, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { 
                    val db = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                    db.child("status").setValue(TripStatus.ACCEPTED)
                    Toast.makeText(context, "Accepted!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACCEPT RIDE")
            }
        }
    }
}
