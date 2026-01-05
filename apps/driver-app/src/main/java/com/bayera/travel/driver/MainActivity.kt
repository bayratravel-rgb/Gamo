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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.TripStatus
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- GLOBAL CRASH HANDLER ---
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("error", stackTrace)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        val errorMsg = intent.getStringExtra("error")
        if (errorMsg != null) {
            setContent { ErrorScreen(errorMsg) }
            return
        }

        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val prefs = try { context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE) } catch(e:Exception){ null }
            
            // Logic: If name exists, go to dashboard. Else login.
            val startScreen = if (prefs?.getString("name", "").isNullOrEmpty()) "login" else "dashboard"

            NavHost(navController = navController, startDestination = startScreen) {
                composable("login") { LoginScreen(navController) }
                composable("dashboard") { DriverSuperDashboard(navController) }
                composable("wallet") { WalletScreen(navController) }
            }
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(64.dp))
            Text("App Crashed", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error Details:", fontWeight = FontWeight.Bold)
            Text(error, color = Color.Gray)
        }
    }
}

// --- RESTORED LOGIC BELOW ---

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
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Rides") },
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF2E7D32))
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Earnings") },
                    selected = false, onClick = { navController.navigate("wallet") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.Black)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(if (selectedTab == 0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $driverName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { prefs.edit().clear().apply(); navController.navigate("login") { popUpTo(0) } }) { Text("Logout", color = Color.Red) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedTab == 0) RideRequestsScreen(driverName) else Text("Delivery Coming Soon")
        }
    }
}

@Composable
fun RideRequestsScreen(driverName: String) {
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance().getReference("trips")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trips = mutableListOf<Trip>()
                var myJob: Trip? = null
                for (child in snapshot.children) {
                    try {
                        val trip = child.getValue(Trip::class.java)
                        if (trip != null) {
                            if (trip.driverId != null && trip.driverId!!.contains(driverName) && trip.status != TripStatus.COMPLETED && trip.status != TripStatus.CANCELLED) myJob = trip
                            if (trip.status == TripStatus.REQUESTED) trips.add(trip)
                        }
                    } catch (e: Exception) {}
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
fun ActiveJobCard(trip: Trip) {
    val context = LocalContext.current
    val db = FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverId = prefs.getString("phone", "000")?.filter { it.isDigit() } ?: "000"

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT TRIP", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            Text("From: ${trip.pickupLocation.address}")
            Text("To: ${trip.dropoffLocation.address}")
            Text(if (trip.paymentStatus == "PAID_WALLET") "PAID VIA WALLET" else "Collect: ${trip.price}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trip.status == TripStatus.ACCEPTED) {
                Button(onClick = { val uri = "google.navigation:q=${trip.pickupLocation.lat},${trip.pickupLocation.lng}"; startNav(context, uri) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)), modifier = Modifier.fillMaxWidth()) { Text("NAV TO PICKUP") }
                Button(onClick = { db.child("status").setValue(TripStatus.IN_PROGRESS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) { Text("START TRIP") }
            } else if (trip.status == TripStatus.IN_PROGRESS) {
                Button(onClick = { val uri = "google.navigation:q=${trip.dropoffLocation.lat},${trip.dropoffLocation.lng}"; startNav(context, uri) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.fillMaxWidth()) { Text("NAV TO DROP-OFF") }
                
                if (trip.paymentStatus == "PAID_WALLET") {
                    Button(onClick = { updateBalance(driverId, trip.price, false, context); db.child("status").setValue(TripStatus.COMPLETED) }, modifier = Modifier.fillMaxWidth()) { Text("COMPLETE (Paid)") }
                } else {
                    Button(onClick = { updateBalance(driverId, trip.price, true, context); db.child("status").setValue(TripStatus.COMPLETED) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth()) { Text("COLLECT CASH") }
                }
            }
        }
    }
}

fun updateBalance(driverId: String, amount: Double, isCash: Boolean, context: Context) {
    val driverWalletRef = FirebaseDatabase.getInstance().getReference("drivers").child(driverId).child("balance")
    driverWalletRef.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val currentBalance = currentData.getValue(Double::class.java) ?: 0.0
            val newBalance = if (isCash) currentBalance - (amount * 0.10) else currentBalance + (amount * 0.90)
            currentData.value = newBalance
            return Transaction.success(currentData)
        }
        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
    })
}

fun startNav(context: Context, uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    intent.setPackage("com.google.android.apps.maps")
    try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "Google Maps not found", Toast.LENGTH_SHORT).show() }
}

@Composable
fun RideCard(trip: Trip, driverId: String) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📍 ${trip.pickupLocation.address}")
            Button(onClick = { 
                FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                   .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverId))
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth()) { Text("ACCEPT RIDE") }
        }
    }
}
