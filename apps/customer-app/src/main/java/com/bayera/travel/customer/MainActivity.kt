package com.bayera.travel.customer

import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn // Standard Pin
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place // Replacement for MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
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
    
    // Arba Minch Coordinates
    val startGeo = GeoPoint(6.0206, 37.5557)
    
    var addressText by remember { mutableStateOf("Fetching location...") }
    var currentGeoPoint by remember { mutableStateOf(startGeo) }
    var isMapMoving by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. MAP
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    controller.setCenter(startGeo)
                    minZoomLevel = 10.0
                    maxZoomLevel = 20.0

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

        // DETECT IDLE
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
                        withContext(Dispatchers.Main) { addressText = "Unknown Location" }
                    }
                }
            }
        }

        // 2. CENTER PIN
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Pin",
            modifier = Modifier.size(50.dp).align(Alignment.Center).offset(y = (-25).dp),
            tint = Color(0xFFD32F2F)
        )

        // 3. TOP MENU
        SmallFloatingActionButton(
            onClick = {},
            modifier = Modifier.padding(top = 40.dp, start = 16.dp).align(Alignment.TopStart),
            containerColor = Color.White
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }

        // 4. BOTTOM SHEET
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
        ) {
            Text("Confirm pick-up point", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // CHANGED ICON HERE (Used 'Place' instead of 'MyLocation')
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF1E88E5))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    val db = FirebaseDatabase.getInstance().getReference("trips")
                    val newId = UUID.randomUUID().toString()
                    val trip = Trip(
                        tripId = newId,
                        customerId = "Yabu (Map User)",
                        pickupLocation = Location(currentGeoPoint.latitude, currentGeoPoint.longitude, addressText),
                        price = 150.0,
                        status = TripStatus.REQUESTED
                    )
                    db.child(newId).setValue(trip)
                    Toast.makeText(context, "Request sent!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Confirm Pickup", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
