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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.io.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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

        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent { MaterialTheme { CustomerMaster() } }
    }
}

@Composable
fun CustomerMaster() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "user_${UUID.randomUUID().toString().take(4)}") ?: ""
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                var found: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null && t.customerPhone == userPhone && t.status != TripStatus.COMPLETED) found = t
                }
                activeTrip = found
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5))) {
            Text("📍 Arba Minch Map active", modifier = Modifier.align(Alignment.Center))
        }
        if (activeTrip == null) {
            Box(modifier = Modifier.fillMaxSize()) { BookingUI(userPhone, db, Modifier.align(Alignment.BottomCenter)) }
        } else {
            Box(modifier = Modifier.fillMaxSize()) { LockedTripUI(activeTrip!!, Modifier.align(Alignment.BottomCenter)) }
        }
    }
}

@Composable
fun BookingUI(phone: String, db: DatabaseReference, modifier: Modifier) {
    var dest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }

    Card(modifier = modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(if(step==1) "Where to?" else "Note to Driver", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if(step==1) {
                OutlinedTextField(value = dest, onValueChange = { dest = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Next") }
            } else {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Add Notes (Accuracy)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    val id = UUID.randomUUID().toString()
                    db.child(id).setValue(Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = dest), notes = note, price = 150.0))
                }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Request Ride") }
            }
        }
    }
}

@Composable
fun LockedTripUI(trip: Trip, modifier: Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = if(trip.status == TripStatus.IN_PROGRESS) Color(0xFFFFEBEE) else Color.White)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if(trip.status == TripStatus.IN_PROGRESS) {
                Text("LOCKED: TRIP IN PROGRESS", color = Color.Red, fontWeight = FontWeight.ExtraBold)
            }
            Text("Status: ${trip.status}", style = MaterialTheme.typography.headlineSmall)
            Text("Driver: ${trip.driverName ?: "Searching..."}")
            Text("To: ${trip.dropoffLocation.address}")
            if(trip.notes.isNotEmpty()) Text("Note: ${trip.notes}", color = Color.Gray)
        }
    }
}

@Composable
fun ErrorScreen(err: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        Text(err, color = Color.Yellow, modifier = Modifier.padding(16.dp))
    }
}
