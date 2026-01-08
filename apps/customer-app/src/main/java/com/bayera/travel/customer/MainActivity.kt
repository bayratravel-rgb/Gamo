package com.bayera.travel.customer

import android.os.Bundle
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
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel").build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {}
        setContent {
            val nav = rememberNavController()
            MaterialTheme {
                NavHost(navController = nav, startDestination = "dash") {
                    composable("dash") { DashboardUI(nav) }
                    composable("ride") { RideBookingUI(nav) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(onClick = { nav.navigate("ride") }, modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(35.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(modifier = Modifier.weight(1f).height(130.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(35.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideBookingUI(nav: NavController) {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var mode by remember { mutableStateOf("PICKUP") } // PICKUP, DEST, SUMMARY
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    var note by remember { mutableStateOf("") }
    val mapState = remember { mutableStateOf<MapView?>(null) }

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
        AndroidView(factory = { ctx -> MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); controller.setZoom(16.0); controller.setCenter(GeoPoint(6.022, 37.559)); mapState.value = this } }, modifier = Modifier.fillMaxSize())
        
        if (activeTrip == null) {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
            
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (mode == "PICKUP") {
                        Text("Start Trip From?", color = Color.Gray); Text("Arba Minch Center", fontWeight = FontWeight.Bold)
                        Button(onClick = { mode = "DEST" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Set Pickup Here") }
                    } else if (mode == "DEST") {
                        Text("Where to?", color = Color.Gray); Text("Select Destination", fontWeight = FontWeight.Bold)
                        Button(onClick = { mode = "SUMMARY" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Set Destination Here") }
                    } else {
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Notes for Driver (Accuracy)") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            val id = UUID.randomUUID().toString()
                            db.child(id).setValue(Trip(tripId = id, customerPhone = "user_bb", dropoffLocation = Location(address = "Destination"), notes = note, price = 110.0))
                        }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) { Text("BOOK RIDE • 110 ETB", color = Color.Black) }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if(activeTrip!!.status == TripStatus.IN_PROGRESS) "EN ROUTE" else "FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                    Text("Driver: ${activeTrip!!.driverName ?: "Partner"}")
                    Text("Fare: ${activeTrip!!.price} ETB")
                }
            }
        }
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.ArrowBack, null) }
    }
}
