package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { DriverMaster() } }
    }
}

@Composable
fun DriverMaster() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val reqList = mutableListOf<Trip>()
                var myJob: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverName && t.status != TripStatus.COMPLETED) myJob = t
                        if (t.status == TripStatus.REQUESTED) reqList.add(t)
                    }
                }
                requests = reqList
                currentJob = myJob
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().padding(16.dp)) {
            Text("Partner: $driverName", style = MaterialTheme.typography.titleLarge)
            if (currentJob != null) {
                ActiveJobUI(currentJob!!, db)
            } else {
                Text("Ride Requests", modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn {
                    items(requests) { trip ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Destination: ${trip.dropoffLocation.address}")
                                if(trip.notes.isNotEmpty()) Text("Note: ${trip.notes}", color = Color.Red)
                                Button(onClick = {
                                    db.child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName, "driverName" to driverName))
                                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Accept Ride") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveJobUI(trip: Trip, db: DatabaseReference) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ACTIVE TRIP", fontWeight = FontWeight.Bold)
            Text("To: ${trip.dropoffLocation.address}")
            Text("Payment: ${trip.paymentMethod}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (trip.status == TripStatus.ACCEPTED) {
                Button(onClick = { db.child(trip.tripId).child("status").setValue(TripStatus.IN_PROGRESS) }, modifier = Modifier.fillMaxWidth()) { Text("Start Trip") }
            } else {
                Button(onClick = { db.child(trip.tripId).child("status").setValue(TripStatus.COMPLETED) }, modifier = Modifier.fillMaxWidth()) { Text("Finish & Collect Fare") }
            }
        }
    }
}
