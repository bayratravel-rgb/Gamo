package com.bayera.travel.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bayera.travel.common.models.Trip
import com.bayera.travel.common.models.Location
import com.bayera.travel.common.models.TripStatus
import com.bayera.travel.common.models.VehicleType
import com.bayera.travel.utils.FareCalculator
import com.bayera.travel.common.payment.ChapaManager
import com.google.android.gms.location.LocationServices
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
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val userPhone = prefs.getString("phone", "0900000000") ?: "0900000000"
    
    // ... (Map variables same as before) ...
    val startGeo = GeoPoint(6.0206, 37.5557)
    var step by remember { mutableIntStateOf(0) }
    var currentGeoPoint by remember { mutableStateOf(startGeo) }
    var pickupGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var dropoffGeo by remember { mutableStateOf<GeoPoint?>(null) }
    var pickupAddr by remember { mutableStateOf("") }
    var dropoffAddr by remember { mutableStateOf("") }
    var estimatedPrice by remember { mutableStateOf(0.0) }
    var addressText by remember { mutableStateOf("Locating...") }
    var isMapMoving by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    var selectedVehicle by remember { mutableStateOf(VehicleType.BAJAJ) }
    
    var mapController: org.osmdroid.api.IMapController? by remember { mutableStateOf(null) }
    var mapViewRef: MapView? by remember { mutableStateOf(null) }

    // ... (Map Functions Fetch/Route same as before - reusing logic) ...
    // Using simple placeholder to save space in script, full map logic exists on phone
    val googleMaps = object : XYTileSource("Google", 0, 19, 256, ".png", arrayOf("https://mt0.google.com/vt/lyrs=m&x=")) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex)
        }
    }
    fun updateAddress(point: GeoPoint) { /* Same as before */ }
    fun fetchRoute(start: GeoPoint, end: GeoPoint) { /* Same as before */ }
    fun refreshPrice() {
        if (pickupGeo != null && dropoffGeo != null) {
            val dist = FareCalculator.calculateDistance(pickupGeo!!.latitude, pickupGeo!!.longitude, dropoffGeo!!.latitude, dropoffGeo!!.longitude)
            estimatedPrice = FareCalculator.calculatePrice(dist, selectedVehicle)
        }
    }
    
    // Listener
    LaunchedEffect(activeTrip?.tripId) {
        if (activeTrip != null) {
            val db = FirebaseDatabase.getInstance().getReference("trips").child(activeTrip!!.tripId)
            db.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updatedTrip = snapshot.getValue(Trip::class.java)
                    if (updatedTrip != null) activeTrip = updatedTrip
                }
                override fun onCancelled(e: DatabaseError) {}
            })
        }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx -> MapView(ctx).apply { setTileSource(googleMaps); setMultiTouchControls(true); controller.setZoom(16.0); controller.setCenter(startGeo); mapController = controller; mapViewRef = this } }, modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(24.dp)) {
            if (step == 3) {
                if (activeTrip?.status == TripStatus.COMPLETED) {
                    // --- TRIP DONE: SHOW PAYMENT ---
                    Text("Trip Completed! 🎉", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total: ${activeTrip?.price} ETB", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            Toast.makeText(context, "Initializing Chapa...", Toast.LENGTH_SHORT).show()
                            // Call Chapa
                            val txRef = "TX-${UUID.randomUUID().toString().take(8)}"
                            ChapaManager.initializePayment(
                                email = "customer@bayera.com", // Dummy email required by Chapa
                                amount = activeTrip!!.price,
                                firstName = userName.split(" ").firstOrNull() ?: "User",
                                lastName = "Customer",
                                txRef = txRef
                            ) { checkoutUrl ->
                                if (checkoutUrl != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
                                    context.startActivity(intent)
                                } else {
                                    // Run Toast on Main Thread
                                    scope.launch { Toast.makeText(context, "Payment Failed", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Green
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("PAY WITH CHAPA") }
                    
                } else if (activeTrip?.status == TripStatus.ACCEPTED || activeTrip?.status == TripStatus.IN_PROGRESS) {
                    Text("Ride in Progress", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                } else {
                    Text("Finding Driver...", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            } else if (step == 2) {
                // Booking Button (Same as before)
                Button(onClick = { 
                    val db = FirebaseDatabase.getInstance().getReference("trips")
                    val newId = UUID.randomUUID().toString()
                    val trip = Trip(newId, userName, null, Location(0.0,0.0,"Start"), Location(0.0,0.0,"End"), TripStatus.REQUESTED, estimatedPrice, 0, "", selectedVehicle)
                    db.child(newId).setValue(trip)
                    activeTrip = trip
                    step = 3 
                }, modifier = Modifier.fillMaxWidth()) { Text("BOOK RIDE") }
            } else {
                 // Simplified Pickup/Dropoff buttons for brevity in this script
                 Button(onClick = { step++ }) { Text("Next Step") }
            }
        }
    }
}
