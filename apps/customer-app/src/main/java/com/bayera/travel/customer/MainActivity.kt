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
        setContent { MaterialTheme { CustomerMaster() } }
    }
}

@Composable
fun CustomerMaster() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userPhone = prefs.getString("phone", "0900000000") ?: "0900000000"
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

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

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
            Text("Arba Minch Map View", modifier = Modifier.align(Alignment.Center))
        }
        if (activeTrip == null) BookingPanel(userPhone) else TripStatusPanel(activeTrip!!)
    }
}

@Composable
fun BookingPanel(phone: String) {
    var step by remember { mutableIntStateOf(1) }
    var destination by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("CASH") }

    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (step == 1) "Where to?" else "Trip Details", style = MaterialTheme.typography.titleLarge)
            if (step == 1) {
                OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Continue") }
            } else {
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Notes for driver") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { payMethod = "CASH" }, colors = ButtonDefaults.buttonColors(containerColor = if(payMethod == "CASH") Color.Black else Color.Gray)) { Text("Cash") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { payMethod = "WALLET" }, colors = ButtonDefaults.buttonColors(containerColor = if(payMethod == "WALLET") Color.Black else Color.Gray)) { Text("Wallet") }
                }
                Button(onClick = {
                    val id = UUID.randomUUID().toString()
                    val trip = Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = destination), notes = note, paymentMethod = payMethod, price = 150.0)
                    FirebaseDatabase.getInstance().getReference("trips").child(id).setValue(trip)
                }, modifier = Modifier.fillMaxWidth()) { Text("Request Ride (150 ETB)") }
            }
        }
    }
}

@Composable
fun TripStatusPanel(trip: Trip) {
    Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (trip.status) {
                TripStatus.REQUESTED -> {
                    CircularProgressIndicator()
                    Text("Searching for a partner...", modifier = Modifier.padding(top = 8.dp))
                }
                TripStatus.ACCEPTED -> {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp))
                    Text("${trip.driverName ?: "Partner"} is coming!", fontWeight = FontWeight.Bold)
                    Text("Note: ${trip.notes}", style = MaterialTheme.typography.bodySmall)
                }
                TripStatus.IN_PROGRESS -> {
                    Text("Trip in Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Destination: ${trip.dropoffLocation.address}")
                }
                else -> {}
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Price: ${trip.price} ETB")
                Text("Pay via: ${trip.paymentMethod}")
            }
        }
    }
}
