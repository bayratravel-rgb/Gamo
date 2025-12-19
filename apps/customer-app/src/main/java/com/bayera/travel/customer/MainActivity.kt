package com.bayera.travel.customer

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
import com.bayera.travel.utils.FareCalculator
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        Configuration.getInstance().userAgentValue = packageName
        setContent { AppUI() }
    }
}

@Composable
fun AppUI() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Default Arba Minch
    val startGeo = GeoPoint(6.0206, 37.5557)
    
    // UI State
    var step by remember { mutableIntStateOf(0) } // 0=Pickup, 1=Dropoff, 2=Review, 3=WAITING
    
    var currentGeoPoint by remember { mutableStateOf(startGeo) }
    var pickupGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var dropoffGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var pickupAddr by remember { mutableStateOf("") }
    var dropoffAddr by remember { mutableStateOf("") }
    var estimatedPrice by remember { mutableStateOf(0.0) }
    var addressText by remember { mutableStateOf("Locating...") }
    var isMapMoving by remember { mutableStateOf(false) }
    
    // Active Trip Data
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    
    // Map References
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    var mapViewRef: MapView? by remember { mutableStateOf(null) }

    // --- LISTENER FOR TRIP UPDATES (The Handshake) ---
    LaunchedEffect(activeTrip?.tripId) {
        if (activeTrip != null) {
            val db = FirebaseDatabase.getInstance().getReference("trips").child(activeTrip!!.tripId)
            db.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedTrip = snapshot.getValue(Trip::class.java)
                    if (updatedTrip != null) activeTrip = updatedTrip
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
                    isTilesScaledToDpi = true
                    minZoomLevel = 6.0
                    maxZoomLevel = 22.0
                    controller.setZoom(16.0)
                    controller.setCenter(startGeo)
                    mapController = controller
                    mapViewRef = this

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { isMapMoving = true; return true }
                        override fun onZoom(event: ZoomEvent?): Boolean { isMapMoving = true; return true }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) currentGeoPoint = mapView.mapCenter as GeoPoint
                
                // DRAW OVERLAYS
                mapView.overlays.clear()

                if (step >= 1 || step == 3) {
                    val m1 = Marker(mapView)
                    m1.position = pickupGeo
                    m1.title = "Pickup"
                    m1.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default) // Standard Pin
                    mapView.overlays.add(m1)
                }

                if (step >= 2 || step == 3) {
                    val m2 = Marker(mapView)
                    m2.position = dropoffGeo
                    m2.title = "Dropoff"
                    mapView.overlays.add(m2)
                    
                    // Simple Line (Straight for now, Road Routing requires API)
                    val line = Polyline()
                    line.addPoint(pickupGeo)
                    line.addPoint(dropoffGeo)
                    line.color = android.graphics.Color.BLUE
                    line.width = 12f
                    mapView.overlays.add(line)
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Address Fetcher
        LaunchedEffect(isMapMoving) {
            if (isMapMoving && step < 2) { // Only fetch address during selection
                kotlinx.coroutines.delay(600)
                isMapMoving = false 
                scope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(currentGeoPoint.latitude, currentGeoPoint.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val line = addresses[0].getAddressLine(0)
                            val shortAddr = line.split(",").take(2).joinToString(",")
                            withContext(Dispatchers.Main) { addressText = shortAddr }
                        }
                    } catch (e: Exception) {}
                }
            }
        }

        // --- SELECTION PIN (Only steps 0 and 1) ---
        if (step < 2) {
            Icon(
                imageVector = if (step == 0) Icons.Default.Home else Icons.Default.LocationOn,
                contentDescription = "Pin",
                modifier = Modifier.size(40.dp).align(Alignment.Center).offset(y = (-20).dp), // Smaller Icon
                tint = if (step == 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        }

        // --- BOTTOM SHEET UI ---
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(24.dp)
        ) {
            if (step == 3) {
                // --- STATE 3: WAITING FOR DRIVER ---
                if (activeTrip?.status == TripStatus.ACCEPTED) {
                    Text("✅ Driver Found!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Driver: ${activeTrip?.driverId}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your ride is on the way.", color = Color.Gray)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Finding you a driver...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Please wait.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            // Cancel Logic
                            step = 0 
                            activeTrip = null
                            pickupGeo = null
                            dropoffGeo = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel Request", color = Color.Black) }
                }
                
            } else if (step == 0) {
                // PICKUP
                Text("Confirm Pickup Point", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        pickupGeo = currentGeoPoint
                        pickupAddr = addressText
                        step = 1 
                        Toast.makeText(context, "Now drag to destination", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Set Pickup") }
                
            } else if (step == 1) {
                // DROPOFF
                Text("Where to?", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        dropoffGeo = currentGeoPoint
                        dropoffAddr = addressText
                        val dist = FareCalculator.calculateDistance(pickupGeo!!.latitude, pickupGeo!!.longitude, dropoffGeo!!.latitude, dropoffGeo!!.longitude)
                        estimatedPrice = FareCalculator.calculatePrice(dist)
                        step = 2 
                        
                        // Zoom out to show whole route
                        mapController?.setZoom(13.0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Set Destination") }

            } else if (step == 2) {
                // REVIEW
                Text("Total Fare: $estimatedPrice ETB", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Text("From: $pickupAddr", style = MaterialTheme.typography.bodySmall)
                Text("To: $dropoffAddr", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        val db = FirebaseDatabase.getInstance().getReference("trips")
                        val newId = UUID.randomUUID().toString()
                        val trip = Trip(
                            tripId = newId, customerId = "Yabu",
                            pickupLocation = Location(pickupGeo!!.latitude, pickupGeo!!.longitude, pickupAddr),
                            dropoffLocation = Location(dropoffGeo!!.latitude, dropoffGeo!!.longitude, dropoffAddr),
                            price = estimatedPrice, status = TripStatus.REQUESTED
                        )
                        db.child(newId).setValue(trip)
                        activeTrip = trip
                        step = 3 // ENTER WAITING MODE
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
