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
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
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
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            AppUI()
        }
    }
}

@Composable
fun AppUI() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Default: Arba Minch
    val startGeo = GeoPoint(6.0206, 37.5557)
    
    var addressText by remember { mutableStateOf("አካባቢውን በመፈለግ ላይ...") } // "Locating..."
    var currentGeoPoint by remember { mutableStateOf(startGeo) }
    var isMapMoving by remember { mutableStateOf(false) }
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }

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
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            zoomToUser()
        }
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
                    setTileSource(TileSourceFactory.MAPNIK) // Using Standard Map
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true 
                    controller.setZoom(15.0)
                    controller.setCenter(startGeo)
                    mapController = controller

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean {
                            isMapMoving = true
                            return true
                        }
                        override fun onZoom(event: ZoomEvent?): Boolean {
                            isMapMoving = true
                            return true
                        }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) {
                    currentGeoPoint = mapView.mapCenter as GeoPoint
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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
                        withContext(Dispatchers.Main) { addressText = "የማይታወቅ ቦታ" } // Unknown Loc
                    }
                }
            }
        }

        // Pin
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin",
            modifier = Modifier.size(48.dp).align(Alignment.Center).offset(y = (-24).dp),
            tint = Color(0xFFD32F2F)
        )

        // AMHARIC SEARCH BAR
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("ወዴት ነው?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) // Where to?
                    Text("መዳረሻዎን ያስገቡ", style = MaterialTheme.typography.bodySmall, color = Color.Gray) // Enter dest
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF1E88E5))
            }
        }

        // GPS Button
        FloatingActionButton(
            onClick = { zoomToUser() },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).offset(y = 50.dp),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color(0xFF1E88E5))
        }

        // AMHARIC BOTTOM SHEET
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
        ) {
            Text("የመነሻ ቦታን ያረጋግጡ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) // Confirm Pickup
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
                        customerId = "Yabu (Amharic)",
                        pickupLocation = Location(currentGeoPoint.latitude, currentGeoPoint.longitude, addressText),
                        price = 150.0,
                        status = TripStatus.REQUESTED
                    )
                    db.child(newId).setValue(trip)
                    Toast.makeText(context, "ጥያቄ ተልኳል!", Toast.LENGTH_SHORT).show() // Request Sent
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("አረጋግጥ", color = Color.Black, fontWeight = FontWeight.Bold) // Confirm
            }
        }
    }
}
