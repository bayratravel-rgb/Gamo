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
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.io.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- GLOBAL CRASH HANDLER ---
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("error", sw.toString()); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent); android.os.Process.killProcess(android.os.Process.myPid())
        }

        if (intent.getStringExtra("error") != null) {
            setContent { ErrorScreen(intent.getStringExtra("error")!!) }; return
        }

        // --- FORCED INITIALIZATION ---
        try {
            FirebaseApp.initializeApp(applicationContext)
        } catch (e: Exception) {
            // If already initialized, ignore
        }

        setContent { MaterialTheme { CustomerMaster() } }
    }
}

@Composable
fun CustomerMaster() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "user_test") ?: "user_test"
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    
    // We delay database access until we are sure we are in a safe state
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseDatabase.getInstance().getReference("trips")
            db.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    var found: Trip? = null
                    for (child in s.children) {
                        val t = child.getValue(Trip::class.java)
                        if (t != null && t.customerPhone == userPhone && t.status != TripStatus.COMPLETED) {
                            found = t
                        }
                    }
                    activeTrip = found
                }
                override fun onCancelled(e: DatabaseError) {}
            })
        } catch (e: Exception) {
            // Handle initialization errors
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8EAF6))) {
            Text("📍 Map View Ready", modifier = Modifier.align(Alignment.Center))
        }
        if (activeTrip == null) {
            Box(modifier = Modifier.fillMaxSize()) { BookingUI(userPhone, Modifier.align(Alignment.BottomCenter)) }
        } else {
            Box(modifier = Modifier.fillMaxSize()) { LockedTripUI(activeTrip!!, Modifier.align(Alignment.BottomCenter)) }
        }
    }
}

@Composable
fun BookingUI(phone: String, modifier: Modifier) {
    var dest by remember { mutableStateOf("") }
    Card(modifier = modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Where to?", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = dest, onValueChange = { dest = it }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val id = UUID.randomUUID().toString()
                FirebaseDatabase.getInstance().getReference("trips").child(id)
                    .setValue(Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = dest)))
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Request Ride") }
        }
    }
}

@Composable
fun LockedTripUI(trip: Trip, modifier: Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TRIP IN PROGRESS", color = Color.Red, fontWeight = FontWeight.Bold)
            Text("Status: ${trip.status}")
            Text("To: ${trip.dropoffLocation.address}")
        }
    }
}

@Composable
fun ErrorScreen(err: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        Text("CRASH LOG", color = Color.Red, style = MaterialTheme.typography.headlineSmall)
        Text(err, color = Color.Yellow)
    }
}
