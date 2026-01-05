package com.bayera.travel.driver
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.bayera.travel.common.models.Trip
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val test = Trip(tripId = "test")
        setContent { Text("Driver App Loaded: ${test.tripId}") }
    }
}
