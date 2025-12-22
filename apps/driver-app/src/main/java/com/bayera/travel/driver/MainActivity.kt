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
import androidx.compose.material.icons.filled.LocalShipping // Delivery Icon
import androidx.compose.material.icons.filled.LocalTaxi // Taxi Icon
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
                composable("dashboard") { SuperAppDashboard(navController) }
            }
        }
    }
}

@Composable
fun SuperAppDashboard(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Taxi, 1=Delivery

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocalTaxi, null) },
                    label = { Text("Rides") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocalShipping, null) },
                    label = { Text("Deliveries") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFE65100))
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                // TAXI SCREEN
                TaxiScreen(navController)
            } else {
                // DELIVERY SCREEN (Placeholder for now, uses same logic)
                DeliveryScreen()
            }
        }
    }
}

@Composable
fun TaxiScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var currentJob by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")
        tripsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                var myJob: Trip? = null
                for (child in snapshot.children) {
                    try {
                        val trip = child.getValue(Trip::class.java)
                        if (trip != null) {
                            if (trip.driverId != null && trip.driverId!!.contains(driverName)) {
                                if (trip.status != TripStatus.COMPLETED && trip.status != TripStatus.CANCELLED) myJob = trip
                            }
                            if (trip.status == TripStatus.REQUESTED) trips.add(trip)
                        }
                    } catch (e: Exception) {}
                }
                activeTrips = trips.reversed()
                currentJob = myJob
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $driverName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    prefs.edit().clear().apply()
                    navController.navigate("login") { popUpTo(0) }
                }) { Text("Logout", color = Color.Red) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (currentJob != null) {
                ActiveJobCard(currentJob!!)
            } else {
                Text("Incoming Rides", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (activeTrips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No rides...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                } else {
                    LazyColumn { items(activeTrips) { trip -> TripCard(trip, "$driverName") } }
                }
            }
        }
    }
}

@Composable
fun DeliveryScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF3E0)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Delivery Orders", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No food orders yet...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        }
    }
}

// ... (KEEPING ActiveJobCard AND TripCard FUNCTIONS EXACTLY AS THEY WERE)
// ... (I am truncating them here for brevity, but the file overwrite should include them.
//      Since you have the previous working code, I will assume you can append them or
//      I can provide the FULL file again if you prefer).

@Composable
fun ActiveJobCard(trip: Trip) {
    val context = LocalContext.current
    val db = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT TRIP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            Spacer(modifier = Modifier.height(8.dp))
            Text("👤 ${trip.customerId}"); Spacer(modifier = Modifier.height(4.dp))
            Text("🟢 From: ${trip.pickupLocation.address}"); Text("🔴 To: ${trip.dropoffLocation.address}")
            if (trip.pickupNotes.isNotEmpty()) Text("📝 Note: ${trip.pickupNotes}", color = Color.Red, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (trip.status == TripStatus.ACCEPTED) {
                Button(onClick = { val uri = "google.navigation:q=${trip.pickupLocation.lat},${trip.pickupLocation.lng}"; val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)); intent.setPackage("com.google.android.apps.maps"); try { context.startActivity(intent) } catch(e: Exception) {} }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Navigation, null); Spacer(modifier = Modifier.width(8.dp)); Text("NAVIGATE TO PICKUP") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { db.child("status").setValue(TripStatus.IN_PROGRESS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) { Text("START TRIP") }
            } else if (trip.status == TripStatus.IN_PROGRESS) {
                Button(onClick = { val uri = "google.navigation:q=${trip.dropoffLocation.lat},${trip.dropoffLocation.lng}"; val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)); intent.setPackage("com.google.android.apps.maps"); try { context.startActivity(intent) } catch(e: Exception) {} }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Navigation, null); Spacer(modifier = Modifier.width(8.dp)); Text("NAVIGATE TO DROP-OFF") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { db.child("status").setValue(TripStatus.COMPLETED) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) { Text("COMPLETE TRIP") }
            }
        }
    }
}

@Composable
fun TripCard(trip: Trip, driverId: String) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👤 ${trip.customerId}", fontWeight = FontWeight.Bold)
            Text("📍 ${trip.pickupLocation.address}")
            if (trip.pickupNotes.isNotEmpty()) Text("📝 ${trip.pickupNotes}", color = Color.Gray)
            Text("💰 ${trip.price} ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) { Text("ACCEPT RIDE") }
        }
    }
}
