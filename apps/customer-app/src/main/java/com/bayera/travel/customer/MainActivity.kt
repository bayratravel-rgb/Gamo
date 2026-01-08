package com.bayera.travel.customer

import android.content.*
import android.os.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BayeraTravel"
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    var screen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")
    val userPhone = "user_yy"

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
        when (screen) {
            "home" -> DashboardUI { screen = "map" }
            "map" -> MapRideUI(db, userPhone) { screen = "home" }
            "status" -> activeTrip?.let { StatusUI(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hi, yy!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRideClick, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(40.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MapRideUI(db: DatabaseReference, phone: String, onBack: () -> Unit) {
    val googleRoadmap = XYTileSource(
        "GoogleRoadmap", 1, 20, 256, ".png",
        arrayOf("https://mt0.google.com/vt/lyrs=m&x=", "https://mt1.google.com/vt/lyrs=m&x=", "https://mt2.google.com/vt/lyrs=m&x=")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(googleRoadmap) // HIGH DETAIL GOOGLE TILES
                setMultiTouchControls(true)
                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(6.0206, 37.5534)) // Centered on Arba Minch Landmarks
            }
        }, modifier = Modifier.fillMaxSize())

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.Black)
        }

        Button(
            onClick = {
                val id = UUID.randomUUID().toString()
                db.child(id).setValue(Trip(tripId = id, customerPhone = phone, status = TripStatus.REQUESTED, price = 110.0))
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Set Pickup", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
    }
}

@Composable
fun StatusUI(trip: Trip) {
    // This uses the exact color and style from your first screenshot
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFFF1EBF2), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
            Text("Driver: ${trip.driverName ?: "Partner"}", style = MaterialTheme.typography.titleMedium)
            Text("Fare: ${trip.price} ETB", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
