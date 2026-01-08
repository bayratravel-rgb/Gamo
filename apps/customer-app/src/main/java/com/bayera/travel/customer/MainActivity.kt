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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.io.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🛡️ CRASH PROTECTOR (Big Brother's Safety Net)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fatal_log", sw.toString()); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent); android.os.Process.killProcess(android.os.Process.myPid())
        }

        if (intent.getStringExtra("fatal_log") != null) {
            setContent { ErrorUI(intent.getStringExtra("fatal_log")!!) }; return
        }

        // 🔑 MANUAL FIREBASE INIT (Proven to work)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {}

        // 🧭 OSMDROID CONFIG (Crucial for tile downloading)
        Configuration.getInstance().userAgentValue = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        
        setContent { MaterialTheme { CustomerMasterUI() } }
    }
}

@Composable
fun CustomerMasterUI() {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val userPhone = "user_yy"

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == userPhone && it.status != TripStatus.COMPLETED }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // 🌍 HIGH DETAIL GOOGLE TILES (Roadmap view from screenshot 2)
        val googleTiles = remember { XYTileSource(
            "GoogleRoads", 1, 20, 256, ".png",
            arrayOf("https://mt0.google.com/vt/lyrs=m&x=", "https://mt1.google.com/vt/lyrs=m&x=", "https://mt2.google.com/vt/lyrs=m&x="),
            "© Google"
        )}

        val mapView = remember { MapView(context).apply { 
            setTileSource(googleTiles)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(6.0206, 37.5534)) // Arba Minch Landmarks
        } }

        // Managing Map Lifecycle (Required for rendering)
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // --- 💳 OVERLAY UI ---
        if (activeTrip == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = Color(0xFF2E7D32))
                Button(
                    onClick = {
                        val id = UUID.randomUUID().toString()
                        db.child(id).setValue(Trip(tripId = id, customerPhone = userPhone, status = TripStatus.REQUESTED, price = 110.0))
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                    shape = RoundedCornerShape(28.dp)
                ) { Text("Set Pickup", fontWeight = FontWeight.Bold) }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.98f), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                Text("Fare: ${activeTrip!!.price} ETB", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                    Text(" PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                TextButton(onClick = { db.child(activeTrip!!.tripId).removeValue() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Cancel and Return to Map", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ErrorUI(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚠️ SYSTEM ERROR", color = Color.Red, fontWeight = FontWeight.Bold)
        Text(log, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
    }
}
