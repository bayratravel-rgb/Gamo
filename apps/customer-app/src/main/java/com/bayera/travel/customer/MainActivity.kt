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
    val userPhone = prefs.getString("phone", "user_test") ?: "user_test"
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
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
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
             Text("📍 Arba Minch Live Map", modifier = Modifier.align(Alignment.Center))
        }
        if (activeTrip == null) {
            Box(modifier = Modifier.fillMaxSize()) { BookingUI(userPhone, db, Modifier.align(Alignment.BottomCenter)) }
        } else {
            Box(modifier = Modifier.fillMaxSize()) { LockedTripUI(activeTrip!!, Modifier.align(Alignment.BottomCenter)) }
        }
    }
}

@Composable
fun BookingUI(phone: String, db: DatabaseReference, modifier: Modifier) {
    var dest by remember { mutableStateOf("") }
    var payMethod by remember { mutableStateOf("CASH") }
    Card(modifier = modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Where to?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = dest, onValueChange = { dest = it }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { payMethod = "CASH" }, colors = ButtonDefaults.buttonColors(containerColor = if(payMethod=="CASH") Color.Black else Color.Gray)) { Text("Cash") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { payMethod = "WALLET" }, colors = ButtonDefaults.buttonColors(containerColor = if(payMethod=="WALLET") Color.Black else Color.Gray)) { Text("Wallet") }
            }
            Button(onClick = {
                val id = UUID.randomUUID().toString()
                db.child(id).setValue(Trip(tripId = id, customerPhone = phone, dropoffLocation = Location(address = dest), paymentMethod = payMethod, price = 150.0))
            }, modifier = Modifier.fillMaxWidth()) { Text("Request Ride") }
        }
    }
}

@Composable
fun LockedTripUI(trip: Trip, modifier: Modifier) {
    val isEnRoute = trip.status == TripStatus.IN_PROGRESS
    Card(modifier = modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = if(isEnRoute) Color(0xFFFFEBEE) else Color.White)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if(isEnRoute) "LOCKED: EN ROUTE" else "SEARCHING...", color = if(isEnRoute) Color.Red else Color.Black, fontWeight = FontWeight.ExtraBold)
            Text("Destination: ${trip.dropoffLocation.address}")
            Text("Price: ${trip.price} ETB", fontWeight = FontWeight.Bold)
        }
    }
}
