package com.bayera.travel.customer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

@androidx.compose.runtime.Composable
fun DashboardUI(nav: androidx.navigation.NavController) {
    androidx.compose.material3.Button(onClick = { nav.navigate("ride") }) { androidx.compose.material3.Text("Open HD Map") }
}
