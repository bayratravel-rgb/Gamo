package com.bayera.travel.customer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BayeraApp"
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent {
            val nav = rememberNavController()
            MaterialTheme {
                NavHost(navController = nav, startDestination = "dash") {
                    composable("dash") { DashboardUI(nav) }
                    composable("ride") { RideScreen(nav) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: androidx.navigation.NavController) {
    androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(20.dp)) {
        androidx.compose.material3.Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        androidx.compose.material3.Text("Hi, bb!", style = MaterialTheme.typography.titleLarge)
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
        androidx.compose.material3.Card(onClick = { nav.navigate("ride") }, modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(130.dp), colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE3F2FD))) {
            androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center, modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                androidx.compose.material3.Text("Ride", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
