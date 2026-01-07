package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        setContent { MaterialTheme { CustomerMasterApp() } }
    }
}

@Composable
fun CustomerMasterApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "user_test") ?: "user_test"
    
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
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8EAF6)))
        if (activeTrip == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                BookingUI(userPhone, db, Modifier.align(Alignment.BottomCenter))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LockedTripUI(activeTrip!!, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
fun LockedTripUI(trip: Trip, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LOCKED: TRIP IN PROGRESS", color = Color.Red, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${trip.status}", style = MaterialTheme.typography.headlineSmall)
            Text("Destination: ${trip.dropoffLocation.address}")
        }
    }
}

@Composable
fun BookingUI(phone: String, db: DatabaseReference, modifier: Modifier = Modifier) {
    var dest by remember { mutableStateOf("") }
    Card(modifier = modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Request Ride", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = dest, onValueChange = { dest = it }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                val id = UUID.randomUUID().toString()
                val trip = Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = dest))
                db.child(id).setValue(trip)
            }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Confirm Request") }
        }
    }
}
