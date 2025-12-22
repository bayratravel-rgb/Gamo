package com.bayera.travel.hotelreception

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// FIXED: Explicit Imports
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalTaxi 
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { HotelDashboard() }
    }
}

@Composable
fun HotelDashboard() {
    val context = LocalContext.current
    var roomNumber by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var hotelName by remember { mutableStateOf("Haile Resort Arba Minch") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF3E5F5)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Hotel, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(hotelName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                Text("Concierge Service", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Book Guest Ride", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = { Text("Room Number") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (roomNumber.isNotEmpty() && destination.isNotEmpty()) {
                                    val db = FirebaseDatabase.getInstance().getReference("trips")
                                    val newId = UUID.randomUUID().toString()
                                    val trip = Trip(
                                        tripId = newId,
                                        customerId = "$hotelName (Room: $roomNumber)",
                                        pickupLocation = Location(6.0206, 37.5557, hotelName),
                                        dropoffLocation = Location(0.0, 0.0, destination),
                                        price = 200.0,
                                        status = TripStatus.REQUESTED
                                    )
                                    db.child(newId).setValue(trip)
                                    Toast.makeText(context, "Taxi Requested!", Toast.LENGTH_LONG).show()
                                    roomNumber = ""
                                    destination = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.LocalTaxi, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CALL TAXI NOW")
                        }
                    }
                }
            }
        }
    }
}
