package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔑 MANUAL FIREBASE INIT (Proven Stable)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {}

        Configuration.getInstance().userAgentValue = "Mozilla/5.0"
        setContent { MaterialTheme { CustomerMasterApp() } }
    }
}

@Composable
fun CustomerMasterApp() {
    var screen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")
    val userPhone = "user_yy"

    // THE HANDSHAKE ENGINE
    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == userPhone && it.status != TripStatus.COMPLETED }
                if (activeTrip != null) screen = "status"
                else if (screen == "status") screen = "home"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 🌍 BASE LAYER: THE MAP (Always Rendering) ---
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val googleTiles = remember { XYTileSource("GoogleRoads", 1, 20, 256, ".png", arrayOf("https://mt0.google.com/vt/lyrs=m&x="), "© Google") }
        val mapView = remember { MapView(context).apply { setTileSource(googleTiles); setMultiTouchControls(true) } }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) mapView.onResume()
                else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        AndroidView(factory = { mapView.apply { controller.setZoom(16.0); controller.setCenter(GeoPoint(6.0206, 37.5534)) } }, modifier = Modifier.fillMaxSize())

        // --- 💳 TOP LAYER: UI OVERLAYS ---
        if (screen == "home") {
            DashboardOverlay { screen = "map" }
        } else if (screen == "map") {
            BookingOverlay(db, userPhone) { screen = "home" }
        } else if (screen == "status") {
            activeTrip?.let { StatusOverlay(it, db) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardOverlay(onRide: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hi, yy!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRide, modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2)); Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00)); Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BookingOverlay(db: DatabaseReference, phone: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
            Icon(Icons.Default.ArrowBack, null)
        }
        Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = Color(0xFF2E7D32))
        Button(
            onClick = {
                val id = UUID.randomUUID().toString()
                db.child(id).setValue(Trip(tripId = id, customerPhone = phone, status = TripStatus.REQUESTED, price = 110.0))
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Set Pickup Here", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun StatusOverlay(trip: Trip, db: DatabaseReference) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EBF2))) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                Text("Fare: ${trip.price} ETB", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.background(Color.White).padding(8.dp)) { Text("✅ PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
                
                // THE ESCAPE KEY
                TextButton(onClick = { db.child(trip.tripId).removeValue() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Cancel Request & Reset Dashboard", color = Color.Gray)
                }
            }
        }
    }
}
