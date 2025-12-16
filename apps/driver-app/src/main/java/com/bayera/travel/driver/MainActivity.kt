package com.bayera.travel.driver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent {
            var rawDataLog by remember { mutableStateOf("Listening for data...") }

            LaunchedEffect(Unit) {
                val database = FirebaseDatabase.getInstance()
                val tripsRef = database.getReference("trips")

                tripsRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // DEBUG MODE: Show exactly what the database sends
                        val count = snapshot.childrenCount
                        var log = "Found $count trips:\n"
                        
                        for (child in snapshot.children) {
                            log += "Trip ID: ${child.key}\n"
                            log += "Data: ${child.value}\n\n"
                        }
                        rawDataLog = log
                    }

                    override fun onCancelled(error: DatabaseError) {
                        rawDataLog = "Error: ${error.message}"
                    }
                })
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE8F5E9)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("DEBUG MODE", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = rawDataLog)
                    }
                }
            }
        }
    }
}
