package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
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
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerMasterApp() } }
    }
}

@Composable
fun CustomerMasterApp() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "user_test") ?: "user_test"
    
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    LaunchedEffect(Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                var found: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    // LOCK LOGIC: Find any trip that is NOT completed
                    if (t != null && t.customerPhone == userPhone && t.status != TripStatus.COMPLETED) {
                        found = t
                    }
                }
                activeTrip = found
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full screen Map area
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8EAF6))) {
            Text("📍 Arba Minch Live Map", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        }

        // NAVIGATION LOCK: Switch UI based on state
        if (activeTrip == null) {
            BookingUI(userPhone, db)
        } else {
            // This screen is "Locked" - No way to go back to BookingUI
            LockedTripUI(activeTrip!!)
        }
    }
}

@Composable
fun LockedTripUI(trip: Trip) {
    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (trip.status) {
                    TripStatus.REQUESTED -> {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Searching for a Driver...", fontWeight = FontWeight.Bold)
                    }
                    TripStatus.ACCEPTED -> {
                        Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2), modifier = Modifier.size(48.dp))
                        Text("Driver ${trip.driverName} is coming!", style = MaterialTheme.typography.titleLarge)
                        Text("Note to driver: ${trip.notes}", color = Color.Gray)
                    }
                    TripStatus.IN_PROGRESS -> {
                        // THE LOCKED PROGRESS SCREEN
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(48.dp))
                        Text("EN ROUTE", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFD32F2F), fontWeight = FontWeight.ExtraBold)
                        Text("Heading to: ${trip.dropoffLocation.address}", style = MaterialTheme.typography.bodyLarge)
                        
                        Box(modifier = Modifier.padding(12.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Text("⚠️ Navigation Locked: Trip is currently active.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    else -> {}
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Fare", style = MaterialTheme.typography.labelSmall)
                        Text("${trip.price} ETB", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Payment Mode", style = MaterialTheme.typography.labelSmall)
                        Text(trip.paymentMethod, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@Composable
fun BookingUI(phone: String, db: DatabaseReference) {
    var dest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("CASH") }
    var step by remember { mutableIntStateOf(1) }

    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (step == 1) {
                Text("Where to?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = dest, onValueChange = { dest = it }, placeholder = { Text("Search Arba Minch...") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Set Destination") }
            } else {
                Text("Trip Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (Blue Gate, Hotel, etc.)") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { payMethod = "CASH" }, colors = ButtonDefaults.buttonColors(containerColor = if(payMethod=="CASH") Color.Black else Color.Gray)) { Text("Cash") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { payMethod = "WALLET" }, colors = ButtonDefaults.buttonColors(containerColor = if(payMethod=="WALLET") Color.Black else Color.Gray)) { Text("Wallet") }
                }
                Button(onClick = {
                    val id = UUID.randomUUID().toString()
                    val trip = Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = dest), notes = note, paymentMethod = payMethod, price = 150.0)
                    db.child(id).setValue(trip)
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Confirm Request") }
            }
        }
    }
}
