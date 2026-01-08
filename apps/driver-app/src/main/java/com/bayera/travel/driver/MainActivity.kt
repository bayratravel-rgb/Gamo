package com.bayera.travel.driver

import android.content.*
import android.os.*
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
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("error", sw.toString()); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent); android.os.Process.killProcess(android.os.Process.myPid())
        }

        if (intent.getStringExtra("error") != null) {
            setContent { ErrorScreen(intent.getStringExtra("error")!!) }; return
        }

        try { FirebaseApp.initializeApp(applicationContext) } catch (e: Exception) {}

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Partner App Ready", style = MaterialTheme.typography.headlineMedium)
                        Text("Waiting for rides...")
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(err: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState())) {
        Text(err, color = Color.Yellow)
    }
}
