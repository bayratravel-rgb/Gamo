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
        
        // Manual Firebase Init
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel").build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {}

        Configuration.getInstance().userAgentValue = "BayeraTravel"
        setContent { MaterialTheme { CustomerMasterUI() } }
    }
}

@Composable
fun CustomerMasterUI() {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val userPhone = "user_yy"

    // High Detail Google Tile Source
    val googleTiles = XYTileSource(
        "GoogleRoads", 1, 20, 256, ".png",
        arrayOf("https://mt0.google.com/vt/lyrs=m&x=", "https://mt1.google.com/vt/lyrs=m&x=", "https://mt2.google.com/vt/lyrs=m&x="),
        "© Google"
    )

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
        // --- 🗺️ THE MAP IS NOW THE BASE LAYER (ALWAYS VISIBLE) ---
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(googleTiles)
                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(6.0206, 37.5534))
                setMultiTouchControls(true)
            }
        }, modifier = Modifier.fillMaxSize())

        // --- 💳 OVERLAY UI ---
        if (activeTrip == null) {
            // Map Selection Mode
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
            // Status Mode (Finding Driver)
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xFFF1EBF2), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FINDING DRIVER", color = Color.Red, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                Text("Fare: ${activeTrip!!.price} ETB", fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32))
                    Text(" PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }

                // 🔓 CANCEL BUTTON (To let you go back to map)
                TextButton(onClick = { db.child(activeTrip!!.tripId).removeValue() }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Cancel Request", color = Color.Gray)
                }
            }
        }
    }
}
