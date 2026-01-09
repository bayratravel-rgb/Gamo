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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import org.osmdroid.config.Configuration
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
        setContent {
            val navController = rememberNavController()
            MaterialTheme {
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { SuperDashboard(navController) }
                    composable("ride") { RideBookingUI(navController) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperDashboard(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = { navController.navigate("ride") }, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(36.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(36.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RideBookingUI(navController: NavController) {
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }.firstOrNull { it.status != TripStatus.COMPLETED }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx -> MapView(ctx).apply { setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK); controller.setZoom(16.0); controller.setCenter(GeoPoint(6.02, 37.55)) } }, modifier = Modifier.fillMaxSize())
        
        if (activeTrip == null) {
            Button(onClick = { /* book logic */ }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                Text("Set Pickup Here", fontWeight = FontWeight.Bold)
            }
        } else {
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Text("Fare: ${activeTrip!!.price} ETB", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
