package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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
        Configuration.getInstance().userAgentValue = "BayeraTravel"
        
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel")
                .build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {}

        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    var screen by remember { mutableStateOf("home") }
    var selectionMode by remember { mutableStateOf("PICKUP") } // PICKUP, DEST, SUMMARY
    var pickupPoint by remember { mutableStateOf(GeoPoint(6.02, 37.55)) }
    var destPoint by remember { mutableStateOf(GeoPoint(6.03, 37.56)) }
    var vehicleType by remember { mutableStateOf("COMFORT") }
    var note by remember { mutableStateOf("") }
    
    val mapState = remember { mutableStateOf<MapView?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    Scaffold(
        bottomBar = {
            if (screen == "home") {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
                    NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
                }
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            if (screen == "home") {
                DashboardUI { screen = "map" }
            } else {
                RideMapUI(
                    selectionMode, 
                    { selectionMode = it },
                    { pickupPoint = it },
                    { destPoint = it },
                    vehicleType, { vehicleType = it },
                    note, { note = it },
                    mapState,
                    onBack = { screen = "home"; selectionMode = "PICKUP" },
                    onBook = {
                        val id = UUID.randomUUID().toString()
                        db.child(id).setValue(Trip(tripId = id, customerPhone = "user_bb", pickupLocation = Location(pickupPoint.latitude, pickupPoint.longitude, "Pickup"), dropoffLocation = Location(destPoint.latitude, destPoint.longitude, "Destination"), vehicleType = vehicleType, notes = note, price = 110.0))
                        screen = "home" 
                        selectionMode = "PICKUP"
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(onClick = onRideClick, modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(35.dp), tint = Color(0xFF1976D2))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(35.dp), tint = Color(0xFFF57C00))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideMapUI(mode: String, onModeChange: (String) -> Unit, onPickupSet: (GeoPoint) -> Unit, onDestSet: (GeoPoint) -> Unit, vehicle: String, onVehicleChange: (String) -> Unit, note: String, onNoteChange: (String) -> Unit, mapState: MutableState<MapView?>, onBack: () -> Unit, onBook: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // --- 🧭 MAP ---
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(6.022, 37.559))
                mapState.value = this
            }
        }, modifier = Modifier.fillMaxSize())

        // --- 🎯 CENTER PIN ---
        if (mode != "SUMMARY") {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode == "PICKUP") Color(0xFF2E7D32) else Color.Red)
        }

        // --- 🧭 RE-CENTER FAB ---
        FloatingActionButton(onClick = { mapState.value?.controller?.animateTo(GeoPoint(6.022, 37.559)) }, modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp), containerColor = Color.White, shape = CircleShape) {
            Icon(Icons.Default.MyLocation, null, tint = Color(0xFF1976D2))
        }

        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.ArrowBack, null) }

        // --- 💳 DYNAMIC CARDS ---
        Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (mode == "PICKUP") {
                    Text("Start Trip From?", color = Color.Gray); Text("Arba Minch Center", fontWeight = FontWeight.Bold)
                    Button(onClick = { onPickupSet(mapState.value?.mapCenter as GeoPoint); onModeChange("DEST") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Set Pickup Here") }
                } else if (mode == "DEST") {
                    Text("Where to?", color = Color.Gray); Text("Arba Minch Area", fontWeight = FontWeight.Bold)
                    Button(onClick = { onDestSet(mapState.value?.mapCenter as GeoPoint); onModeChange("SUMMARY") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Set Destination Here") }
                } else {
                    Text("Trip Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("COMFORT", "LUXURY", "POOL").forEach { type ->
                            FilterChip(selected = vehicle == type, onClick = { onVehicleChange(type) }, label = { Text(type) })
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = onNoteChange, label = { Text("Note for Driver (Accuracy)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = onBook, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) { Text("BOOK RIDE • 110 ETB", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
