package com.bayera.travel.driver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
// --- FIXED: ADDED ALL ICON IMPORTS ---
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.TripStatus
import com.bayera.travel.common.models.DeliveryOrder
import com.bayera.travel.common.models.DeliveryStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dashboard"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("dashboard") { DashboardScreen(navController) }
            }
        }
    }
}

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    
    // UI State: 0 = Taxi, 1 = Delivery
    var currentTab by remember { mutableIntStateOf(0) }
    
    var taxiRequests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var deliveryRequests by remember { mutableStateOf<List<DeliveryOrder>>(emptyList()) }

    // FETCH TAXI REQUESTS
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("trips")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Trip>()
                for (child in snapshot.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null && t.status == TripStatus.REQUESTED) list.add(t)
                }
                taxiRequests = list.reversed()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        
        // FETCH DELIVERY REQUESTS
        val deliveryDb = FirebaseDatabase.getInstance().getReference("deliveries")
        deliveryDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<DeliveryOrder>()
                for (child in snapshot.children) {
                    val d = child.getValue(DeliveryOrder::class.java)
                    if (d != null && d.status == DeliveryStatus.PENDING) list.add(d)
                }
                deliveryRequests = list.reversed()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hi, $driverName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { 
                        prefs.edit().clear().apply()
                        navController.navigate("login") { popUpTo(0) }
                    }) { Text("Logout", color = Color.Red) }
                }

                // TABS
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Button(
                        onClick = { currentTab = 0 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == 0) Color(0xFF2E7D32) else Color.Gray)
                    ) { Icon(Icons.Default.LocalTaxi, null); Spacer(modifier = Modifier.width(8.dp)); Text("TAXI") }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { currentTab = 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == 1) Color(0xFFE65100) else Color.Gray)
                    ) { Icon(Icons.Default.LocalShipping, null); Spacer(modifier = Modifier.width(8.dp)); Text("DELIVERY") }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // LIST
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (currentTab == 0) {
                        items(taxiRequests) { trip -> TaxiCard(trip, driverName) }
                    } else {
                        items(deliveryRequests) { order -> DeliveryCard(order, driverName) }
                    }
                }
            }
        }
    }
}

@Composable
fun TaxiCard(trip: Trip, driverId: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🚕 Taxi Request", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            Text("Customer: ${trip.customerId}")
            Text("From: ${trip.pickupLocation.address}")
            Text("To: ${trip.dropoffLocation.address}")
            Button(onClick = { 
                FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                    .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId))
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("ACCEPT RIDE") }
        }
    }
}

@Composable
fun DeliveryCard(order: DeliveryOrder, driverId: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📦 Delivery Request", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            Text("Restaurant: ${order.restaurantName}")
            Text("Customer: ${order.customerName}")
            Text("Earnings: ${order.deliveryFee} ETB")
            Button(onClick = { 
                FirebaseDatabase.getInstance().getReference("deliveries").child(order.orderId)
                    .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId))
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))) { Text("ACCEPT DELIVERY") }
        }
    }
}
