package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val driverId = prefs.getString("phone", "0900000000") ?: "0900000000"
    val driverName = prefs.getString("name", "Partner") ?: "Partner"
    
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var balance by remember { mutableStateOf(0.0) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Rides, 1: Wallet

    val db = FirebaseDatabase.getInstance().getReference()

    // Listeners
    LaunchedEffect(Unit) {
        // 1. Listen for Trips
        db.child("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>()
                var myJob: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverId && t.status != TripStatus.COMPLETED) myJob = t
                        if (t.status == TripStatus.REQUESTED) list.add(t)
                    }
                }
                requests = list.reversed()
                currentJob = myJob
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // 2. Listen for Balance
        db.child("drivers").child(driverId).child("balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 0.0 }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Rides") }, selected = selectedTab == 0, onClick = { selectedTab = 0 })
                NavigationBarItem(icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Earnings") }, selected = selectedTab == 1, onClick = { selectedTab = 1 })
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().padding(16.dp)) {
            if (selectedTab == 1) {
                // WALLET UI
                Text("My Earnings", style = MaterialTheme.typography.headlineMedium)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Current Balance")
                        Text("${String.format("%.2f", balance)} ETB", style = MaterialTheme.typography.displaySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // RIDES UI
                Text("Hi, $driverName", style = MaterialTheme.typography.titleLarge)
                if (currentJob != null) {
                    ActiveJobCard(currentJob!!, driverId, db)
                } else {
                    LazyColumn {
                        items(requests) { trip ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("📍 Pickup: ${trip.pickupLocation.address}")
                                    Text("🏁 Destination: ${trip.dropoffLocation.address}")
                                    Text("💰 Fare: ${trip.price} ETB", fontWeight = FontWeight.Bold)
                                    Button(onClick = {
                                        db.child("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId, "driverName" to driverName))
                                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Accept Ride") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveJobCard(trip: Trip, driverId: String, db: DatabaseReference) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT TRIP (LOCKED)", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
            Text("To: ${trip.dropoffLocation.address}")
            if(trip.notes.isNotEmpty()) Text("Note: ${trip.notes}", color = Color.Red, fontWeight = FontWeight.Bold)
            Text("Payment: ${trip.paymentMethod}", fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trip.status == TripStatus.ACCEPTED) {
                Button(onClick = { db.child("trips").child(trip.tripId).child("status").setValue(TripStatus.IN_PROGRESS) }, modifier = Modifier.fillMaxWidth()) {
                    Text("START TRIP")
                }
            } else {
                Button(
                    onClick = { completeTrip(trip, driverId, db) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("FINISH & SETTLE BALANCE")
                }
            }
        }
    }
}

fun completeTrip(trip: Trip, driverId: String, db: DatabaseReference) {
    val amount = trip.price
    val isCash = trip.paymentMethod == "CASH"
    
    // 1. Set trip as completed
    db.child("trips").child(trip.tripId).child("status").setValue(TripStatus.COMPLETED)
    
    // 2. Transactional Balance Update
    val balanceRef = db.child("drivers").child(driverId).child("balance")
    balanceRef.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val current = currentData.getValue(Double::class.java) ?: 0.0
            // If Cash: Deduct 10% commission. If Wallet: Add 90% fare.
            val change = if (isCash) -(amount * 0.10) else (amount * 0.90)
            currentData.value = current + change
            return Transaction.success(currentData)
        }
        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
    })
}
