package com.bayera.travel.customer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
        
        // DEBUG: Initialize Firebase explicitly
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase Init Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        setContent {
            val context = LocalContext.current
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Database Test", style = MaterialTheme.typography.headlineLarge)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(onClick = {
                    try {
                        val database = FirebaseDatabase.getInstance()
                        val myRef = database.getReference("test_message")
                        
                        myRef.setValue("Hello from Arba Minch!")
                            .addOnSuccessListener {
                                Toast.makeText(context, "SUCCESS! check database now.", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "FAILED: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, "CRASH: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("SEND TEST DATA")
                }
            }
        }
    }
}
