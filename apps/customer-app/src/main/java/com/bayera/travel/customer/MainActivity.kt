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
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import java.io.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- 🛡️ CRASH PROTECTOR ---
        // If anything crashes, we show the error instead of closing.
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fatal_error", sw.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        // Check if we are showing a crash report
        val errorMsg = intent.getStringExtra("fatal_error")
        if (errorMsg != null) {
            setContent { MaterialTheme { ErrorUI(errorMsg) } }
            return
        }

        // --- 🛡️ SAFE FIREBASE START ---
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // If Firebase fails (e.g. missing config), we let the app keep running
        }

        // --- 🚀 LAUNCH UI ---
        setContent { 
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CustomerSuperDashboard() 
                }
            }
        }
    }
}

@Composable
fun CustomerSuperDashboard() {
    // This is the dashboard you want for marketing
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Bayera Travel", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Hi, bb!", style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Services", color = Color.Gray)
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Card(modifier = Modifier.weight(1f).height(120.dp).padding(end = 8.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Ride", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).height(120.dp).padding(start = 8.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Shopping", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Hotels & Resorts", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ErrorUI(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚠️ APP STOPPED WORKING", color = Color.Red, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(10.dp))
        Text("The error is shown below. Please copy it:", color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Text(log, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
    }
}
