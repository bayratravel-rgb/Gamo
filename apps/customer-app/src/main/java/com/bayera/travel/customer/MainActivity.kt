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
import com.google.firebase.database.FirebaseDatabase
import java.io.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("error", sw.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        val errorMsg = intent.getStringExtra("error")
        if (errorMsg != null) {
            setContent { ErrorScreen(errorMsg) }
            return
        }

        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Bayera Customer App", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Status: System Operational ✅")
                        Button(onClick = { /* Booking logic */ }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                            Text("Find a Ride")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("CUSTOMER APP CRASHED", color = Color.Red, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(10.dp))
        Text(error, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
    }
}
