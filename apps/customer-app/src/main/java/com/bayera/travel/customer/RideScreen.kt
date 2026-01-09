package com.bayera.travel.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.util.UUID

@Composable
fun RideScreen(navController: NavController) {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var mode by remember { mutableStateOf("PICKUP") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val mapState = remember { mutableStateOf<MapView?>(null) }
    
    // Coordinates for the Blue Line
    val routeOverlay = remember { Polyline() }

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == "user_bb" && it.status != TripStatus.COMPLETED }
                
                // If trip is accepted/in-progress, draw the line
                activeTrip?.let {
                    val start = GeoPoint(it.pickupLocation.lat, it.pickupLocation.lng)
                    val end = GeoPoint(it.dropoffLocation.lat, it.dropoffLocation.lng)
                    routeOverlay.setPoints(listOf(start, end)) // Simple line for now
                    routeOverlay.color = android.graphics.Color.BLUE
                    routeOverlay.width = 10f
                    mapState.value?.overlays?.add(routeOverlay)
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val googleSat = XYTileSource("GoogleSat", 1, 20, 256, ".png", arrayOf("https://mt0.google.com/vt/lyrs=y&x="))
        
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(googleSat)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(6.02, 37.55))
                mapState.value = this
            }
        }, modifier = Modifier.fillMaxSize())

        if (activeTrip == null) {
            // PICKUP SELECTION UI
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = Color(0xFF2E7D32))
            Button(
                onClick = {
                    val id = UUID.randomUUID().toString()
                    db.child(id).setValue(Trip(tripId = id, customerPhone = "user_bb", status = TripStatus.REQUESTED, price = 110.0))
                },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(28.dp)
            ) { Text("Set Pickup Here") }
        } else {
            // THE ESSENTIAL LOCK SCREEN
            val isEnRoute = activeTrip!!.status == TripStatus.IN_PROGRESS
            Box(modifier = Modifier.fillMaxSize().background(if(isEnRoute) Color.Red.copy(alpha = 0.1f) else Color.Transparent)) {
                Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if(isEnRoute) "EN ROUTE 🚕" else "FINDING DRIVER", color = if(isEnRoute) Color.Red else Color.Black, fontWeight = FontWeight.Black)
                        Text("Destination: Arba Minch University")
                        Text("Fare: 110.0 ETB", fontWeight = FontWeight.Bold)
                        if(!isEnRoute) {
                            TextButton(onClick = { db.child(activeTrip!!.tripId).removeValue() }) { Text("Cancel", color = Color.Gray) }
                        } else {
                            Text("Navigation UI Locked", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
