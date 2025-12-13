package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF0F4F8) // Light Blue-Grey Background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = "✈️ Bayera Travel",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5) // Nice Blue Color
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Primary Button
                        Button(
                            onClick = { /* TODO: Add action */ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Book a Flight")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Secondary Button
                        OutlinedButton(
                            onClick = { /* TODO: Add action */ },
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
