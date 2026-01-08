package com.bayera.travel.customer

import android.content.*
import android.os.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun CustomerSuperApp() {
    var currentScreen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val userPhone = "user_bb" 
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                var found: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null && t.customerPhone == userPhone && t.status != TripStatus.COMPLETED) found = t
                }
                activeTrip = found
                if (activeTrip != null) currentScreen = "status"
                else if (currentScreen == "status") currentScreen = "home"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        bottomBar = {
            if (activeTrip == null) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = currentScreen == "home", onClick = { currentScreen = "home" })
                    NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = currentScreen == "activity", onClick = { currentScreen = "activity" })
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = currentScreen == "account", onClick = { currentScreen = "account" })
                }
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            when (currentScreen) {
                "home" -> SuperDashboardUI { currentScreen = "map" }
                "map" -> MapBookingUI(userPhone) { currentScreen = "home" }
                "status" -> activeTrip?.let { TripStatusLockedUI(it) }
            }
        }
    }
}

@Composable
fun SuperDashboardUI(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Hi, bb!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Services", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ServiceCard("Ride", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Modifier.weight(1f), onRideClick)
            ServiceCard("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Modifier.weight(1f)) {}
        }
        Spacer(modifier = Modifier.height(16.dp))
        ServiceCard("Hotels & Resorts", Icons.Default.Hotel, Color(0xFFF3E5F5), Modifier.fillMaxWidth()) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCard(title: String, icon: ImageVector, bg: Color, modifier: Modifier, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = modifier.height(120.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(32.dp), tint = Color(0xFF1976D2))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MapBookingUI(phone: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
            Text("📍 Arba Minch Map View", modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        }
        Button(
            onClick = {
                val id = UUID.randomUUID().toString()
                FirebaseDatabase.getInstance().getReference("trips").child(id)
                    .setValue(Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = "Arba Minch"), status = TripStatus.REQUESTED))
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Set Pickup Here", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TripStatusLockedUI(trip: Trip) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if(trip.status == TripStatus.IN_PROGRESS) "EN ROUTE" else "DRIVER FOUND", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            Text("Driver: ${trip.driverName ?: "Partner"}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.background(Color(0xFFE8F5E9)).padding(8.dp)) { Text("✅ PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold) }
        }
    }
}
