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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
            MaterialTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
                val startScreen = if (prefs.getString("phone", "").isNullOrEmpty()) "login" else "dashboard"

                NavHost(navController = navController, startDestination = startScreen) {
                    composable("login") { LoginUI(navController) }
                    composable("dashboard") { DriverDashboardUI(navController) }
                    composable("wallet") { WalletUI(navController) }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("BUILD/RUNTIME ERROR", color = Color.Red, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(error, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun LoginUI(navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Driver Login", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("name", name).putString("phone", phone).apply()
                navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
            }
        }) { Text("Go Online") }
    }
}

@Composable
fun DriverDashboardUI(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    val driverName = prefs.getString("name", "Driver") ?: "Driver"
    var activeTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().getReference("trips")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val list = mutableListOf<Trip>()
                    for (child in s.children) {
                        val trip = child.getValue(Trip::class.java)
                        if (trip?.status == TripStatus.REQUESTED) list.add(trip)
                    }
                    activeTrips = list.reversed()
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Rides") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Wallet") }, selected = false, onClick = { navController.navigate("wallet") })
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text("Welcome, $driverName", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(activeTrips) { trip ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pickup: ${trip.pickupLocation.address}")
                            Button(onClick = { 
                                FirebaseDatabase.getInstance().getReference("trips").child(trip.tripId)
                                    .updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName))
                            }) { Text("Accept") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletUI(navController: NavController) {
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
        Text("Earnings", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Current Balance")
                Text("${balance} ETB", style = MaterialTheme.typography.displayMedium, color = Color(0xFF2E7D32))
            }
        }
    }
}
