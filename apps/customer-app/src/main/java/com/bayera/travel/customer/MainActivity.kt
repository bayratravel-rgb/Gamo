package com.bayera.travel.customer

import android.os.*
import android.content.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import java.io.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- 🛡️ GLOBAL CRASH CATCHER ---
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fatal_err", sw.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        val crashLog = intent.getStringExtra("fatal_err")

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (crashLog != null) {
                        // Show the yellow error text if it crashes
                        ErrorDisplay(crashLog)
                    } else {
                        // Start the App Safely
                        SafeAppLoader()
                    }
                }
            }
        }
    }
}

@Composable
fun SafeAppLoader() {
    var isInitialized by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }

    // 🛡️ Initialize Firebase in the background so the UI doesn't freeze/crash
    LaunchedEffect(Unit) {
        try {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel")
                .build()
            
            FirebaseApp.initializeApp(androidx.compose.ui.platform.LocalContext.current, options)
            isInitialized = true
        } catch (e: Exception) {
            initError = e.message
            isInitialized = true // Show dashboard anyway
        }
    }

    if (!isInitialized) {
        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        // Dashboard opens NO MATTER WHAT
        DashboardUI()
    }
}

@Composable
fun DashboardUI() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge)
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.padding(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f).height(120.dp).padding(4.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("Ride") }
            }
            Card(modifier = Modifier.weight(1f).height(120.dp).padding(4.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("Shopping") }
            }
        }
    }
}

@Composable
fun ErrorDisplay(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        Text("⚠️ SYSTEM ERROR", color = Color.Red, modifier = Modifier.padding(16.dp))
        Text(log, color = Color.Yellow, modifier = Modifier.padding(16.dp))
    }
}
