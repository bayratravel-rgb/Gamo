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
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val opt = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel").build()
                FirebaseApp.initializeApp(this, opt)
            }
        } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerUrbanaApp() } }
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
                else if (screen == "status") screen = "home"
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (screen == "home") {
            DashboardUI { screen = "map" }
        } else {
            // Map Background
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
                if (screen == "map") {
                    BookingUI(db) { screen = "home" }
                }
                if (screen == "status") {
                    activeTrip?.let { StatusUI(it, db) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRide: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Urbana Mobility", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("Hi, Ravi!", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRide, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
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
fun BoxScope.BookingUI(db: DatabaseReference, onBack: () -> Unit) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Total Fare: 110.00 ETB", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF2E7D32))
            Spacer(modifier = Modifier.height(16.dp))
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
fun BoxScope.StatusUI(trip: Trip, db: DatabaseReference) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF009688))
            Text("Finding Driver...", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=12.dp))
            TextButton(onClick = { db.child(trip.tripId).removeValue() }) { Text("Cancel", color = Color.Red) }
        }
    }
}
