package com.bayera.travel.customer

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
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
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val userPhone = prefs.getString("phone", "") ?: ""

    // STATE: 0=Pickup, 1=Dropoff, 2=Confirm
    var step by remember { mutableIntStateOf(0) }
    
    var pickupGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var dropoffGeo by remember { mutableStateOf<GeoPoint?>(null) }
    
    var pickupAddr by remember { mutableStateOf("") }
    var dropoffAddr by remember { mutableStateOf("") }
    var locationNote by remember { mutableStateOf("") }
    
    var currentCenter by remember { mutableStateOf(GeoPoint(6.0206, 37.5557)) }
    var isMapMoving by remember { mutableStateOf(false) }
    var addressText by remember { mutableStateOf("Locating...") }
    var estimatedPrice by remember { mutableStateOf(0.0) }
    
    // Handshake
    var currentTrip by remember { mutableStateOf<Trip?>(null) }

    // Map Controller
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun zoomToUser() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val userPos = GeoPoint(loc.latitude, loc.longitude)
                    mapController?.animateTo(userPos)
                    mapController?.setZoom(18.5)
                }
            }
        } catch (e: SecurityException) {}
    }

    // Listener for Driver
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
                    isTilesScaledToDpi = true 
                    minZoomLevel = 6.0
                    maxZoomLevel = 22.0
                    controller.setZoom(15.0)
                    controller.setCenter(currentCenter)
                    mapController = controller

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { isMapMoving = true; return true }
                        override fun onZoom(event: ZoomEvent?): Boolean { isMapMoving = true; return true }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) currentCenter = mapView.mapCenter as GeoPoint
            },
            modifier = Modifier.fillMaxSize()
        )

        // Address Fetcher
        LaunchedEffect(isMapMoving) {
            if (isMapMoving) {
                kotlinx.coroutines.delay(600)
                isMapMoving = false 
                scope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(currentCenter.latitude, currentCenter.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val line = addresses[0].getAddressLine(0)
                            val shortAddr = line.split(",").take(2).joinToString(",")
                            withContext(Dispatchers.Main) { addressText = shortAddr }
                        }
                    } catch (e: Exception) { withContext(Dispatchers.Main) { addressText = "Unknown" } }
                }
            }
        }

        // Center Pin (Changes Color)
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin",
            modifier = Modifier.size(50.dp).align(Alignment.Center).offset(y = (-25).dp),
            tint = if (step == 0) Color(0xFF2E7D32) else Color(0xFFD32F2F) // Green for Pickup, Red for Dropoff
        )

        // Bottom Sheet
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(24.dp)
        ) {
            if (currentTrip != null) {
                // TRIP IN PROGRESS
                if (currentTrip!!.status == TripStatus.ACCEPTED) {
                    Text("✅ Driver Found!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Driver: ${currentTrip!!.driverId}", style = MaterialTheme.typography.bodyLarge)
                } else {
                    CircularProgressIndicator(color = Color(0xFF1E88E5))
                    Text("Finding driver...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // BOOKING FLOW
                Text(if (step == 0) "Set Pickup Point" else "Set Destination", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (step == 0) {
                    // STEP 1: CONFIRM PICKUP
                    OutlinedTextField(
                        value = locationNote,
                        onValueChange = { locationNote = it },
                        label = { Text("Note (e.g. Near gate)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            pickupGeo = currentCenter
                            pickupAddr = addressText
                            step = 1 // Move to Dropoff
                            // Zoom out slightly to help find destination
                            mapController?.setZoom(14.0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Green
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Confirm Pickup", color = Color.White) }
                } else {
                    // STEP 2: CONFIRM DROPOFF & BOOK
                    // Calculate Price Preview
                    val dist = FareCalculator.calculateDistance(
                        pickupGeo!!.latitude, pickupGeo!!.longitude,
                        currentCenter.latitude, currentCenter.longitude
                    )
                    val price = FareCalculator.calculatePrice(dist)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Est. Fare", color = Color.Gray)
                        Text("$price ETB", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row {
                        OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f)) { Text("Back") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { 
                                val db = FirebaseDatabase.getInstance().getReference("trips")
                                val newId = UUID.randomUUID().toString()
                                val trip = Trip(
                                    tripId = newId,
                                    customerId = "$userName ($userPhone)",
                                    pickupLocation = Location(pickupGeo!!.latitude, pickupGeo!!.longitude, pickupAddr),
                                    dropoffLocation = Location(currentCenter.latitude, currentCenter.longitude, addressText),
                                    price = price,
                                    status = TripStatus.REQUESTED,
                                    pickupNotes = locationNote
                                )
                                db.child(newId).setValue(trip)
                                currentTrip = trip
                                step = 0 // Reset UI state
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Book Ride") }
                    }
                }
            }
        }
    }
}
