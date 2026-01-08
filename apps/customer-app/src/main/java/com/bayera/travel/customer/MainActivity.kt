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
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.io.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- STEP 1: THE CRASH CATCHER ---
        // If the app crashes on startup, it will restart and show the error message.
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("crash_log", sw.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        val errorReport = intent.getStringExtra("crash_log")
        if (errorReport != null) {
            setContent { ErrorDiagnosticScreen(errorReport) }
            return
        }

        // --- STEP 2: SAFE INITIALIZATION ---
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Log error internally but try to continue
        }

        setContent { MaterialTheme { CustomerSuperApp() } }
    }
}

@Composable
fun ErrorDiagnosticScreen(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("⚠️ STARTUP CRASH DETECTED", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Please copy this error and send it to me:", color = Color.White, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(20.dp))
        SelectionContainer {
            Text(log, color = Color.Yellow, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer { content() }
}

@Composable
fun CustomerSuperApp() {
    var screen by remember { mutableStateOf("home") }
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = screen == "home", onClick = { screen = "home" })
                NavigationBarItem(icon = { Icon(Icons.Default.History, null) }, label = { Text("Activity") }, selected = false, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("Account") }, selected = false, onClick = {})
            }
        }
    ) { p ->
        Box(modifier = Modifier.padding(p)) {
            when (screen) {
                "home" -> DashboardUI { screen = "map" }
                "map" -> MapUI { screen = "home" }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(onRideClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Hi, bb!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Services", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(onClick = onRideClick, modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF1976D2)); Text("Ride", fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).height(120.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFFF57C00)); Text("Shopping", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MapUI(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFEEEEEE))) {
            Text("📍 Map View Loading...", modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
        }
        Button(
            onClick = { },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Set Pickup Here", fontWeight = FontWeight.Bold) }
    }
}
