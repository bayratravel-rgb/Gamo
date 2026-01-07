package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookingScreen()
                }
            }
        }
    }
}

@Composable
fun BookingScreen() {
    var dest by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Request Ride", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = dest, onValueChange = { dest = it }, label = { Text("Where to?") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Notes for Driver") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val id = UUID.randomUUID().toString()
            val trip = Trip(tripId = id, customerPhone = "0911...", dropoffLocation = Location(address = dest), notes = note)
            FirebaseDatabase.getInstance().getReference("trips").child(id).setValue(trip)
        }, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
            Text("Set Pickup & Request")
        }
    }
}
