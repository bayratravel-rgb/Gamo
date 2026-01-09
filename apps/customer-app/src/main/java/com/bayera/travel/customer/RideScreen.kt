package com.bayera.travel.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.firebase.database.FirebaseDatabase
import com.bayera.travel.common.models.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(navController: NavController) {
    var mode by remember { mutableStateOf("PICKUP") } // PICKUP, DEST, SUMMARY, SEARCHING
    val mapState = remember { mutableStateOf<MapView?>(null) }
    var pickupLoc by remember { mutableStateOf(GeoPoint(6.022, 37.559)) }
    var destLoc by remember { mutableStateOf(GeoPoint(6.022, 37.559)) }
    val db = FirebaseDatabase.getInstance().getReference("trips")

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                // FIX TILING: Limit zoom so it doesn't show the world
                minZoomLevel = 10.0
                controller.setZoom(16.0)
                controller.setCenter(pickupLoc)
                mapState.value = this
            }
        }, modifier = Modifier.fillMaxSize())

        if (mode != "SEARCHING") {
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) {
                Icon(Icons.Default.ArrowBack, null)
            }
        }

        if (mode == "PICKUP" || mode == "DEST") {
            Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode == "PICKUP") Color(0xFF2E7D32) else Color.Red)
        }

        Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (mode) {
                    "PICKUP" -> {
                        Text("Start Trip From?", color = Color.Gray)
                        Text("Arba Minch Center", fontWeight = FontWeight.Bold)
                        Button(onClick = { pickupLoc = mapState.value?.mapCenter as GeoPoint; mode = "DEST" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Set Pickup Here") }
                    }
                    "DEST" -> {
                        Text("Where to?", color = Color.Gray)
                        Text("Move map to Destination", fontWeight = FontWeight.Bold)
                        Button(onClick = { destLoc = mapState.value?.mapCenter as GeoPoint; mode = "SUMMARY" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Set Destination Here") }
                    }
                    "SUMMARY" -> {
                        Text("Trip Summary", fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Arba Minch Center → University", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val id = UUID.randomUUID().toString()
                            val trip = Trip(tripId = id, customerPhone = "user_gg", price = 110.0, status = TripStatus.REQUESTED)
                            db.child(id).setValue(trip)
                            mode = "SEARCHING"
                        }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) {
                            Text("BOOK RIDE • 110 ETB", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    "SEARCHING" -> {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                        Text("Finding your driver...", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
                        TextButton(onClick = { mode = "PICKUP" }) { Text("Cancel Request", color = Color.Gray) }
                    }
                }
            }
        }
    }
}
