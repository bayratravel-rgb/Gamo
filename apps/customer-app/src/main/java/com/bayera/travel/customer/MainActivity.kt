package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.bayera.travel.common.models.*
import com.bayera.travel.utils.FareCalculator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            val nav = rememberNavController()
            val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { Text("Login Placeholder") }
                    composable("dash") { DashboardUI(nav) }
                    composable("summary/{vType}") { b ->
                        val type = b.arguments?.getString("vType") ?: "BAJAJ"
                        SummaryUI(nav, if(type == "CODE_3") VehicleType.CODE_3 else VehicleType.BAJAJ)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(onClick = { nav.navigate("summary/BAJAJ") }, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("Bajaj", fontWeight = FontWeight.Bold, color = Color.Black) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(onClick = { nav.navigate("summary/CODE_3") }, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("Code 3", fontWeight = FontWeight.Bold, color = Color.Black) }
            }
        }
    }
}

@Composable
fun SummaryUI(nav: NavController, type: VehicleType) {
    val price = FareCalculator.calculatePrice(5.0, type)
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Trip Summary", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Vehicle: ${type.name}", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("$price ETB", style = MaterialTheme.typography.displaySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("Price adjusted for 300 ETB/L Benzine", fontSize = 10.sp, color = Color.Gray)
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top=16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) {
                    Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
