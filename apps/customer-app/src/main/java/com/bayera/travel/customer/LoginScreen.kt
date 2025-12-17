package com.bayera.travel.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun LoginScreen(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8)) // Light Blue-Grey
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Bayera",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E88E5)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Enter your details to continue", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        // Name Input
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Input
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Login Button
        Button(
            onClick = {
                // For now, we just simulate login and go to Map
                if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                    navController.navigate("home")
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", fontSize = MaterialTheme.typography.bodyLarge.fontSize)
        }
    }
}
