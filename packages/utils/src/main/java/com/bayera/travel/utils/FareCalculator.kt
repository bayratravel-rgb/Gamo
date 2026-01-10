package com.bayera.travel.utils
import com.bayera.travel.common.models.VehicleType
import kotlin.math.*
object FareCalculator {
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
    fun calculatePrice(dist: Double, type: VehicleType): Double {
        val base = when(type) { VehicleType.BAJAJ -> 50.0; VehicleType.COMFORT -> 80.0; VehicleType.LUXURY -> 150.0; VehicleType.POOL -> 40.0 }
        return base + (dist * 15.0)
    }
}
