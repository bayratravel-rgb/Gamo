package com.bayera.travel.customer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.google.firebase.database.FirebaseDatabase
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
            
            Box(modifier = Modifier.fillMaxSize()) {
                // MAP
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            val startPoint = GeoPoint(6.0206, 37.5557)
                            controller.setCenter(startPoint)
                            
                            val startMarker = Marker(this)
                            startMarker.position = startPoint
                            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            startMarker.title = "Pickup Here"
                            overlays.add(startMarker)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // UI OVERLAY
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Arba Minch Rides", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            Toast.makeText(context, "Requesting Ride...", Toast.LENGTH_SHORT).show()

                            // --- REAL DATA LOGIC ---
                            val database = FirebaseDatabase.getInstance()
                            val tripsRef = database.getReference("trips")
                            
                            val newTripId = UUID.randomUUID().toString()
                            
                            // Create the Trip Object
                            val newTrip = Trip(
                                tripId = newTripId,
                                customerId = "CUST-001",
                                pickupLocation = Location(6.0206, 37.5557, "Arba Minch Airport"),
                                dropoffLocation = Location(6.03, 37.56, "University"),
                                price = 150.0,
                                estimatedTime = 15,
                                status = TripStatus.REQUESTED
                            )
                            
                            // Send to Cloud
                            tripsRef.child(newTripId).setValue(newTrip)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Driver Requested! 🚕", Toast.LENGTH_LONG).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Pickup")
                    }
                }
            }
        }
    }
}
