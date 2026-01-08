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
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.bayera.travel.common.models.*
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "BayeraTravel"
        try { FirebaseApp.initializeApp(this) } catch (e: Exception) {}
        setContent { MaterialTheme { CustomerRideExperience() } }
    }
}

@Composable
fun CustomerRideExperience() {
    val db = FirebaseDatabase.getInstance().getReference("trips")
    var selectionMode by remember { mutableStateOf("PICKUP") } // PICKUP, DEST, SUMMARY, ACTIVE
    var pickupPoint by remember { mutableStateOf(GeoPoint(6.02, 37.55)) }
    var destPoint by remember { mutableStateOf(GeoPoint(6.02, 37.55)) }
    var selectedVehicle by remember { mutableStateOf("COMFORT") }
    var driverNote by remember { mutableStateOf("") }
    
    val mapState = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 🧭 MAP ENGINE ---
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

        // --- 🎯 CENTER PIN (GREEN FOR PICKUP / RED FLAG FOR DEST) ---
        if (selectionMode == "PICKUP" || selectionMode == "DEST") {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if(selectionMode == "PICKUP") Icons.Default.MyLocation else Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).offset(y = (-20).dp),
                    tint = if(selectionMode == "PICKUP") Color(0xFF2E7D32) else Color.Red
                )
            }
        }

        // --- 🧭 RE-CENTER / COMPASS BUTTON ---
        FloatingActionButton(
            onClick = { mapState.value?.controller?.animateTo(GeoPoint(6.0227, 37.5597)) },
            modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp).offset(y = (-40).dp),
            containerColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Explore, contentDescription = "Locate", tint = Color(0xFF1976D2))
        }

        // --- 💳 DYNAMIC BOTTOM CARDS ---
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
            when (selectionMode) {
                "PICKUP" -> SelectionUI("Start Trip From?", "Set Pickup Here", Color(0xFF2E7D32)) {
                    pickupPoint = mapState.value?.mapCenter as GeoPoint
                    selectionMode = "DEST"
                }
                "DEST" -> SelectionUI("Where to?", "Set Destination Here", Color.Red) {
                    destPoint = mapState.value?.mapCenter as GeoPoint
                    selectionMode = "SUMMARY"
                }
                "SUMMARY" -> SummaryUI(selectedVehicle, { selectedVehicle = it }, driverNote, { driverNote = it }) {
                    val id = UUID.randomUUID().toString()
                    db.child(id).setValue(Trip(tripId = id, customerPhone = "user_bb", pickupLocation = Location(pickupPoint.latitude, pickupPoint.longitude, "Arba Minch"), dropoffLocation = Location(destPoint.latitude, destPoint.longitude, "Arba Minch"), vehicleType = selectedVehicle, notes = driverNote, price = 110.0))
                    selectionMode = "ACTIVE"
                }
                "ACTIVE" -> TripActiveUI()
            }
        }
    }
}

@Composable
fun SelectionUI(title: String, btnText: String, color: Color, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = Color.Gray, fontSize = 14.sp)
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(27.dp)
            ) { Text(btnText, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryUI(selected: String, onSelect: (String) -> Unit, note: String, onNote: (String) -> Unit, onBook: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Trip Summary", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("COMFORT", "LUXURY", "POOL").forEach { type ->
                    FilterChip(selected = selected == type, onClick = { onSelect(type) }, label = { Text(type) })
                }
            }
            OutlinedTextField(value = note, onValueChange = onNote, label = { Text("Notes for Driver (Accuracy)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBook, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) {
                Text("BOOK RIDE • 110 ETB", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TripActiveUI() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF2E7D32))
            Text("Finding your driver...", modifier = Modifier.padding(top = 16.dp), fontWeight = FontWeight.Bold)
        }
    }
}
