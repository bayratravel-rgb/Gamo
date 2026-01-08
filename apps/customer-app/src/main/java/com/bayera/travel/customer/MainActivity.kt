package com.bayera.travel.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BayeraApp"
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerRideExperience() } }
    }
}

@Composable
fun CustomerRideExperience() {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var selectionMode by remember { mutableStateOf("PICKUP") } // PICKUP, DEST, SUMMARY, ACTIVE
    var pickupPoint by remember { mutableStateOf(GeoPoint(6.02, 37.55)) }
    var destPoint by remember { mutableStateOf(GeoPoint(6.03, 37.56)) }
    var selectedVehicle by remember { mutableStateOf("COMFORT") }
    var driverNote by remember { mutableStateOf("") }
    
    val mapState = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🧭 REAL MAP ENGINE
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(16.0)
                    controller.setCenter(pickupPoint)
                    mapState.value = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 🎯 CENTRAL PIN INDICATOR (Visual Only)
        if (selectionMode == "PICKUP" || selectionMode == "DEST") {
            Icon(
                Icons.Default.LocationOn, 
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(40.dp).offset(y = (-20).dp),
                tint = if(selectionMode == "PICKUP") Color(0xFF2E7D32) else Color.Red
            )
        }

        // 🧭 COMPASS / LOCATE ME BUTTON
        SmallFloatingActionButton(
            onClick = { mapState.value?.controller?.animateTo(GeoPoint(6.02, 37.55)) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, bottom = 100.dp),
            containerColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Locate Me", tint = Color(0xFF1976D2))
        }

        // 💳 DYNAMIC BOTTOM CARDS
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
            when (selectionMode) {
                "PICKUP" -> SelectionCard("Start Trip From?", "Arba Minch Center", "Set Pickup Here", Color(0xFF2E7D32)) {
                    pickupPoint = mapState.value?.mapCenter as GeoPoint
                    selectionMode = "DEST"
                }
                "DEST" -> SelectionCard("Where to?", "Arba Minch Area", "Set Destination Here", Color.Red) {
                    destPoint = mapState.value?.mapCenter as GeoPoint
                    selectionMode = "SUMMARY"
                }
                "SUMMARY" -> SummaryCard(selectedVehicle, { selectedVehicle = it }, driverNote, { driverNote = it }) {
                    val id = UUID.randomUUID().toString()
                    val trip = Trip(tripId = id, customerPhone = "user_bb", pickupLocation = Location(pickupPoint.latitude, pickupPoint.longitude, "Pickup"), dropoffLocation = Location(destPoint.latitude, destPoint.longitude, "Destination"), vehicleType = selectedVehicle, notes = driverNote, price = 110.0)
                    db.child(id).setValue(trip)
                    selectionMode = "ACTIVE"
                }
                "ACTIVE" -> Text("Searching for Driver...", modifier = Modifier.background(Color.White).padding(16.dp))
            }
        }
    }
}

@Composable
fun SelectionCard(title: String, subtitle: String, btnText: String, btnColor: Color, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = Color.Gray, fontSize = 14.sp)
            Text(subtitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                shape = RoundedCornerShape(25.dp)
            ) { Text(btnText) }
        }
    }
}

@Composable
fun SummaryCard(selected: String, onSelect: (String) -> Unit, note: String, onNote: (String) -> Unit, onBook: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Trip Summary", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("COMFORT", "LUXURY", "POOL").forEach { type ->
                    FilterChip(selected = selected == type, onClick = { onSelect(type) }, label = { Text(type) })
                }
            }
            OutlinedTextField(value = note, onValueChange = onNote, label = { Text("Notes for accuracy (Gate, Color, etc.)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Price")
                Text("110.0 ETB", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 22.sp)
            }
            Button(onClick = onBook, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) {
                Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
