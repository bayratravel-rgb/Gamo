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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home // For Pickup
import androidx.compose.material.icons.filled.LocationOn // For Dropoff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
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
import com.google.firebase.database.FirebaseDatabase
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
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    
    // Default: Arba Minch
    val startGeo = GeoPoint(6.0206, 37.5557)
    
    // States: 0=Set Pickup, 1=Set Dropoff, 2=Confirm
    var step by remember { mutableIntStateOf(0) }
    
    var currentCenter by remember { mutableStateOf(startGeo) }
    var addressText by remember { mutableStateOf("Locating...") }
    var isMapMoving by remember { mutableStateOf(false) }

    // Saved Points
    var pickupGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var pickupAddr by remember { mutableStateOf("") }
    var dropoffGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var dropoffAddr by remember { mutableStateOf("") }
    var estimatedPrice by remember { mutableStateOf(0.0) }
    var locationNote by remember { mutableStateOf("") }

    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- BETTER GPS LOGIC ---
    fun zoomToUser() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val userPos = GeoPoint(loc.latitude, loc.longitude)
                    // Only move map if we are in "Selection Mode" (Step 0 or 1)
                    if (step < 2) {
                        mapController?.animateTo(userPos)
                        mapController?.setZoom(18.0)
                    }
                } else {
                    Toast.makeText(context, "GPS Signal weak, please move map manually", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Map View Logic
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 5.0
                    maxZoomLevel = 22.0
                    controller.setZoom(16.0)
                    controller.setCenter(startGeo)
                    mapController = controller

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { isMapMoving = true; return true }
                        override fun onZoom(event: ZoomEvent?): Boolean { isMapMoving = true; return true }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) currentCenter = mapView.mapCenter as GeoPoint
                
                // CLEAR OLD OVERLAYS
                mapView.overlays.clear()

                // DRAW PICKUP PIN (If set)
                if (pickupGeo != null) {
                    val m1 = Marker(mapView)
                    m1.position = pickupGeo
                    m1.title = "Pickup: $pickupAddr"
                    // Green Icon logic here if custom drawable, else default
                    mapView.overlays.add(m1)
                }

                // DRAW DROPOFF PIN (If set)
                if (dropoffGeo != null) {
                    val m2 = Marker(mapView)
                    m2.position = dropoffGeo
                    m2.title = "Dropoff: $dropoffAddr"
                    mapView.overlays.add(m2)
                }

                // DRAW LINE (If both set)
                if (pickupGeo != null && dropoffGeo != null) {
                    val line = Polyline()
                    line.addPoint(pickupGeo)
                    line.addPoint(dropoffGeo)
                    line.color = android.graphics.Color.BLUE
                    line.width = 10f
                    mapView.overlays.add(line)
                }
                
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Address Fetcher (Reverse Geocode)
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
                    } catch (e: Exception) { withContext(Dispatchers.Main) { addressText = "Loading..." } }
                }
            }
        }

        // --- FLOATING CENTER PIN (Only during selection) ---
        if (step < 2) {
            Icon(
                // Step 0 = Home Icon (Pickup), Step 1 = Flag/Pin Icon (Dropoff)
                imageVector = if (step == 0) Icons.Default.Home else Icons.Default.LocationOn,
                contentDescription = "Pin",
                modifier = Modifier.size(50.dp).align(Alignment.Center).offset(y = (-25).dp),
                tint = if (step == 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
            
            // "Aiming" Text
            Card(
                modifier = Modifier.align(Alignment.Center).offset(y = (-60).dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Text(
                    text = if (step == 0) "Set Pickup" else "Set Dropoff",
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold,
                    color = if (step == 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }
        }

        // GPS Button
        FloatingActionButton(
            onClick = { zoomToUser() },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).offset(y = 50.dp),
            containerColor = Color.White
        ) { Icon(Icons.Default.MyLocation, contentDescription = "Locate Me", tint = Color(0xFF1E88E5)) }

        // --- BOTTOM SHEET UI ---
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(24.dp)
        ) {
            if (step == 0) {
                // PICKUP SELECTION
                Text("Where are you?", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        pickupGeo = currentCenter
                        pickupAddr = addressText
                        step = 1 // Next
                        Toast.makeText(context, "Pickup Set! Now drag to destination.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Confirm Pickup", color = Color.White) }
                
            } else if (step == 1) {
                // DROPOFF SELECTION
                Text("Where are you going?", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    OutlinedButton(onClick = { step = 0; pickupGeo = null }, modifier = Modifier.weight(1f)) { Text("Back") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { 
                            dropoffGeo = currentCenter
                            dropoffAddr = addressText
                            // Calc Price
                            val dist = FareCalculator.calculateDistance(pickupGeo!!.latitude, pickupGeo!!.longitude, dropoffGeo!!.latitude, dropoffGeo!!.longitude)
                            estimatedPrice = FareCalculator.calculatePrice(dist)
                            step = 2 // Review
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Set Destination") }
                }
                
            } else {
                // CONFIRMATION
                Text("Trip Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("🟢 From: $pickupAddr", style = MaterialTheme.typography.bodyMedium)
                Text("🔴 To: $dropoffAddr", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = locationNote, onValueChange = { locationNote = it },
                    label = { Text("Driver Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Price", color = Color.Gray)
                    Text("$estimatedPrice ETB", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        // SEND TO FIREBASE
                        val db = FirebaseDatabase.getInstance().getReference("trips")
                        val newId = UUID.randomUUID().toString()
                        val trip = Trip(
                            tripId = newId, customerId = "Yabu",
                            pickupLocation = Location(pickupGeo!!.latitude, pickupGeo!!.longitude, pickupAddr),
                            dropoffLocation = Location(dropoffGeo!!.latitude, dropoffGeo!!.longitude, dropoffAddr),
                            price = estimatedPrice, status = TripStatus.REQUESTED, pickupNotes = locationNote
                        )
                        db.child(newId).setValue(trip)
                        Toast.makeText(context, "Searching for Drivers...", Toast.LENGTH_LONG).show()
                        // Reset or show waiting screen logic (omitted for brevity)
                        step = 0
                        pickupGeo = null
                        dropoffGeo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("BOOK NOW", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
