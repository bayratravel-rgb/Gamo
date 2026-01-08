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
import androidx.navigation.NavController
import androidx.navigation.compose.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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
            val nav = rememberNavController()
            MaterialTheme {
                NavHost(navController = nav, startDestination = "dash") {
                    composable("dash") { DashboardUI(nav) }
                    composable("ride") { RideFlowUI(nav) }
                    composable("payment/{tripId}/{price}") { b -> 
                        PaymentScreen(nav, b.arguments?.getString("tripId") ?: "", b.arguments?.getString("price") ?: "0") 
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Hi, yy!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Services", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ServiceTile("Ride", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Color(0xFF1976D2)) { nav.navigate("ride") }
            ServiceTile("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFF57C00)) {}
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hotel, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(32.dp))
                Column(modifier = Modifier.padding(start = 16.dp)) { Text("Hotels & Resorts", fontWeight = FontWeight.Bold); Text("Book your stay", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun ServiceTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, iconColor: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.height(140.dp).width(160.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = iconColor)
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RideFlowUI(nav: NavController) {
    var mode by remember { mutableStateOf("MAP") } // MAP -> SUMMARY -> STATUS
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == "user_yy" && it.status != TripStatus.COMPLETED }
                if (activeTrip != null) mode = "STATUS"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx -> MapView(ctx).apply { setTileSource(TileSourceFactory.MAPNIK); controller.setZoom(16.0); controller.setCenter(GeoPoint(6.022, 37.559)) } }, modifier = Modifier.fillMaxSize())
        
        if (mode == "MAP") {
            Button(onClick = { mode = "SUMMARY" }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), shape = RoundedCornerShape(28.dp)) { Text("Set Pickup Here") }
        } else if (mode == "SUMMARY") {
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Trip Summary", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Row(modifier = Modifier.padding(vertical = 12.dp)) {
                        SuggestionChip(onClick = {}, label = { Text("COMFORT") })
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text("LUXURY") })
                    }
                    Text("Total Price: 110.0 ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Button(onClick = { 
                        val id = UUID.randomUUID().toString()
                        db.child(id).setValue(Trip(tripId = id, customerPhone = "user_yy", price = 110.0, status = TripStatus.REQUESTED))
                        nav.navigate("payment/$id/110")
                    }, modifier = Modifier.fillMaxWidth().padding(top=16.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) { Text("BOOK RIDE", color = Color.Black) }
                }
            }
        } else if (mode == "STATUS") {
            activeTrip?.let { 
                Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅ Driver Found!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text("Driver: ${it.driverName ?: "Partner"}")
                        if (it.status == TripStatus.IN_PROGRESS) Text("LOCKED: EN ROUTE", color = Color.Red, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentScreen(nav: NavController, tripId: String, price: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CreditCard, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(64.dp))
        Text("Pay for Ride", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("$price.0 ETB", fontSize = 48.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { /* Chapa Call */ }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))) { Text("Pay Now") }
        TextButton(onClick = { nav.popBackStack() }) { Text("Pay Cash to Driver", color = Color(0xFF673AB7)) }
    }
}
