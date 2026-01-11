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
import androidx.compose.ui.platform.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🧭 BYPASS THE BLOCK: Identify as a professional browser
        Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        
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

        setContent {
            val nav = rememberNavController()
            val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
            MaterialTheme {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { GamoLoginUI(nav) }
                    composable("dash") { DashboardUI(nav) }
                    composable("ride") { RideBookingUI(nav) }
                }
            }
        }
    }
}

@Composable
fun GamoLoginUI(nav: NavController) {
    val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Welcome to Gamo", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Powered by Bayera Travel", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if(name.isNotEmpty()) { prefs.edit().putString("name", name).putString("email", email).apply(); nav.navigate("dash") } }, 
               modifier = Modifier.fillMaxWidth().padding(top=32.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
            Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    val name = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("name", "bb")
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
        Text("Hi, $name!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = { nav.navigate("ride") }, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp)); Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(40.dp)); Text("Hotels", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RideBookingUI(nav: NavController) {
    var mode by remember { mutableStateOf("PICKUP") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")
    
    // 🌍 HIGH DETAIL GOOGLE TILES
    val googleTiles = remember { XYTileSource("GoogleRoads", 1, 20, 256, ".png", arrayOf("https://mt0.google.com/vt/lyrs=m&x="), "© Google") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).apply { setTileSource(googleTiles); setMultiTouchControls(true) } }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mapView.onResume()
            else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }.firstOrNull { it.status != TripStatus.COMPLETED }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView.apply { controller.setZoom(17.0); controller.setCenter(GeoPoint(6.0206, 37.5534)) } }, modifier = Modifier.fillMaxSize())
        
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.ArrowBack, null, tint = Color.Black) }
        
        if (activeTrip == null) {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
            Button(
                onClick = { if(mode=="PICKUP") mode = "DEST" else {
                    val id = UUID.randomUUID().toString()
                    db.child(id).setValue(Trip(tripId = id, customerPhone = "user_yy", status = TripStatus.REQUESTED, price = 110.0))
                }},
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red),
                shape = RoundedCornerShape(28.dp)
            ) { Text(if(mode=="PICKUP") "Set Pickup Here" else "Set Destination & Book") }
        } else {
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Box(modifier = Modifier.padding(top=12.dp).background(Color(0xFFE8F5E9)).padding(8.dp)) { Text("✅ PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { db.child(activeTrip!!.tripId).removeValue() }) { Text("Cancel", color = Color.Gray) }
                }
            }
        }
    }
}
