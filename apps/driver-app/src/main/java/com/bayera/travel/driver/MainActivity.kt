package com.bayera.travel.driver

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
    val driverName = prefs.getString("name", "Partner") ?: "Partner"
    val driverId = prefs.getString("phone", "0900...") ?: "0900..."
    
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var balance by remember { mutableStateOf(0.0) }
    val db = FirebaseDatabase.getInstance().getReference()

    LaunchedEffect(Unit) {
        db.child("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>(); var myJob: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverName && t.status != TripStatus.COMPLETED) myJob = t
                        if (t.status == TripStatus.REQUESTED) list.add(t)
                    }
                }
                requests = list; currentJob = myJob
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("drivers").child(driverId).child("balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 0.0 }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Partner: $driverName | Balance: ${balance} ETB", fontWeight = FontWeight.Bold)
        if (currentJob != null) {
            ActiveJobCard(currentJob!!, db.child("trips").child(currentJob!!.tripId), driverId, db)
        } else {
            LazyColumn {
                items(requests) { trip ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("New Ride to: ${trip.dropoffLocation.address}")
                            Button(onClick = {
                                db.child("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName, "driverName" to driverName))
                            }) { Text("Accept Ride") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveJobCard(trip: Trip, tripRef: DatabaseReference, driverId: String, db: DatabaseReference) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("ACTIVE TRIP", fontWeight = FontWeight.Bold)
            Text("Destination: ${trip.dropoffLocation.address}")
            if(trip.notes.isNotEmpty()) Text("Note: ${trip.notes}", color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            if (trip.status == TripStatus.ACCEPTED) {
                Button(onClick = { tripRef.child("status").setValue(TripStatus.IN_PROGRESS) }, modifier = Modifier.fillMaxWidth()) { Text("START TRIP") }
            } else {
                Button(onClick = { 
                    tripRef.child("status").setValue(TripStatus.COMPLETED)
                    db.child("drivers").child(driverId).child("balance").runTransaction(object : Transaction.Handler {
                        override fun doTransaction(d: MutableData): Transaction.Result {
                            val cur = d.getValue(Double::class.java) ?: 0.0
                            d.value = cur - (trip.price * 0.10) // 10% commission deduction
                            return Transaction.success(d)
                        }
                        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
                    })
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("FINISH & DEDUCT 10%") }
            }
        }
    }
}
