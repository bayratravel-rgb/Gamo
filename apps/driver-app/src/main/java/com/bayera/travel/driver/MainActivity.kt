package com.bayera.travel.driver

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// Importing Shared Logic
import com.bayera.travel.common.models.User
import com.bayera.travel.common.models.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Using Shared Model for Driver
        val driverUser = User(
            id = "DRV-9988",
            phoneNumber = "+251911556677",
            name = "Kebede Driver",
            role = UserRole.DRIVER
        )

        setContent {
            val context = LocalContext.current
            var isOnline by remember { mutableStateOf(false) }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFE8F5E9) // Light Green Background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🚖 Driver App",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32) // Dark Green
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Welcome, ${driverUser.name}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { 
                                isOnline = !isOnline
                                val status = if (isOnline) "Online" else "Offline"
                                Toast.makeText(context, "You are now $status", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnline) Color(0xFF2E7D32) else Color.Gray
                            ),
                            modifier = Modifier.width(220.dp).height(50.dp)
                        ) {
                            Text(if (isOnline) "🟢 GO OFFLINE" else "🔴 GO ONLINE")
                        }
                    }
                }
            }
        }
    }
}
