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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.bayera.travel.common.models.*

@Composable
fun RideScreen(navController: NavController) {
    var mode by remember { mutableStateOf("PICKUP") }
    val mapState = remember { mutableStateOf<MapView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true 
                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(6.0206, 37.5557))
                mapState.value = this
            }
        }, modifier = Modifier.fillMaxSize())

        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp).background(Color.White, CircleShape)) {
            Icon(Icons.Default.ArrowBack, null)
        }

        Icon(Icons.Default.LocationOn, null, modifier = Modifier.align(Alignment.Center).size(45.dp).offset(y = (-22).dp), tint = if(mode == "PICKUP") Color(0xFF2E7D32) else Color.Red)

        Card(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (mode == "PICKUP") {
                    Text("Confirm Pickup", fontWeight = FontWeight.Bold)
                    Button(onClick = { mode = "DEST" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Set Pickup Here") }
                } else if (mode == "DEST") {
                    Text("Where to?", fontWeight = FontWeight.Bold)
                    Button(onClick = { mode = "SUMMARY" }, modifier = Modifier.fillMaxWidth().padding(top=12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Set Destination Here") }
                } else {
                    Text("Trip Summary", fontWeight = FontWeight.Bold)
                    Text("Fare: 110.0 ETB", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth().padding(top=16.dp).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600))) { Text("BOOK RIDE", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
