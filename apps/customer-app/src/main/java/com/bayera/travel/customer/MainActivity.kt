package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Manual Firebase Init using your provided project info
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel")
                .build()
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {}

        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    var screen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val userPhone = "user_bb"
    val db = try { FirebaseDatabase.getInstance().getReference("trips") } catch(e:Exception) { null }

    LaunchedEffect(Unit) {
        db?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == userPhone && it.status != TripStatus.COMPLETED }
                if (activeTrip != null) screen = "status"
                else if (screen == "status") screen = "home"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        bottomBar = {
            if (activeTrip == null) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = screen == "home", onClick = { screen = "home" })
                    NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = {})
                }
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            when (screen) {
                "home" -> DashboardUI { screen = "map" }
                "map" -> MapPlaceholderUI { screen = "home" }
                "status" -> activeTrip?.let { StatusLockedUI(it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Services", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRideClick, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(32.dp))
                    Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00), modifier = Modifier.size(32.dp))
                    Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MapPlaceholderUI(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Text("📍 Arba Minch Map View Ready", modifier = Modifier.align(Alignment.Center))
        IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) { Icon(Icons.Default.ArrowBack, null) }
        Button(
            onClick = { /* Handle booking */ },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Set Pickup Here", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun StatusLockedUI(trip: Trip) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(64.dp))
        Text("Driver Found!", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
        Text("Status: ${trip.status}")
    }
}
