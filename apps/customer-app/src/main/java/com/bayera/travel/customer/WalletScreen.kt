package com.bayera.travel.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bayera.travel.common.payment.ChapaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            
            Card(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)), 
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Balance", color = Color.White.copy(alpha = 0.8f))
                    Text("$balance ETB", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Top Up Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (ETB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount >= 5.0) {
                        isLoading = true
                        val txRef = "TX-${UUID.randomUUID().toString().take(10)}"
                        val email = "customer@bayera.com" 
                        val fName = prefs.getString("name", "User") ?: "User"
                        val lName = "Customer" // Added missing lastName argument
                        
                        // FIX: Ensure arguments match ChapaManager.initializePayment exactly
                        ChapaManager.initializePayment(email, amount, fName, lName, txRef) { url ->
                            // Use Launch inside Composable scope to update UI
                            // But since this callback is from a Thread, we need to be careful.
                            // Compose state updates are thread-safe, but Toast/Intent needs UI thread.
                            
                            // We can't launch coroutine here easily without scope. 
                            // Using mainLooper handler is safer for non-composable callback.
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isLoading = false
                                if (!url.isNullOrEmpty()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                        
                                        val newBal = balance + amount.toFloat()
                                        prefs.edit().putFloat("wallet_balance", newBal).apply()
                                        balance = newBal
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No Browser Found", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Payment Failed. Server Error.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "Enter valid amount (> 5 ETB)", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Pay with Telebirr / Chapa")
            }
        }
    }
}
