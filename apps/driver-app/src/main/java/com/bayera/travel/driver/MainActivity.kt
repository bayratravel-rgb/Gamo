package com.bayera.travel.driver

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { DriverMaster() } }
    }
}

@Composable
fun DriverMaster() {
    val driverId = "0900000000"
    val driverName = "Partner_Arba"
    val db = FirebaseDatabase.getInstance().getReference()
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var balance by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        db.child("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>(); var myJob: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverId && t.status != TripStatus.COMPLETED) myJob = t
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
        Text("Partner: $driverName", fontWeight = FontWeight.Bold)
        Text("Balance: ${String.format("%.2f", balance)} ETB", color = Color(0xFF2E7D32))
        
        if (currentJob != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE TRIP", fontWeight = FontWeight.Bold)
                    Text("To: ${currentJob!!.dropoffLocation.address}")
                    Button(onClick = {
                        if (currentJob!!.status == TripStatus.ACCEPTED) {
                            db.child("trips").child(currentJob!!.tripId).child("status").setValue(TripStatus.IN_PROGRESS)
                        } else {
                            settleTrip(currentJob!!, driverId, db)
                        }
                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { 
                        Text(if(currentJob!!.status == TripStatus.ACCEPTED) "START TRIP" else "FINISH & SETTLE")
                    }
                }
            }
        } else {
            LazyColumn {
                items(requests) { trip ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ride to: ${trip.dropoffLocation.address}")
                            Button(onClick = {
                                db.child("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId, "driverName" to driverName))
                            }) { Text("Accept") }
                        }
                    }
                }
            }
        }
    }
}

fun settleTrip(trip: Trip, driverId: String, db: DatabaseReference) {
    db.child("trips").child(trip.tripId).child("status").setValue(TripStatus.COMPLETED)
    db.child("drivers").child(driverId).child("balance").runTransaction(object : Transaction.Handler {
        override fun doTransaction(d: MutableData): Transaction.Result {
            val cur = d.getValue(Double::class.java) ?: 0.0
            val change = if(trip.paymentMethod == "CASH") -(trip.price * 0.10) else (trip.price * 0.90)
            d.value = cur + change
            return Transaction.success(d)
        }
        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
    })
}
