package com.bayera.travel.customer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bayera.travel.common.models.*
import com.bayera.travel.utils.FareCalculator
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(navController: NavController) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("PICKUP") }
    var pickupGeo by remember { mutableStateOf(GeoPoint(6.0206, 37.5557)) }
    var destGeo by remember { mutableStateOf(GeoPoint(6.0206, 37.5557)) }
    var selectedVehicle by remember { mutableStateOf(VehicleType.COMFORT) }
    val mapState = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true // YOUR HD HACK 🚀
                controller.setZoom(16.0)
                controller.setCenter(pickupGeo)
                mapState.value = this
            }
        }, modifier = Modifier.fillMaxSize())

        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) {
            Icon(Icons.Default.ArrowBack, null)
        }

        if (mode != "SUMMARY") {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode=="PICKUP") Color(0xFF2E7D32) else Color.Red)
        }

        Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (mode == "PICKUP") {
                    Text("Confirm Pickup", fontWeight = FontWeight.Bold)
                    Button(onClick = { pickupGeo = mapState.value?.mapCenter as GeoPoint; mode = "DEST" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Set Pickup Here") }
                } else if (mode == "DEST") {
                    Text("Where to?", fontWeight = FontWeight.Bold)
                    Button(onClick = { destGeo = mapState.value?.mapCenter as GeoPoint; mode = "SUMMARY" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Set Destination Here") }
                } else {
                    Text("Trip Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Fare: 110.0 ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Button(onClick = { /* Push to Firebase */ }, modifier = Modifier.fillMaxWidth().padding(top=16.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) { Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
