package com.bayera.travel.customer

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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
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
        Configuration.getInstance().userAgentValue = "BayeraApp"
        try {
            val opt = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel").build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, opt)
        } catch (e: Exception) {}
        setContent {
            val nav = rememberNavController()
            MaterialTheme {
                NavHost(navController = nav, startDestination = "dash") {
                    composable("dash") { DashboardUI(nav) }
                    composable("ride") { RideBookingFlow(nav) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = { nav.navigate("ride") }, modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(40.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RideBookingFlow(nav: NavController) {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    var mode by remember { mutableStateOf("PICKUP") }
    val mapView = remember { MapView(nav.context).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true) } }

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == "user_bb" && it.status != TripStatus.COMPLETED }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView.apply { controller.setZoom(16.0); controller.setCenter(GeoPoint(6.02, 37.55)) } }, modifier = Modifier.fillMaxSize())
        
        if (activeTrip == null) {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(if(mode=="PICKUP") "Confirm Pickup" else "Confirm Destination", fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { if(mode=="PICKUP") mode = "DEST" else {
                            val id = UUID.randomUUID().toString()
                            db.child(id).setValue(Trip(tripId = id, customerPhone = "user_bb", status = TripStatus.REQUESTED, price = 110.0))
                        }},
                        modifier = Modifier.fillMaxWidth().padding(top=12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
                    ) { Text("Set Here") }
                }
            }
        } else {
            // THE LOCKED UI
            val isEnRoute = activeTrip!!.status == TripStatus.IN_PROGRESS
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = if(isEnRoute) Color(0xFFFFEBEE) else Color.White)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if(isEnRoute) "LOCKED: EN ROUTE" else "FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black)
                    Text("Driver: ${activeTrip!!.driverName ?: "Partner"}")
                    Text("Fare: ${activeTrip!!.price} ETB", fontWeight = FontWeight.Bold)
                    if(!isEnRoute) TextButton(onClick = { db.child(activeTrip!!.tripId).removeValue() }) { Text("Cancel Request", color = Color.Gray) }
                }
            }
        }
    }
}
