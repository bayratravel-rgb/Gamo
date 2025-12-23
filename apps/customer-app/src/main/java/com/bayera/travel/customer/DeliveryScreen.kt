package com.bayera.travel.customer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fastfood // Standard icon
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bayera.travel.common.models.Delivery
import com.bayera.travel.common.models.DeliveryStatus
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val userPhone = prefs.getString("phone", "") ?: ""

    // Hardcoded Arba Minch Restaurants
    val restaurants = listOf(
        "Momo Hotel" to 250.0, 
        "Tourist Hotel" to 300.0, 
        "Paradise Lodge" to 450.0, 
        "Sodo Coffee" to 100.0
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Delivery") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Order Lunch 🍔", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            Text("Deliver to: Arba Minch", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(restaurants) { (name, basePrice) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fastfood, null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Avg Price: $basePrice ETB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Button(
                                onClick = { 
                                    // SEND ORDER TO FIREBASE
                                    val db = FirebaseDatabase.getInstance().getReference("deliveries")
                                    val newId = UUID.randomUUID().toString()
                                    val order = Delivery(
                                        id = newId,
                                        customerName = userName,
                                        customerPhone = userPhone,
                                        restaurantName = name,
                                        items = "Special Meal", // Simplified for MVP
                                        price = basePrice + 50.0, // Food + Delivery Fee
                                        location = "Current GPS Loc",
                                        status = DeliveryStatus.PENDING
                                    )
                                    db.child(newId).setValue(order)
                                    Toast.makeText(context, "Order Sent to $name!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                            ) {
                                Text("Order")
                            }
                        }
                    }
                }
            }
        }
    }
}
