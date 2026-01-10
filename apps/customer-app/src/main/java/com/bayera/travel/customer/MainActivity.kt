package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1A237E))) { CustomerUrbanaApp() } }
    }
}

@Composable
fun CustomerUrbanaApp() {
    var screen by remember { mutableStateOf("home") }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeTrip = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.customerPhone == "user_bb" && it.status != TripStatus.COMPLETED }
                if (activeTrip != null) screen = "status"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dashboard Content
        if (screen == "home") {
            DashboardUI { screen = "map" }
        } else {
            // Map/Status Placeholder
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
                Text("Map Background Active", modifier = Modifier.align(Alignment.Center))
                if (screen == "map") BookingUI(db) { screen = "home" }
                if (screen == "status") activeTrip?.let { StatusUI(it, db) }
            }
        }

        // --- 🛡️ PERSISTENT SAFETY SHIELD (Urbana Differentiator) ---
        if (screen != "home") {
            FloatingActionButton(
                onClick = { /* Toggle Recording/Guardian Alert */ },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                containerColor = Color(0xFF009688),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Shield, contentDescription = "Safety", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRide: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Urbana Mobility", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("Hi, Ravi! Where to today?", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRide, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1A237E)); Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocalMall, null, tint = Color(0xFF00796B)); Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BookingUI(db: DatabaseReference, onBack: () -> Unit) {
    var showBreakdown by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Confirm Urbana Ride", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            // --- 🏷️ TRANSPARENT PRICING BREAKDOWN ---
            Column(modifier = Modifier.clickable { showBreakdown = !showBreakdown }.padding(vertical = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Fare", color = Color.Gray)
                    Text("110.00 ETB", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF2E7D32))
                }
                if (showBreakdown) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    PriceLine("Base Fare", "20.00")
                    PriceLine("Distance (4.2km)", "70.00")
                    PriceLine("Service Fee", "20.00")
                } else {
                    Text("Tap to see breakdown ⌵", fontSize = 10.sp, color = Color.Blue)
                }
            }

            Button(onClick = {
                val id = UUID.randomUUID().toString()
                db.child(id).setValue(Trip(tripId = id, customerPhone = "user_bb", price = 110.0, status = TripStatus.REQUESTED))
            }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Text("REQUEST RIDE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PriceLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp)
        Text("$value ETB", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusUI(trip: Trip, db: DatabaseReference) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF009688))
            Text("Driver Found!", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=12.dp))
            Text("Your Urbana trip is being prepared.")
            TextButton(onClick = { db.child(trip.tripId).removeValue() }) { Text("Cancel Request", color = Color.Red) }
        }
    }
}
