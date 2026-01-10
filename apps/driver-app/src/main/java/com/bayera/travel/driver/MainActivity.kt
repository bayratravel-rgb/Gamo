package com.bayera.travel.driver

import android.content.*
import android.os.*
import android.net.Uri
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
        setContent { MaterialTheme { DriverProfessionalUI() } }
    }
}

@Composable
fun DriverProfessionalUI() {
    val context = LocalContext.current
    val db = FirebaseDatabase.getInstance().getReference()
    var balance by remember { mutableStateOf(0.0) }
    var requests by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var activeJob by remember { mutableStateOf<Trip?>(null) }
    val driverName = "sffu the driver"

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
                requests = list; activeJob = job
            }
            override fun onCancelled(e: DatabaseError) {}
        })
        db.child("drivers").child("0900000000").child("balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) { balance = s.getValue(Double::class.java) ?: 0.0 }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Rides") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Earnings") }, selected = false, onClick = {})
            }
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize().background(Color(0xFFE8F5E9)).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Hi, $driverName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f)); Text("Logout", color = Color.Red, modifier = Modifier.clickable { })
            }
            Text("Incoming Rides", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp))

            if (activeJob != null) {
                ActiveJobCard(activeJob!!, db, context)
            } else {
                LazyColumn {
                    items(requests) { trip ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = Color(0xFF1976D2))
                                    Text(" Customer Request", fontWeight = FontWeight.Bold)
                                }
                                Text("${trip.price} ETB", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.padding(top=16.dp)) {
                                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("DECLINE", color = Color.Red) }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { db.child("trips").child(trip.tripId).updateChildren(mapOf("status" to "ACCEPTED", "driverId" to driverName, "driverName" to driverName)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("ACCEPT") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveJobCard(trip: Trip, db: DatabaseReference, context: Context) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CURRENT TRIP", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            Text("To: Arba Minch Center")
            Text("Collect: ${trip.price} ETB", fontWeight = FontWeight.Bold)
            Button(onClick = { 
                val uri = "google.navigation:q=6.02,37.55"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps"))
            }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))) { Text("NAVIGATE") }
            Button(onClick = {
                db.child("trips").child(trip.tripId).child("status").setValue(TripStatus.COMPLETED)
                db.child("drivers").child("0900000000").child("balance").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(d: MutableData): Transaction.Result {
                        d.value = (d.getValue(Double::class.java) ?: 0.0) - (trip.price * 0.10)
                        return Transaction.success(d)
                    }
                    override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {}
                })
            }, modifier = Modifier.fillMaxWidth().padding(top=8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("CASH COLLECTED") }
        }
    }
}
