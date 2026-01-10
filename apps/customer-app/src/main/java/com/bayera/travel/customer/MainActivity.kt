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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.io.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- 🛡️ GLOBAL CRASH CATCHER ---
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fatal_log", sw.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        val crashReport = intent.getStringExtra("fatal_log")
        if (crashReport != null) {
            setContent { ErrorDiagnosticUI(crashReport) }
            return
        }

        // --- 🔑 MANUAL FIREBASE INIT ---
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {}

        setContent { 
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) { 
                CustomerUrbanaApp() 
            } 
        }
    }
}

@Composable
fun CustomerUrbanaApp() {
    var screen by remember { mutableStateOf("home") }
    val db = try { FirebaseDatabase.getInstance().getReference("trips") } catch(e: Exception) { null }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
        if (screen == "home") {
            DashboardUI { screen = "map" }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
                Text("Map Engine Active", modifier = Modifier.align(Alignment.Center), color = Color.White)
                IconButton(onClick = { screen = "home" }, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
                    Icon(Icons.Default.ArrowBack, null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRide: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Urbana Mobility", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Text("Hi, Ravi!", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRide, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF5C6BC0), modifier = Modifier.size(40.dp))
                    Text("Ride", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Card(onClick = {}, modifier = Modifier.weight(1f).height(140.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFF26A69A), modifier = Modifier.size(40.dp))
                    Text("Shopping", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ErrorDiagnosticUI(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚠️ URBANA SYSTEM CRASH", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Error Details (Please copy and send):", color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        Text(log, color = Color.Yellow, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}
