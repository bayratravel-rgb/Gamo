package com.bayera.travel.driver

import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.*
import com.bayera.travel.common.models.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val opt = FirebaseOptions.Builder()
                .setApplicationId("1:643765664968:android:656f004fce2ecda85af9fd")
                .setApiKey("AIzaSyCuzSPe6f4JoQYuYS-JskaHT11jKNEuA20")
                .setDatabaseUrl("https://bayera-travel-default-rtdb.europe-west1.firebasedatabase.app")
                .setProjectId("bayera-travel").build()
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, opt)
        } catch (e: Exception) {}
        setContent { MaterialTheme { DriverMasterUI() } }
    }
}

@Composable
fun DriverMasterUI() {
    val db = FirebaseDatabase.getInstance().getReference()
    val driverName = "sffu the driver"
    var balance by remember { mutableStateOf(500.0) }
    var activeJob by remember { mutableStateOf<Trip?>(null) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        db.child("trips").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<Trip>(); var job: Trip? = null
                for (child in s.children) {
                    val t = child.getValue(Trip::class.java)
                    if (t != null) {
                        if (t.driverId == driverName && t.status != TripStatus.COMPLETED) job = t
                        if (t.status == TripStatus.REQUESTED) list.add(t)
                    }
                }
                requests = list.reversed(); activeJob = job
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("drivers").child("0900000000").child("balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 500.0 }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFE8F5E9)).padding(16.dp)) {
        Text("Partner: $driverName", fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("${String.format("%.1f", balance)} ETB", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (activeJob != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE TRIP", fontWeight = FontWeight.Black, color = Color.Red)
                    Row(modifier = Modifier.padding(top=16.dp)) {
                        Button(onClick = { 
                            val uri = "google.navigation:q=6.02,37.55"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps"))
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("NAVIGATE") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val next = if(activeJob!!.status == TripStatus.ACCEPTED) TripStatus.IN_PROGRESS else TripStatus.COMPLETED
                            db.child("trips").child(activeJob!!.tripId).child("status").setValue(next)
                            if (next == TripStatus.COMPLETED) {
                                db.child("drivers").child("0900000000").child("balance").runTransaction(object : Transaction.Handler {
                                    override fun doTransaction(d: MutableData): Transaction.Result {
                                        d.value = (d.getValue(Double::class.java) ?: 0.0) - (activeJob!!.price * 0.10)
                                        return Transaction.success(d)
                                    }
                                    override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
                                })
                            }
                        }, modifier = Modifier.weight(1f)) { Text(if(activeJob!!.status == TripStatus.ACCEPTED) "START" else "FINISH") }
                    }
                }
            }
        } else {
            LazyColumn {
                items(requests) { trip ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("New Ride • ${trip.price} ETB", fontWeight = FontWeight.Bold)
                            Button(onClick = { db.child("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName, "driverName" to driverName)) }, modifier = Modifier.fillMaxWidth()) { Text("Accept") }
                        }
                    }
                }
            }
        }
    }
}
