package com.bayera.travel.driver

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // FIXED: Added Import
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("driver_prefs", Context.MODE_PRIVATE)
    
    // In real app, fetch from Firebase. For MVP, use local cache or 0.
    var balance by remember { mutableStateOf(500.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Earnings") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)), 
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Text("Total Earnings", color = Color.White.copy(alpha = 0.8f))
                    Text("$balance ETB", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    Toast.makeText(context, "Withdrawal Request Sent to Admin!", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Request Withdrawal")
            }
        }
    }
}
