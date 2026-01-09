package com.bayera.travel.driver

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val opt = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:656f004fce2ecda85af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel").build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, opt)
        } catch (e: Exception) {}
        setContent { MaterialTheme { DriverDashboard() } }
    }
}

@Composable
fun DriverDashboard() {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                requests = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .filter { it.status == TripStatus.REQUESTED }.reversed()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bayera Partner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("500.0 ETB", color = Color.White, style = MaterialTheme.typography.displayMedium)
            }
        }
        Text("Incoming Requests", fontWeight = FontWeight.Bold)
        LazyColumn {
            items(requests) { trip ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("New Ride • ${trip.price} ETB", fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            db.child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverName" to "Arba Partner"))
                        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Accept") }
                    }
                }
            }
        }
    }
}
