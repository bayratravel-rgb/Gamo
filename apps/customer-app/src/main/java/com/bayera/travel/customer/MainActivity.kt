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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This grabs the current context so we can show popups
            val context = LocalContext.current
            
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF0F4F8)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✈️ Bayera Travel",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Primary Button with Click Action
                        Button(
                            onClick = { 
                                // This is the logic!
                                Toast.makeText(context, "Searching for Flights... 🛫", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Book a Flight")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Secondary Button with Click Action
                        OutlinedButton(
                            onClick = { 
                                Toast.makeText(context, "You have no tickets yet 🎟️", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("My Tickets")
                        }
                    }
                }
            }
        }
    }
}
