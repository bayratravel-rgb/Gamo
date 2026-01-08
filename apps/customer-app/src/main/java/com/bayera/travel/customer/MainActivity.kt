package com.bayera.travel.customer

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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BayeraMarketingDemo"
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    var screen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    // Listen for Trip updates to handle "Locked" status screen automatically
    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == "user_bb" && it.status != TripStatus.COMPLETED }
                if (activeTrip != null) screen = "status"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        bottomBar = {
            if (activeTrip == null && screen != "map") {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = screen == "home", onClick = { screen = "home" })
                    NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
                }
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            when (screen) {
                "home" -> DashboardUI { screen = "map" }
                "map" -> MapBookingUI { screen = "home" }
                "status" -> activeTrip?.let { StatusUI(it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Services", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRideClick, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(32.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(32.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFF7B1FA2))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Hotels & Resorts", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MapBookingUI(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // REAL OSMDROID MAP
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(6.02, 37.55)) // Arba Minch center
            }
        }, modifier = Modifier.fillMaxSize())

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
            Icon(Icons.Default.ArrowBack, null)
        }

        Button(
            onClick = { /* Booking logic here */ },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Set Pickup Here", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun StatusUI(trip: Trip) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(64.dp))
            Text("Driver Found!", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32))
            Text("Status: ${trip.status}")
        }
    }
}
