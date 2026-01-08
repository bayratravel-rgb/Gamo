package com.bayera.travel.driver

import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { DriverCoreUI() } }
    }
}

@Composable
fun DriverCoreUI() {
    val driverName = "sffu the driver"
    val db = FirebaseDatabase.getInstance().getReference()
    var currentJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        db.child("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>(); var myJob: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverName && t.status != TripStatus.COMPLETED) myJob = t
                        if (t.status == TripStatus.REQUESTED) list.add(t)
                    }
                }
                requests = list; currentJob = myJob
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Hi, $driverName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f)); Text("Logout", color = Color.Red)
        }
        
        if (currentJob != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CURRENT TRIP", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Text("To: ${currentJob!!.dropoffLocation.address}")
                    Text("💰 Collect: ${currentJob!!.price} ETB", fontWeight = FontWeight.Bold)
                    
                    Button(onClick = { 
                        val uri = "google.navigation:q=${currentJob!!.dropoffLocation.lat},${currentJob!!.dropoffLocation.lng}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps"))
                    }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("NAVIGATE") }
                    
                    Button(onClick = {
                        val next = if(currentJob!!.status == TripStatus.ACCEPTED) TripStatus.IN_PROGRESS else TripStatus.COMPLETED
                        db.child("trips").child(currentJob!!.tripId).child("status").setValue(next)
                    }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { 
                        Text(if(currentJob!!.status == TripStatus.ACCEPTED) "START TRIP" else "CASH COLLECTED")
                    }
                }
            }
        } else {
            Text("Incoming Rides", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))
            LazyColumn {
                items(requests) { trip ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("👤 Customer Request", fontWeight = FontWeight.Bold)
                            Text("Price: ${trip.price} ETB")
                            Row {
                                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("DECLINE", color = Color.Red) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    db.child("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName, "driverName" to driverName))
                                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("ACCEPT") }
                            }
                        }
                    }
                }
            }
        }
    }
}
