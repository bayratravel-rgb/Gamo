package com.bayera.travel.driver

import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { DriverApp() } }
    }
}

@Composable
fun DriverApp() {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    val context = LocalContext.current
    var activeJob by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                activeJob = s.children.mapNotNull { it.getValue(Trip::class.java) }
                    .firstOrNull { it.driverId == "Partner_Arba" && it.status != TripStatus.COMPLETED }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Partner Dashboard", style = MaterialTheme.typography.headlineMedium)
        
        activeJob?.let { job ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE TRIP", color = Color.Red)
                    Text("Destination: ${job.dropoffLocation.address}")
                    
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Button(onClick = {
                            val uri = "google.navigation:q=${job.dropoffLocation.lat},${job.dropoffLocation.lng}"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps"))
                        }, modifier = Modifier.weight(1f)) { Text("NAVIGATE") }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(onClick = {
                            val nextStatus = if(job.status == TripStatus.ACCEPTED) TripStatus.IN_PROGRESS else TripStatus.COMPLETED
                            db.child(job.tripId).child("status").setValue(nextStatus)
                        }, modifier = Modifier.weight(1f)) { 
                            Text(if(job.status == TripStatus.ACCEPTED) "START" else "FINISH") 
                        }
                    }
                }
            }
        } ?: Text("Waiting for requests...")
    }
}
