package com.bayera.travel.customer

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
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
import androidx.lifecycle.*
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
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val opt = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel").build()
                FirebaseApp.initializeApp(this, opt)
            }
        } catch (e: Exception) {}

        Configuration.getInstance().userAgentValue = "Mozilla/5.0"
        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    var currentScreen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == "user_yy" && it.status != TripStatus.COMPLETED }
                if (activeTrip != null) currentScreen = "status"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        bottomBar = {
            if (activeTrip == null && currentScreen == "home") {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
                    NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
                }
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            when (currentScreen) {
                "home" -> SuperDashboard { currentScreen = "map" }
                "map" -> InteractiveMapFlow(db) { currentScreen = "home" }
                "status" -> activeTrip?.let { StatusOverlayUI(it, db) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperDashboard(onRide: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Services", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRide, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hotel, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.padding(start = 16.dp)) { Text("Hotels & Resorts", fontWeight = FontWeight.Bold); Text("Book your stay", fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMapFlow(db: DatabaseReference, onBack: () -> Unit) {
    var mode by remember { mutableStateOf("PICKUP") } // PICKUP -> DEST -> SUMMARY
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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView.apply { controller.setZoom(17.0); controller.setCenter(GeoPoint(6.02, 37.55)) } }, modifier = Modifier.fillMaxSize())
        
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.ArrowBack, null) }
        
        if (mode != "SUMMARY") {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
            Button(
                onClick = { if(mode=="PICKUP") mode = "DEST" else mode = "SUMMARY" },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red),
                shape = RoundedCornerShape(28.dp)
            ) { Text(if(mode=="PICKUP") "Set Pickup Here" else "Set Destination Here", fontWeight = FontWeight.Bold) }
        } else {
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Trip Summary", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Row(modifier = Modifier.padding(vertical = 12.dp)) {
                        SuggestionChip(onClick = {}, label = { Text("COMFORT") })
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text("LUXURY") })
                    }
                    Button(onClick = {
                        val id = UUID.randomUUID().toString()
                        db.child(id).setValue(Trip(tripId = id, customerPhone = "user_yy", price = 110.0, status = TripStatus.REQUESTED))
                    }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) {
                        Text("BOOK RIDE • 110 ETB", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusOverlayUI(trip: Trip, db: DatabaseReference) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Map stays visible in background
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFFF1EBF2), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
            Text("Fare: ${trip.price} ETB", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) { Text("✅ PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal=16.dp, vertical=8.dp)) }
            TextButton(onClick = { db.child(trip.tripId).removeValue() }, modifier = Modifier.padding(top=16.dp)) { Text("Cancel & Reset", color = Color.Gray) }
        }
    }
}
