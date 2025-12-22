package com.bayera.travel.delivery

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// FIXED: Use standard ShoppingCart icon instead of LocalShipping
import androidx.compose.material.icons.filled.ShoppingCart 
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import java.util.UUID

// Mock Data for UI Testing
data class DeliveryOrder(
    val id: String,
    val restaurant: String,
    val customer: String,
    val price: Double,
    val status: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent {
            DeliveryDashboard()
        }
    }
}

@Composable
fun DeliveryDashboard() {
    val context = LocalContext.current
    
    // Fake Orders for UI Demo
    val orders = listOf(
        DeliveryOrder("DEL-101", "Burger House", "Yabu", 250.0, "PENDING"),
        DeliveryOrder("DEL-102", "Pizza Corner", "Kebede", 400.0, "PENDING"),
        DeliveryOrder("DEL-103", "Mama's Kitchen", "Sara", 150.0, "PENDING")
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFF3E0)) { // Light Orange
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // FIXED ICON
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFE65100), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bayera Delivery", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("New Orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(orders) { order ->
                        DeliveryCard(order)
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryCard(order: DeliveryOrder) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(order.restaurant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${order.price} ETB", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Customer: ${order.customer}", color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: Accept Logic */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ACCEPT DELIVERY")
            }
        }
    }
}
