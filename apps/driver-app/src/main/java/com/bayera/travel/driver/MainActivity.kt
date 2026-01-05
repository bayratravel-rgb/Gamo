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
import com.google.firebase.database.*
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
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("error", sw.toString())
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
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
            val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
            
            // Check if logged in via 'name' or 'phone'
            val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "dashboard"

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
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(16.dp).verticalScroll(rememberScrollState())) {
            Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
            Text("RUNTIME CRASH", color = Color.Red, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Technical Details for Debugging:", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(error, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
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
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Rides") },
                    selected = selectedTab == 0, onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Wallet") },
                    selected = false, onClick = { navController.navigate("wallet") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF0F2F5)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $driverName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { 
                    prefs.edit().clear().apply()
                    navController.navigate("login") { popUpTo(0) }
                }) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) }
            }
            RideRequestsScreen(driverName)
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
                    val trip = child.getValue(Trip::class.java)
                    if (trip != null) {
                        if (trip.driverId == driverName && trip.status != TripStatus.COMPLETED) myJob = trip
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
        Text("Requested Rides", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn {
            items(activeTrips) { trip ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📍 From: ${trip.pickupLocation.address}")
                        Text("🏁 To: ${trip.dropoffLocation.address}")
                        Button(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            onClick = { 
                                FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                                    .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName))
                            }
                        ) { Text("Accept Ride") }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveJobCard(trip: Trip) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT ACTIVE TRIP", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Destination: ${trip.dropoffLocation.address}")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val uri = "google.navigation:q=${trip.dropoffLocation.lat},${trip.dropoffLocation.lng}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply { setPackage("com.google.android.apps.maps") }
                    try { context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "Maps not installed", Toast.LENGTH_SHORT).show() }
                }
            ) { Text("Start Navigation") }
        }
    }
}

@Composable
fun WalletScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val phone = prefs.getString("phone", "000") ?: "000"
    var balance by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("drivers").child(phone).child("balance")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 0.0 }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
        Text("Earnings Dashboard", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Total Balance", style = MaterialTheme.typography.labelLarge)
                Text("${balance} ETB", style = MaterialTheme.typography.displayMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        }
    }
}
