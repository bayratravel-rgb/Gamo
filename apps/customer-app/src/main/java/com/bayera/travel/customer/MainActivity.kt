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
import com.bayera.travel.utils.FareCalculator
import java.io.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- 🛡️ CRASH PROTECTOR (SAFETY NET) ---
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fatal_log", sw.toString()); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent); android.os.Process.killProcess(android.os.Process.myPid())
        }

        if (intent.getStringExtra("fatal_log") != null) {
            setContent { ErrorScreenUI(intent.getStringExtra("fatal_log")!!) }; return
        }

        // --- 🔑 MANUAL FIREBASE INIT ---
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

        Configuration.getInstance().userAgentValue = "BayeraMarketing"
        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    val nav = rememberNavController()
    val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
    
    NavHost(navController = nav, startDestination = start) {
        composable("login") { LoginScreenUI(nav) }
        composable("dash") { DashboardUI(nav) }
        composable("ride_map") { RideMapBookingUI(nav) }
    }
}

@Composable
fun LoginScreenUI(nav: NavController) {
    val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome to Bayera", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
    val name = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("name", "User")
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
        Text("Hi, $name!", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = { nav.navigate("ride_map") }, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Hotel, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(40.dp))
                    Text("Hotels", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RideMapBookingUI(nav: NavController) {
    var mode by remember { mutableStateOf("PICKUP") } // PICKUP, DEST, SUMMARY
    var pickupLoc by remember { mutableStateOf(GeoPoint(6.02, 37.55)) }
    var destLoc by remember { mutableStateOf(GeoPoint(6.02, 37.55)) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true) } }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mapView.onResume()
            else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView.apply { controller.setZoom(16.0); controller.setCenter(pickupLoc) } }, modifier = Modifier.fillMaxSize())
        
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.ArrowBack, null, tint = Color.Black) }
        
        if (mode != "SUMMARY") {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
            Button(
                onClick = { if(mode=="PICKUP") { pickupLoc = mapView.mapCenter as GeoPoint; mode = "DEST" } else { destLoc = mapView.mapCenter as GeoPoint; mode = "SUMMARY" } },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red),
                shape = RoundedCornerShape(28.dp)
            ) { Text(if(mode=="PICKUP") "Set Pickup Here" else "Set Destination") }
        } else {
            val dist = FareCalculator.calculateDistance(pickupLoc.latitude, pickupLoc.longitude, destLoc.latitude, destLoc.longitude)
            val price = FareCalculator.calculatePrice(dist, VehicleType.BAJAJ)
            
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Trip Summary", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Est. Distance: ${String.format("%.2f", dist)} km", color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("$price ETB", style = MaterialTheme.typography.displaySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Button(onClick = { /* Firebase send */ }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) {
                        Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreenUI(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚠️ SYSTEM CRASH LOG", color = Color.Red, fontWeight = FontWeight.Bold)
        Text(log, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
    }
}
