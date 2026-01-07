package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        setContent { MaterialTheme { DriverMasterUI() } }
    }
}

@Composable
fun DriverMasterUI() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Driver_01") ?: "Driver_01"
    
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>()
                var myJob: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverName && t.status != TripStatus.COMPLETED) myJob = t
                        if (t.status == TripStatus.REQUESTED) list.add(t)
                    }
                }
                requests = list
                currentJob = myJob
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Partner: $driverName", style = MaterialTheme.typography.headlineSmall)
        
        if (currentJob != null) {
            // JOB HANDLING PANEL
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE JOB", fontWeight = FontWeight.Bold)
                    Text("Destination: ${currentJob!!.dropoffLocation.address}")
                    Text("Note: ${currentJob!!.notes}", color = Color.Red)
                    Text("Payment: ${currentJob!!.paymentMethod}", fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (currentJob!!.status == TripStatus.ACCEPTED) {
                        Button(onClick = { db.child(currentJob!!.tripId).child("status").setValue(TripStatus.IN_PROGRESS) }, modifier = Modifier.fillMaxWidth()) {
                            Text("START TRIP (Locks Customer Screen)")
                        }
                    } else if (currentJob!!.status == TripStatus.IN_PROGRESS) {
                        Button(onClick = { db.child(currentJob!!.tripId).child("status").setValue(TripStatus.COMPLETED) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                            Text("FINISH TRIP (Unlock)")
                        }
                    }
                }
            }
        } else {
            // REQUEST LIST
            LazyColumn {
                items(requests) { trip ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("To: ${trip.dropoffLocation.address}")
                            Button(onClick = {
                                db.child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName, "driverName" to driverName))
                            }) { Text("Accept Ride") }
                        }
                    }
                }
            }
        }
    }
}
