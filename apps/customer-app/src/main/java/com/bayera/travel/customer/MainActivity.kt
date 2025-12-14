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
// --- IMPORT FROM SHARED MODULE ---
import com.bayera.travel.common.models.User
import com.bayera.travel.common.models.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a dummy user using the Shared Data Model
        val currentUser = User(
            id = "US-12345",
            phoneNumber = "+251911223344",
            name = "Abebe Bikila",
            role = UserRole.CUSTOMER
        )

        setContent {
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
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Display the name from the Shared Model
                        Text(
                            text = "Welcome, ${currentUser.name}!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.DarkGray
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { 
                                Toast.makeText(context, "Booking for ${currentUser.name}...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Book a Flight")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                Toast.makeText(context, "ID: ${currentUser.id}", Toast.LENGTH_SHORT).show()
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
