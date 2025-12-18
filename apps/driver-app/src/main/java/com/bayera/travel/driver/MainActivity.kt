package com.bayera.travel.driver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueYou are thinking ahead! That is excellent.

**Current State:**
*   You confirm **Pickup** location (Where you are).
*   The price is calculated to a *dummy* destination (University).EventListener
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.TripStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch

**The Missing Pieces:**
1.  **Setting Destination:** The customer needs to select where they are going.
2.   (e: Exception) {}

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)**Navigation for Driver:** The driver needs to see the route on a map (Google Maps or Waze).

---
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "

### 🗺️ Plan for Day 10 (Advanced Navigation)

We will solve both problems:

1.  **Customer Appdashboard"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("dashboard") { DashboardScreen(navController) }
            }
        }
    }
}

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var currentAcceptedTrip by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")

        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                var myTrip: Trip? = null
                :**
    *   Add a **"Set Destination"** step.
    *   First, pin Pickup.
    *   Click "Next".
    *   Pin Destination.
    *   Calculate REAL price between Pickup <-> Destination.

2.  **Driver App:**
    *   Add a **"Navigate"** button.
    *   When clicked, it opens **Google Maps App** on the driver's phone with the route pre-loaded. (This is how Uber/Bolt do it to save money on building their own navigation).

---

### 🛠️ Step 1: Add "Set Destination" Flow
                for (child in snapshot.children) {
                    try {
                        val trip = child.getValue( (Customer)

We need to change the UI flow: **Pickup -> Destination -> Confirm Price**.

**Run this script:**

```bash
Trip::class.java)
                        if (trip != null) {
                            // If I accepted this trip, show it at the top
                            if (trip.driverId != null && trip.driverId!!.contains(driverName)) {
                               cd ~/Ira-para

cat > apps/customer-app/src/main/java/com/bayera/travel/customer/MainActivity.kt <<'EOF'
package com.bayera.travel.customer

import android.Manifest
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx. myTrip = trip
                            }
                            // Show available trips
                            if (trip.status == TripStatus.REQUESTED) {
                                trips.add(trip)
                            }
                        }
                    } catch (e: Exception) {}
                }
                activeTrips = trips.reversed()
                currentAcceptedTrip = mycompose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.composeTrip
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx5E9)) {
            Column(modifier = Modifier.padding(16.dp)) {
                // HEADER
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hi, $driverName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier..compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.weight(1f))
                    TextButton(onClick = { 
                        prefs.edit().clear().apply()
                        navController.navigate("login") { popUpTo(0) }
                    }) { Text("Logout", color = Color.Red) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CURRENTcompose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
import com.bayera.travel.utils.FareCalculator
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import JOB (If accepted)
                if (currentAcceptedTrip != null) {
                    Text("🟢 CURRENT JOB", style = MaterialTheme. kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroidtypography.titleMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold).events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java
                    CurrentJobCard(currentAcceptedTrip!!)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // INCOMING LIST
                Text("Incoming Requests", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (activeTrips.isEmpty()) {
                    .util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreateBox(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Searching...", style = MaterialTheme.typography.body(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeAppLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn {
                        items(activeTrips) { trip(this) } catch (e: Exception) {}
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
    
    // STATES
    // 0 = Selecting Pickup, 1 = Selecting Destination
    var selectionStep by remember { mutableIntStateOf(0) } 
 ->
                            TripCard(trip, "$driverName")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentJobCard(trip: Trip) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), // Light Green Highlight
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(    
    var pickupGeo by remember { mutableStateOf(GeoPoint(6.0206, 37.55516.dp)) {
            Text("Picking up: ${trip.customerId}", style = MaterialTheme.typography.titleLarge7)) }
    var dropoffGeo by remember { mutableStateOf(GeoPoint(6.0400, 37.5600)) }
    
    var addressText by remember { mutableStateOf("Locating...") }
    var currentGeo, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("📍 ${trip.pickupLocation.address}")
            
            Spacer(modifier = Modifier.height(16Point by remember { mutableStateOf(pickupGeo) }
    var isMapMoving by remember { mutableStateOf(false) }
    var estimatedPrice by remember { mutableStateOf(0.0) }

    Box.dp))
            
            // NAVIGATION BUTTON
            Button(
                onClick = { 
                    // Open Google Maps Navigation
                    val uri = "google.navigation:q=${trip.pickupLocation.lat},${trip.pickupLocation.lng}"(modifier = Modifier.fillMaxSize()) {
        
        // MAP
        AndroidView(
            factory = { ctx ->
                MapView
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.setPackage("com.google.android.apps.maps")
                    try {
                        context.startActivity(intent)
                    } catch (e(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true 
                    controller.setZoom(16.0)
                    controller.: Exception) {
                        Toast.makeText(context, "Google Maps not found", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0setCenter(pickupGeo)

                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): BooleanxFF1976D2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NAVIGATE TO CUSTOMER")
            }
        }
    }
}

@Composable { isMapMoving = true; return true }
                        override fun onZoom(event: ZoomEvent?): Boolean { isMapMoving = true; return true }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) currentGeoPoint = mapView.mapCenter as GeoPoint
            },
            modifier = Modifier.fillMaxSize()
        )

        // ADDRESS & PRICE LOGIC
        LaunchedEffect(isMapMoving) {
            if (isMapMoving) {
                kotlinx.coroutines.delay(600)
                isMapMoving
fun TripCard(trip: Trip, driverId: String) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👤 ${trip.customerId}", fontWeight = FontWeight.Bold)
            Text("📍 ${ = false 
                
                // Calculate Price only if setting Destination
                if (selectionStep == 1) {
                    val dist = FareCalculator.calculateDistance(
                        pickupGeo.latitude, pickupGeo.longitude,
                        currentGeoPointtrip.pickupLocation.address}")
            Text("💰 ${trip.price} ETB", color = Color(.latitude, currentGeoPoint.longitude
                    )
                    estimatedPrice = FareCalculator.calculatePrice(dist)0xFF2E7D32), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { 
                    val db = FirebaseDatabase.getInstance().
                }

                // Get Address Name
                scope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getReference("trips").child(trip.tripId)
                    db.updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(getFromLocation(currentGeoPoint.latitude, currentGeoPoint.longitude, 1)
                        if (!addresses.0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                isNullOrEmpty()) {
                            val line = addresses[0].getAddressLine(0)
                            val shortAddr = line.split(",").take(2).joinToString(",")
                            withContext(Dispatchers.Main) {Text("ACCEPT RIDE")
            }
        }
    }
}
