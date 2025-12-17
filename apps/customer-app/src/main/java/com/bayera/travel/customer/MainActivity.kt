package com.bayera.travel.customer

import android.Manifest
import android.content.Context
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
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            // Check Login Status
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
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val userPhone = prefs.getString("phone", "") ?: ""

    // --- ZOOM LIMITS ---
    val ethiopiaCenter = GeoPoint(9.145, 40.489)
    
    var addressText by remember { mutableStateOf("አካባቢውን በመፈለግ ላይ...") }
    var currentGeoPoint by remember { mutableStateOf(ethiopiaCenter) }
    var isMapMoving by remember { mutableStateOf(false) }
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    
    // --- HANDSHAKE LOGIC ---
    var currentTrip by remember { mutableStateOf<Trip?>(null) }

    // Listen for Trip Updates (Driver Accepted?)
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

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun zoomToUser() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val userPos = GeoPoint(loc.latitude, loc.longitude)
                    mapController?.animateTo(userPos)
                    mapController?.setZoom(18.5)
                    currentGeoPoint = userPos
                }
            }
        } catch (e: SecurityException) {}
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) zoomToUser()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            zoomToUser()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true 
                    
                    // --- ENFORCE ZOOM LIMITS ---
                    minZoomLevel = 6.0 // Can't zoom out past Ethiopia view
                    maxZoomLevel = 22.0
                    
                    controller.setZoom(6.0)
                    controller.setCenter(ethiopiaCenter)
                    mapController = controller

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { isMapMoving = true; return true }
                        override fun onZoom(event: ZoomEvent?): Boolean { isMapMoving = true; return true }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) currentGeoPoint = mapView.mapCenter as GeoPoint
            },
            modifier = Modifier.fillMaxSize()
        )

        // Address Fetcher
        LaunchedEffect(isMapMoving) {
            if (isMapMoving) {
                kotlinx.coroutines.delay(800)
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
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { addressText = "የማይታወቅ ቦታ" }
                    }
                }
            }
        }

        // Center Pin
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin",
            modifier = Modifier.size(48.dp).align(Alignment.Center).offset(y = (-24).dp),
            tint = Color(0xFFD32F2F)
        )

        // Search Bar
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("ወዴት ነው?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("መዳረሻዎን ያስገቡ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF1E88E5))
            }
        }

        FloatingActionButton(
            onClick = { zoomToUser() },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).offset(y = 50.dp),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color(0xFF1E88E5))
        }

        // --- DYNAMIC BOTTOM SHEET ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
        ) {
            if (currentTrip == null) {
                // STATE 1: IDLE
                Text("የመነሻ ቦታን ያረጋግጡ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF1E88E5))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { 
                        val db = FirebaseDatabase.getInstance().getReference("trips")
                        val newId = UUID.randomUUID().toString()
                        val trip = Trip(
                            tripId = newId,
                            customerId = "$userName ($userPhone)",
                            pickupLocation = Location(currentGeoPoint.latitude, currentGeoPoint.longitude, addressText),
                            price = 150.0,
                            status = TripStatus.REQUESTED
                        )
                        db.child(newId).setValue(trip)
                        currentTrip = trip
                        Toast.makeText(context, "ጥያቄ ተልኳል!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("አረጋግጥ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else if (currentTrip!!.status == TripStatus.REQUESTED) {
                // STATE 2: WAITING
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF1E88E5), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("ሹፌር በመፈለግ ላይ...", style = MaterialTheme.typography.titleMedium)
                }
            } else if (currentTrip!!.status == TripStatus.ACCEPTED) {
                // STATE 3: ACCEPTED
                Text("✅ ሹፌር ተገኝቷል!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("ሹፌር: ${currentTrip!!.driverId}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Call Driver Logic */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("ይደውሉ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
