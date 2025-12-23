package com.bayera.travel.customer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
import com.bayera.travel.common.models.VehicleType
import com.bayera.travel.utils.FareCalculator
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            
            // --- LIVE DARK MODE STATE ---
            // We hold the state HERE, at the top level
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_mode", isSystemDark)) }

            // Define Colors based on state
            val colors = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = colors) {
                val navController = rememberNavController()
                val startScreen = if (prefs.getString("name", "").isNullOrEmpty()) "login" else "super_home"

                NavHost(navController = navController, startDestination = startScreen) {
                    composable("login") { LoginScreen(navController) }
                    composable("super_home") { SuperAppHome(navController) }
                    composable("ride_home") { RideScreen(navController) }
                    composable("delivery_home") { DeliveryScreen(navController) }
                    composable("profile") { ProfileScreen(navController) }
                    
                    // Pass the function to toggle theme
                    composable("settings") { 
                        SettingsScreen(navController, isDarkTheme) { newMode ->
                            isDarkTheme = newMode
                            prefs.edit().putBoolean("dark_mode", newMode).apply()
                        } 
                    }
                    composable("history") { HistoryScreen(navController) }
                }
            }
        }
    }
}

// --- UPDATED SETTINGS SCREEN (Accepts toggle function) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, currentMode: Boolean, onThemeChanged: (Boolean) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Mode")
                Switch(
                    checked = currentMode, 
                    onCheckedChange = { onThemeChanged(it) } // Live Update!
                )
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Version 1.2.0 - Bayera Travel", color = Color.Gray)
        }
    }
}

// --- UPDATED PROFILE SCREEN (Working Image Picker) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    var name by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var phone by remember { mutableStateOf(prefs.getString("phone", "") ?: "") }
    var imageUri by remember { mutableStateOf(prefs.getString("profile_image", "") ?: "") }

    // Gallery Logic
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // CRITICAL: Take persistent permission so the image stays after restart
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            
            imageUri = uri.toString()
            prefs.edit().putString("profile_image", imageUri).apply()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Image Circle
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(120.dp).clip(CircleShape).clickable { launcher.launch("image/*") }
                ) {
                    if (imageUri.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                    }
                }
                SmallFloatingActionButton(onClick = { launcher.launch("image/*") }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Edit, null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), readOnly = true)
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    prefs.edit().putString("name", name).apply()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Changes") }
            
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { prefs.edit().clear().apply(); navController.navigate("login") }) { Text("Logout", color = Color.Red) }
        }
    }
}
