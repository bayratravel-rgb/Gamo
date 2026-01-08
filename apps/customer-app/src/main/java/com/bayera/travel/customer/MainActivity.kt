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
import androidx.compose.ui.platform.LocalContext
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
    val userPhone = "user_bb" // Based on your screenshot "Hi, bb!"
    val db = FirebaseDatabase.getInstance().getReference("trips")

    // Real-time monitor for Trip Status (The "Lock")
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
                "home" -> SuperDashboard { currentScreen = "map" }
                "map" -> MapBookingScreen(userPhone) { currentScreen = "home" }
                "status" -> activeTrip?.let { TripStatusLockedUI(it) }
                else -> Text("Coming Soon")
            }
        }
    }
}

@Composable
fun SuperDashboard(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hi, bb!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Settings, null)
        }
        Text("Services", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ServiceTile("Ride", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Modifier.weight(1f)) { onRideClick() }
            ServiceTile("Shopping", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Modifier.weight(1f)) {}
        }
        Spacer(modifier = Modifier.height(16.dp))
        ServiceTile("Hotels & Resorts", Icons.Default.Hotel, Color(0xFFF3E5F5), Modifier.fillMaxWidth(), "Book your stay") {}
    }
}

@Composable
fun ServiceTile(title: String, icon: ImageVector, bg: Color, modifier: Modifier, sub: String? = null, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = Color(0xFF1976D2))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MapBookingScreen(phone: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Placeholder for the Arba Minch Map from your screenshot
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            Text("🗺️ Arba Minch Map View", modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
                Icon(Icons.Default.ArrowBack, null)
            }
        }
        
        // The Green Button from your screenshot
        Button(
            onClick = {
                val id = UUID.randomUUID().toString()
                FirebaseDatabase.getInstance().getReference("trips").child(id)
                    .setValue(Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = "Arba Minch University"), status = TripStatus.REQUESTED))
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Set Pickup Here", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TripStatusLockedUI(trip: Trip) {
    // This is the "Driver Found / PAID" screen from your screenshot
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5))) {
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (trip.status == TripStatus.REQUESTED) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
                Text("Finding Driver...", modifier = Modifier.padding(top = 16.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                    Text(" Driver Found!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                Text("Driver: ${trip.driverName ?: "Arba Partner"}", modifier = Modifier.padding(top = 8.dp))
                
                Box(modifier = Modifier.padding(top = 12.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("✅ PAID", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                
                if (trip.status == TripStatus.IN_PROGRESS) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("EN ROUTE", fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 24.sp)
                    Text("Navigation Locked", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
