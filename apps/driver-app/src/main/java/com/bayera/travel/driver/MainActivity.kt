package com.bayera.travel.driver

import android.content.*
import android.os.*
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual Firebase Init (Same as Customer App for consistency)
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:656f004fce2ecda85af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel")
                .build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {}

        setContent { MaterialTheme { DriverSuperApp() } }
    }
}

@Composable
fun DriverSuperApp() {
    val driverId = "0900000000" // Placeholder
    val driverName = "Partner_Arba"
    val db = FirebaseDatabase.getInstance().getReference()
    
    var balance by remember { mutableStateOf(500.0) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var activeJob by remember { mutableStateOf<Trip?>(null) }
    var isOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Listen for requests
        db.child("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>()
                var current: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverName && t.status != TripStatus.COMPLETED) current = t
                        if (t.status == TripStatus.REQUESTED) list.add(t)
                    }
                }
                requests = list.reversed()
                activeJob = current
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        
        // Listen for balance
        db.child("drivers").child(driverId).child("balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 500.0 }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA))) {
        // TOP BAR
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Hi, $driverName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if(isOnline) "🟢 Online" else "⚪ Offline", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = isOnline, onCheckedChange = { isOnline = it })
        }

        // EARNINGS CARD (Matches your screenshot)
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Earnings", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("${String.format("%.1f", balance)} ETB", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Job Dashboard", modifier = Modifier.padding(start = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = Color.Gray)

        if (activeJob != null) {
            ActiveJobPanel(activeJob!!, db)
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(requests) { trip ->
                    IncomingRequestCard(trip) {
                        db.child("trips").child(trip.tripId).updateChildren(mapOf(
                            "status" to "ACCEPTED",
                            "driverId" to driverName,
                            "driverName" to driverName
                        ))
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingRequestCard(trip: Trip, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = Color(0xFFE3F2FD)) { Text(trip.vehicleType, color = Color(0xFF1976D2)) }
                Spacer(modifier = Modifier.weight(1f))
                Text("${trip.price} ETB", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("📍 Pickup: Arba Minch Center", fontSize = 14.sp)
            Text("🏁 To: ${trip.dropoffLocation.address}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            
            if(trip.notes.isNotEmpty()) {
                Box(modifier = Modifier.padding(top = 8.dp).background(Color(0xFFFFF9C4), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Text("📝 Note: ${trip.notes}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Accept Request") }
        }
    }
}

@Composable
fun ActiveJobPanel(trip: Trip, db: DatabaseReference) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, Color(0xFF2E7D32))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("ACTIVE TRIP", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
            Text("Destination: ${trip.dropoffLocation.address}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { 
                        val uri = "google.navigation:q=${trip.dropoffLocation.lat},${trip.dropoffLocation.lng}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps"))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) { Icon(Icons.Default.Navigation, null); Text(" Map") }
                
                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val newStatus = if(trip.status == TripStatus.ACCEPTED) TripStatus.IN_PROGRESS else TripStatus.COMPLETED
                        db.child("trips").child(trip.tripId).child("status").setValue(newStatus)
                        // If finishing, deduct commission logic would go here
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(trip.status == TripStatus.ACCEPTED) Color.Black else Color.Red)
                ) { Text(if(trip.status == TripStatus.ACCEPTED) "Start" else "Finish") }
            }
        }
    }
}
