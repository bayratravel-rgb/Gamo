package com.bayera.travel.customer

import android.content.*
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- 🛡️ CRASH PROTECTOR ---
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("fatal_log", sw.toString()); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent); android.os.Process.killProcess(android.os.Process.myPid())
        }

        if (intent.getStringExtra("fatal_log") != null) {
            setContent { ErrorUI(intent.getStringExtra("fatal_log")!!) }; return
        }

        // --- 🔑 MANUAL FIREBASE INIT ---
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val opt = FirebaseOptions.Builder()
                    .setApplicationId("1:643765664968:android:801ade1a7ec854095af9fd")
                    .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                    .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                    .setProjectId("bayera-travel").build()
                FirebaseApp.initializeApp(this, opt)
            }
        } catch (e: Exception) {}

        setContent {
            val nav = rememberNavController()
            val prefs = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val start = if (prefs.getString("email", "").isNullOrEmpty()) "login" else "dash"
            
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                NavHost(navController = nav, startDestination = start) {
                    composable("login") { LoginScreen(nav) }
                    composable("dash") { DashboardUI(nav) }
                    composable("map") { MapPlaceholder(nav) }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(nav: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(32.dp), verticalArrangement = Arrangement.Center) {
        Text("Welcome to Urbana", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalize your mobility", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
        
        Button(
            onClick = {
                if (name.isNotEmpty() && email.contains("@")) {
                    context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
                        .putString("name", name).putString("email", email).apply()
                    nav.navigate("dash") { popUpTo("login") { inclusive = true } }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) { Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardUI(nav: NavController) {
    val name = LocalContext.current.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("name", "User")
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Urbana Mobility", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Hi, $name!", color = Color.LightGray, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.weight(1f))
            // ⚙️ SETTINGS BUTTON
            IconButton(onClick = { /* Settings logic */ }) {
                Icon(Icons.Default.Settings, null, tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        Text("Services", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // RIDE TILE
            ServiceTile("Ride", Icons.Default.DirectionsCar, Color(0xFFE3F2FD), Color(0xFF1A237E), Modifier.weight(1f)) { nav.navigate("map") }
            Spacer(modifier = Modifier.width(16.dp))
            // HOTEL TILE (Replaced Shopping)
            ServiceTile("Hotels", Icons.Default.Hotel, Color(0xFFF3E5F5), Color(0xFF6A1B9A), Modifier.weight(1f)) { /* Hotels logic */ }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = { 
            val context = nav.context
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            nav.navigate("login") { popUpTo(0) }
        }) { Text("Log out", color = Color.Red.copy(alpha = 0.6f)) }
    }
}

@Composable
fun ServiceTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bg: Color, iconColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(140.dp), colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = iconColor)
            Text(title, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun MapPlaceholder(nav: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
        Text("Map Engine Ready", modifier = Modifier.align(Alignment.Center), color = Color.White)
        IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, RoundedCornerShape(8.dp))) {
            Icon(Icons.Default.ArrowBack, null)
        }
    }
}

@Composable
fun ErrorUI(log: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("⚠️ SYSTEM ERROR", color = Color.Red, fontWeight = FontWeight.Bold)
        Text(log, color = Color.Yellow, style = MaterialTheme.typography.bodySmall)
    }
}
