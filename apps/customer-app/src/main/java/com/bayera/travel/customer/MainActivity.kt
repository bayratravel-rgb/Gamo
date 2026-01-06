package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerMaster() } }
    }
}

@Composable
fun CustomerMaster() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "0900000000") ?: "0900000000"
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
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
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE)))
        if (activeTrip == null) BookingUI(userPhone) else TripStatusUI(activeTrip!!)
    }
}

@Composable
fun BookingUI(phone: String) {
    var dest by remember { mutableStateOf("") }
    Card(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Where to?", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = dest, onValueChange = { dest = it }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val id = UUID.randomUUID().toString()
                val trip = Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = dest))
                FirebaseDatabase.getInstance().getReference("trips").child(id).setValue(trip)
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Request Ride") }
        }
    }
}

@Composable
fun TripStatusUI(trip: Trip) {
    Card(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Status: ${trip.status}", style = MaterialTheme.typography.headlineSmall)
            Text("Driver: ${trip.driverName ?: "Searching..."}")
            Text("Destination: ${trip.dropoffLocation.address}")
        }
    }
}
