package com.bayera.travel.driver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fastfood
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
import com.bayera.travel.common.models.Delivery
import com.bayera.travel.common.models.DeliveryStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_dashboard"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("super_dashboard") { DriverSuperDashboard(navController) }
            }
        }
    }
}

@Composable
fun DriverSuperDashboard(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Partner") ?: "Partner"
    
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Rides") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    label = { Text("Delivery") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE65100))
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(if (selectedTab == 0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $driverName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    prefs.edit().clear().apply()
                    navController.navigate("login") { popUpTo(0) }
                }) { Text("Logout", color = Color.Red) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) RideRequestsScreen(driverName) else DeliveryRequestsScreen(driverName)
        }
    }
}

// --- DELIVERY TAB LOGIC ---
@Composable
fun DeliveryRequestsScreen(driverName: String) {
    var orders by remember { mutableStateOf<List<Delivery>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("deliveries")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Delivery>()
                for (child in snapshot.children) {
                    try {
                        val order = child.getValue(Delivery::class.java)
                        if (order != null && order.status == DeliveryStatus.PENDING) {
                            list.add(order)
                        }
                    } catch (e: Exception) {}
                }
                orders = list.reversed()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Text("Incoming Food Orders", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))

    if (orders.isEmpty()) {
        Text("No orders nearby...", color = Color.Gray)
    } else {
        LazyColumn {
            items(orders) { order ->
                DeliveryOrderCard(order, driverName)
            }
        }
    }
}

@Composable
fun DeliveryOrderCard(order: Delivery, driverName: String) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Icon(Icons.Default.Fastfood, null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Text(order.restaurantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Client: ${order.customerName} (${order.customerPhone})")
            Text("Order: ${order.items}")
            Text("Earnings: ${order.price} ETB", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { 
                    FirebaseDatabase.getInstance().getReference("deliveries").child(order.id)
                        .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName))
                    Toast.makeText(context, "Delivery Accepted!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("ACCEPT DELIVERY") }
        }
    }
}

// --- RIDE LOGIC (Minified to save space, assuming it's the same as before) ---
@Composable
fun RideRequestsScreen(driverName: String) {
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                var myJob: Trip? = null
                for (child in snapshot.children) {
                    val trip = child.getValue(Trip::class.java)
                    if (trip != null) {
                        if (trip.driverId != null && trip.driverId!!.contains(driverName) && trip.status != TripStatus.COMPLETED) myJob = trip
                        if (trip.status == TripStatus.REQUESTED) trips.add(trip)
                    }
                }
                activeTrips = trips.reversed()
                currentJob = myJob
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    if (currentJob != null) {
        ActiveJobCard(currentJob!!)
    } else {
        Text("Incoming Rides", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        LazyColumn { items(activeTrips) { trip -> RideCard(trip, driverName) } }
    }
}

@Composable
fun RideCard(trip: Trip, driverId: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📍 ${trip.pickupLocation.address}")
            Text("💰 ${trip.price} ETB")
            Button(onClick = { 
                FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                   .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId))
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) { Text("ACCEPT RIDE") }
        }
    }
}

@Composable
fun ActiveJobCard(trip: Trip) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT TRIP", fontWeight = FontWeight.Bold)
            Text("To: ${trip.dropoffLocation.address}")
        }
    }
}
