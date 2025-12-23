package com.bayera.travel.customer

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
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
// FIXED: Added missing import for FareCalculator
import com.bayera.travel.utils.FareCalculator
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
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("name", "User") ?: "User"
    val userPhone = prefs.getString("phone", "") ?: ""
    
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

    val googleMaps = object : XYTileSource("Google", 0, 19, 256, ".png", arrayOf("https://mt0.google.com/vt/lyrs=m&x=")) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex)
        }
    }

    fun updateAddress(point: GeoPoint) {
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val shortAddr = addresses[0].getAddressLine(0).split(",").take(2).joinToString(",")
                    withContext(Dispatchers.Main) { addressText = shortAddr }
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { addressText = "Unknown" } }
        }
    }

    fun fetchRoute(start: GeoPoint, end: GeoPoint) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val url = "http://router.project-osrm.org/route/v1/driving/${start.longitude},${start.latitude};${end.longitude},${end.latitude}?overview=full&geometries=geojson"
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body!!.string())
                    val coordinates = json.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                    val points = ArrayList<GeoPoint>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        points.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                    }
                    withContext(Dispatchers.Main) { routePoints = points }
                }
            } catch (e: Exception) {}
        }
    }

    fun refreshPrice() {
        if (pickupGeo != null && dropoffGeo != null) {
            val dist = FareCalculator.calculateDistance(pickupGeo!!.latitude, pickupGeo!!.longitude, dropoffGeo!!.latitude, dropoffGeo!!.longitude)
            estimatedPrice = FareCalculator.calculatePrice(dist, selectedVehicle)
        }
    }

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

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    fun zoomToUser() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val userPos = GeoPoint(loc.latitude, loc.longitude)
                    mapController?.animateTo(userPos)
                    mapController?.setZoom(18.0)
                }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true) zoomToUser() }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) zoomToUser()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(googleMaps)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    minZoomLevel = 4.0
                    maxZoomLevel = 22.0
                    controller.setZoom(16.0)
                    controller.setCenter(startGeo)
                    mapController = controller
                    mapViewRef = this
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean { isMapMoving = true; return true }
                        override fun onZoom(event: ZoomEvent?): Boolean { isMapMoving = true; return true }
                    })
                }
            },
            update = { mapView ->
                if (isMapMoving) currentGeoPoint = mapView.mapCenter as GeoPoint
                mapView.overlays.clear()
                if (step >= 1 || step == 3) {
                    val m1 = Marker(mapView)
                    m1.position = pickupGeo
                    m1.title = "Pickup"
                    m1.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
                    mapView.overlays.add(m1)
                }
                if ((step >= 2 || step == 3) && dropoffGeo != null) {
                    val m2 = Marker(mapView)
                    m2.position = dropoffGeo
                    m2.title = "Dropoff"
                    mapView.overlays.add(m2)
                    if (routePoints.isNotEmpty()) {
                        val line = Polyline()
                        line.setPoints(routePoints)
                        line.color = android.graphics.Color.BLUE
                        line.width = 15f
                        mapView.overlays.add(line)
                    }
                }
                mapView.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(isMapMoving) {
            if (isMapMoving && step < 2) {
                kotlinx.coroutines.delay(800)
                isMapMoving = false 
                updateAddress(currentGeoPoint)
            }
        }

        if (step < 2) {
            Icon(imageVector = if (step == 0) Icons.Default.Home else Icons.Default.Flag, contentDescription = "Pin", modifier = Modifier.size(40.dp).align(Alignment.Center).offset(y = (-20).dp), tint = if (step == 0) Color(0xFF2E7D32) else Color(0xFFD32F2F))
        }

        FloatingActionButton(onClick = { zoomToUser() }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).offset(y = 50.dp), containerColor = Color.White) { Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color(0xFF1E88E5)) }
        
        FloatingActionButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp), containerColor = Color.White) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black) }

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(24.dp)) {
            if (step == 3) {
                if (activeTrip?.status == TripStatus.ACCEPTED) {
                    Text("✅ Driver Found!", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Text("Driver: ${activeTrip?.driverId}", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Finding a ${selectedVehicle.name}...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { step = 0; activeTrip = null; pickupGeo = null; dropoffGeo = null; routePoints = emptyList() }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray), modifier = Modifier.fillMaxWidth()) { Text("Cancel Request", color = Color.Black) }
                }
            } else if (step == 0) {
                Text("Start Trip From?", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { val center = mapViewRef?.mapCenter as? GeoPoint; if (center != null) { pickupGeo = center; pickupAddr = addressText; step = 1 } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Set Pickup Here") }
            } else if (step == 1) {
                Text("Where to?", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text(addressText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { 
                    val center = mapViewRef?.mapCenter as? GeoPoint
                    if (center != null) {
                        dropoffGeo = center; dropoffAddr = addressText
                        fetchRoute(pickupGeo!!, dropoffGeo!!)
                        refreshPrice() 
                        step = 2 
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Set Destination Here") }
            } else if (step == 2) {
                Text("Trip Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                    items(VehicleType.values()) { vehicle ->
                        FilterChip(
                            selected = selectedVehicle == vehicle,
                            onClick = { selectedVehicle = vehicle; refreshPrice() },
                            label = { Text(vehicle.name) },
                            leadingIcon = { if (selectedVehicle == vehicle) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Price", color = Color.Gray)
                    Text("$estimatedPrice ETB", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { 
                    val db = FirebaseDatabase.getInstance().getReference("trips")
                    val newId = UUID.randomUUID().toString()
                    val trip = Trip(
                        tripId = newId, customerId = "$userName ($userPhone)",
                        pickupLocation = Location(pickupGeo!!.latitude, pickupGeo!!.longitude, pickupAddr),
                        dropoffLocation = Location(dropoffGeo!!.latitude, dropoffGeo!!.longitude, dropoffAddr),
                        price = estimatedPrice, status = TripStatus.REQUESTED, vehicleType = selectedVehicle
                    )
                    db.child(newId).setValue(trip)
                    activeTrip = trip
                    step = 3 
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835)), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
