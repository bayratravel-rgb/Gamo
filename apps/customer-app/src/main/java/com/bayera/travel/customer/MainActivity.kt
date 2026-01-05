package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CustomerAppMaster()
            }
        }
    }
}

@Composable
fun CustomerAppMaster() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "0900000000") ?: "0900000000"
    
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    // Real-time listener for active trips
    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                var found: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null && t.customerPhone == userPhone && t.status != TripStatus.COMPLETED) {
                        found = t
                    }
                }
                activeTrip = found
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold { p ->
        Box(modifier = Modifier.padding(p).fillMaxSize()) {
            // Background Map Placeholder
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE0E0E0))) {
                Text("Arba Minch Map View", modifier = Modifier.align(Alignment.Center))
            }

            if (activeTrip == null) {
                BookingPanel(userPhone)
            } else {
                TripInProgressPanel(activeTrip!!)
            }
        }
    }
}

@Composable
fun BookingPanel(phone: String) {
    var step by remember { mutableIntStateOf(1) }
    var dropoff by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("CASH") }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(if (step == 1) "Request a Ride" else "Finalize Trip", style = MaterialTheme.typography.headlineSmall)
            
            if (step == 1) {
                OutlinedTextField(value = dropoff, onValueChange = { dropoff = it }, label = { Text("Where to?") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
            } else {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note for Accuracy (e.g. Near Bank)") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Payment:")
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = payMethod == "CASH", onClick = { payMethod = "CASH" }, label = { Text("Cash") })
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(selected = payMethod == "WALLET", onClick = { payMethod = "WALLET" }, label = { Text("Wallet") })
                }

                Button(
                    onClick = {
                        val id = UUID.randomUUID().toString()
                        val trip = Trip(
                            tripId = id,
                            customerPhone = phone,
                            pickupLocation = Location(6.0, 37.5, "My Current Location"),
                            dropoffLocation = Location(6.01, 37.51, dropoff),
                            price = 150.0,
                            paymentMethod = payMethod,
                            notes = note
                        )
                        db.child(id).setValue(trip)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) { Text("Confirm 150 ETB Request") }
            }
        }
    }
}

@Composable
fun TripInProgressPanel(trip: Trip) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (trip.status) {
                TripStatus.REQUESTED -> {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                    Text("Finding your driver...", modifier = Modifier.padding(top = 8.dp))
                }
                TripStatus.ACCEPTED -> {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
                    Text("Driver ${trip.driverName ?: "Partner"} is coming!", fontWeight = FontWeight.Bold)
                    Text("Note sent: ${trip.notes}", style = MaterialTheme.typography.bodySmall)
                }
                TripStatus.IN_PROGRESS -> {
                    Text("Trip in Progress 🚕", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text("Destination: ${trip.dropoffLocation.address}")
                }
                else -> {}
            }
            
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Price: ${trip.price} ETB")
                Text("Payment: ${trip.paymentMethod}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
