package com.bayera.travel.driver

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    
    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString("phone", "") ?: "") }
    var carModel by remember { mutableStateOf(prefs.getString("car_model", "") ?: "") }
    var licensePlate by remember { mutableStateOf(prefs.getString("license_plate", "") ?: "") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Partner Registration", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Driver Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = carModel, onValueChange = { carModel = it }, label = { Text("Car Model") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = licensePlate, onValueChange = { licensePlate = it }, label = { Text("License Plate") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isNotEmpty()) {
                    prefs.edit().putString("name", name).putString("phone", phone).putString("car_model", carModel).putString("license_plate", licensePlate).apply()
                    val db = FirebaseDatabase.getInstance().getReference("drivers")
                    val driverId = phone.filter { it.isDigit() }
                    val driverData = mapOf("name" to name, "phone" to phone, "car" to "$carModel ($licensePlate)", "balance" to 0.0)
                    db.child(driverId).updateChildren(driverData)
                    
                    // FIXED ROUTE NAME
                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Go Online") }
    }
}
