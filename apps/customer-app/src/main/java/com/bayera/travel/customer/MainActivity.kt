package com.bayera.travel.customer

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BayeraTravelApp"
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
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
            
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { GamoLoginScreen(nav) }
                    composable("dash") { BayeraDashboard(nav) }
                    composable("ride_map") { RideMapScreen(nav) }
                }
            }
        }
    }
}

@Composable
fun GamoLoginScreen(nav: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id = context.resources.getIdentifier("logo", "drawable", context.packageName)), contentDescription = null, modifier = Modifier.size(160.dp), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Welcome to Gamo", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Powered by Bayera Travel", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if(name.isNotEmpty() && email.contains("@")) {
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().putString("name", name).putString("email", email).apply()
            nav.navigate("dash")
        }}, modifier = Modifier.fillMaxWidth().padding(top=32.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White)) { Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayeraDashboard(nav: NavController) {
    val context = LocalContext.current
    val name = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("name", "User")
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E1E1E)) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Bayera Travel", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Hi, $name!", color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.weight(1f)); IconButton(onClick = {}) { Icon(Icons.Default.Settings, null, tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Card(onClick = { nav.navigate("ride_map") }, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp)); Text("Ride", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Card(onClick = {}, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Hotel, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(40.dp)); Text("Hotels", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun RideMapScreen(nav: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); isTilesScaledToDpi = true } }
    var mode by remember { mutableStateOf("PICKUP") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mapView.onResume()
            else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView.apply { controller.setZoom(16.0); controller.setCenter(GeoPoint(6.02, 37.55)) } }, modifier = Modifier.fillMaxSize())
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) { Icon(Icons.Default.ArrowBack, null, tint = Color.Black) }
        
        if (mode != "SUMMARY") {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
            Button(onClick = { mode = if(mode=="PICKUP") "DEST" else "SUMMARY" }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red), shape = RoundedCornerShape(28.dp)) {
                Text(if(mode=="PICKUP") "Set Pickup Here" else "Set Destination & Book")
            }
        } else {
            Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Trip Summary", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("Fare: 110.0 ETB", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth().padding(top=16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) { Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
