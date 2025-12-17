package com.bayera.travel.customer

import android.content.Context
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            // Auto-login check: If name exists, go straight to Home
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "home"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("home") { HomeScreen() }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    // Retrieve Saved User Name
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "Unknown User") ?: "Unknown User"
    
    var currentTrip by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(currentTrip?.tripId) {
        if (currentTrip != null) {
            val db = FirebaseDatabase.getInstance().getReference("trips").child(currentTrip!!.tripId)
            db.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedTrip = snapshot.getValue(Trip::class.java)
                    if (updatedTrip != null) currentTrip = updatedTrip
                }
                override fun onCancelled(e: DatabaseError) {}
            })
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentTrip == null) {
                Text("Hi, $userName 👋", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text("Arba Minch Rides", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        val newId = UUID.randomUUID().toString()
                        val newTrip = Trip(
                            tripId = newId, 
                            // SEND REAL NAME TO DRIVER
                            customerId = "$userName (Ph: ${prefs.getString("phone", "")})",
                            pickupLocation = Location(6.0206, 37.5557, "Arba Minch Airport"),
                            dropoffLocation = Location(6.03, 37.56, "University"),
                            price = 150.0, estimatedTime = 15, status = TripStatus.REQUESTED
                        )
                        FirebaseDatabase.getInstance().getReference("trips").child(newId).setValue(newTrip)
                        currentTrip = newTrip
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm Pickup") }
            } else if (currentTrip!!.status == TripStatus.REQUESTED) {
                CircularProgressIndicator(color = Color(0xFF1E88E5))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Finding a driver for $userName...", style = MaterialTheme.typography.bodyLarge)
            } else if (currentTrip!!.status == TripStatus.ACCEPTED) {
                Text("✅ Driver Found!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Driver: ${currentTrip!!.driverId}", style = MaterialTheme.typography.bodyLarge)
                Text("Arriving in ${currentTrip!!.estimatedTime} mins", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
