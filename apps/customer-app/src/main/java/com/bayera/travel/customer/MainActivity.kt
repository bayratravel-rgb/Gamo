package com.bayera.travel.customer

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            val navController = rememberNavController()
            
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") { SplashScreen(navController) }
                composable("login") { LoginScreen(navController) }
                composable("super_home") { SuperAppHome(navController) }
                composable("ride_home") { RideScreen(navController) }
                composable("delivery_home") { DeliveryScreen(navController) }
                composable("profile") { ProfileScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
                composable("history") { HistoryScreen(navController) }
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = OvershootInterpolator(2f))
        )
        delay(1500) // Hold for 1.5 seconds

        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val nextScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_home"
        
        navController.navigate(nextScreen) {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1E88E5)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(100.dp).scale(scale.value),
                shape = CircleShape,
                color = Color.White
            ) {
                Icon(
                    Icons.Default.FlightTakeoff,
                    contentDescription = "Logo",
                    tint = Color(0xFF1E88E5),
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bayera Travel",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
// (Include OverShootInterpolator helper)
fun OvershootInterpolator(tension: Float): Easing {
    return Easing { x ->
        val t = x - 1
        t * t * ((tension + 1) * t + tension) + 1
    }
}

// ... (Rest of SuperAppHome and ServiceCard code remains the same)
// I will re-paste them below to ensure the file is complete.

@Composable
fun SuperAppHome(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = { navController.navigate("history") })
                NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Account") }, selected = false, onClick = { navController.navigate("profile") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hi, $userName!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Default.Settings, null) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Services", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ServiceCard("Ride", Icons.Default.LocalTaxi, Color(0xFFE3F2FD), Color(0xFF1E88E5)) { navController.navigate("ride_home") }
                ServiceCard("Delivery", Icons.Default.ShoppingCart, Color(0xFFFFF3E0), Color(0xFFE65100)) { navController.navigate("delivery_home") }
            }
        }
    }
}

@Composable
fun ServiceCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, iconColor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.width(160.dp).height(120.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
