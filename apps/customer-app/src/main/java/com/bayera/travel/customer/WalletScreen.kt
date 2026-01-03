package com.bayera.travel.customer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import java.util.UUID
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    var balance by remember { mutableFloatStateOf(prefs.getFloat("wallet_balance", 0.0f)) }
    var amountText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingAmount by remember { mutableStateOf(0.0) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Payment") },
            text = { Text("Did you complete the payment on Telebirr/Chapa?") },
            confirmButton = {
                TextButton(onClick = {
                    // Update Balance ONLY if user confirms
                    val newBal = balance + pendingAmount.toFloat()
                    val userPhone = prefs.getString("phone", "")?.replace("+", "") ?: "unknown"
                    val db = FirebaseDatabase.getInstance().getReference("users").child(userPhone).child("wallet_balance")
                    db.setValue(newBal)
                    
                    balance = newBal
                    showConfirmDialog = false
                    Toast.makeText(context, "Balance Updated!", Toast.LENGTH_SHORT).show()
                }) { Text("Yes, I Paid") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("No, Cancel") }
            }
        )
    }

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
                    Text("Current Balance", color = Color.White.copy(alpha = 0.8f))
                    Text("$balance ETB", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Top Up Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (ETB)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount >= 5.0) {
                        isLoading = true
                        val txRef = "TX-${UUID.randomUUID().toString().take(10)}"
                        val email = "Yeabkalkassahun21@gmail.com" 
                        val fName = prefs.getString("name", "User") ?: "User"
                        val lName = "Customer"

                        ChapaManager.initializePayment(email, amount, fName, lName, txRef) { url, error ->
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                isLoading = false
                                if (url != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                    // Don't update balance yet! Show Dialog.
                                    pendingAmount = amount
                                    showConfirmDialog = true
                                } else {
                                    Toast.makeText(context, "Failed: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else { Toast.makeText(context, "Enter > 5 ETB", Toast.LENGTH_SHORT).show() }
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
