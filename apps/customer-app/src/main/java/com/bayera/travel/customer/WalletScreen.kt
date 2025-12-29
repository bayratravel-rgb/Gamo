package com.bayera.travel.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bayera.travel.common.payment.ChapaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    var balance by remember { mutableFloatStateOf(prefs.getFloat("wallet_balance", 0.0f)) }
    var amountText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wallet") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Balance: $balance ETB", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount >= 5.0) {
                        isLoading = true
                        val txRef = "TX-${UUID.randomUUID().toString().take(10)}"
                        val email = "customer@bayera.com" 
                        val fName = prefs.getString("name", "User") ?: "User"
                        
                        // DEBUG MODE: Show URL
                        ChapaManager.initializePayment(email, amount, fName, "Bayera", txRef) { url ->
                            isLoading = false
                            if (url != null) {
                                // SHOW URL TO USER (For Debugging)
                                println("Chapa URL: $url")
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } else {
                                // Must run on UI Thread
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Loading..." else "Pay Now")
            }
        }
    }
}
