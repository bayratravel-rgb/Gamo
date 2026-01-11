package com.bayera.travel.customer

import android.content.*
import android.os.*
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
import com.bayera.travel.common.models.*
import com.bayera.travel.utils.FareCalculator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val nav = rememberNavController()
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                NavHost(navController = nav, startDestination = "dash") {
                    composable("dash") { BayeraDashboard(nav) }
                    composable("summary/{vehicle}") { backStack ->
                        val vType = backStack.arguments?.getString("vehicle") ?: "BAJAJ"
                        TripSummaryScreen(nav, vType)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BayeraDashboard(nav: NavController) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Choose Vehicle", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            VehicleCard("Bajaj", Icons.Default.ElectricRickshaw, Color(0xFFE8F5E9), Modifier.weight(1f)) { nav.navigate("summary/BAJAJ") }
            Spacer(modifier = Modifier.width(16.dp))
            VehicleCard("Code 3", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Modifier.weight(1f)) { nav.navigate("summary/CODE_3") }
        }
    }
}

@Composable
fun VehicleCard(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(140.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = Color.Black)
            Text(name, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun TripSummaryScreen(nav: NavController, vehicleType: String) {
    val price = FareCalculator.calculatePrice(5.0, vehicleType) // Example 5km trip
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
        Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Trip Summary", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Vehicle: $vehicleType", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Fare: $price ETB", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                Text("Based on 300 ETB/L Benzine", fontSize = 10.sp, color = Color.Gray)
                
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))
                ) { Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
            Icon(Icons.Default.ArrowBack, null)
        }
    }
}
