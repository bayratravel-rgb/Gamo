package com.bayera.travel.customer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.config.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val context = LocalContext.current
            
            // State to track the current active trip
            var currentTrip by remember { mutableStateOf<Trip?>(null) }

            // Listener: If we have a Trip ID, watch it for changes (like Driver Accepting)
            LaunchedEffect(currentTrip?.tripId) {
                if (currentTrip != null) {
                    val db = FirebaseDatabase.getInstance().getReference("trips").child(currentTrip!!.tripId)
                    db.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val updatedTrip = snapshot.getValue(Trip::class.java)
                            if (updatedTrip != null) {
                                currentTrip = updatedTrip
                            }
                        }
                        override fun onCancelled(e: DatabaseError) {}
                    })
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // MAP (Background)
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            controller.setCenter(GeoPoint(6.0206, 37.5557))
                            val m = Marker(this)
                            m.position = GeoPoint(6.0206, 37.5557)
                            m.title = "Pickup"
                            overlays.add(m)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // UI OVERLAY (Changes based on Status)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    if (currentTrip == null) {
                        // STATE 1: No Trip yet
                        Text("Arba Minch Rides", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                val newId = UUID.randomUUID().toString()
                                val newTrip = Trip(
                                    tripId = newId,
                                    customerId = "CUST-001",
                                    pickupLocation = Location(6.0206, 37.5557, "Arba Minch Airport"),
                                    dropoffLocation = Location(6.03, 37.56, "University"),
                                    price = 150.0,
                                    estimatedTime = 15,
                                    status = TripStatus.REQUESTED
                                )
                                FirebaseDatabase.getInstance().getReference("trips").child(newId).setValue(newTrip)
                                currentTrip = newTrip // Set local state immediately
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Pickup")
                        }
                    } else if (currentTrip!!.status == TripStatus.REQUESTED) {
                        // STATE 2: Waiting for Driver
                        CircularProgressIndicator(color = Color(0xFF1E88E5))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Finding you a driver...", style = MaterialTheme.typography.bodyLarge)
                    
                    } else if (currentTrip!!.status == TripStatus.ACCEPTED) {
                        // STATE 3: DRIVER FOUND!
                        Text("✅ Driver Found!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Driver ID: ${currentTrip!!.driverId}", style = MaterialTheme.typography.bodyMedium)
                        Text("Arriving in ${currentTrip!!.estimatedTime} mins", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
