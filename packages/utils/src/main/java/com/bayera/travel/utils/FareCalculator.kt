package com.bayera.travel.utils

import com.bayera.travel.common.models.VehicleType
import kotlin.math.*

object FareCalculator {
    private const val BENZINE_PRICE = 300.0 

    // THE MISSING FUNCTION RESTORED 🛰️
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun calculatePrice(distanceKm: Double, type: VehicleType): Double {
        val base = if (type == VehicleType.CODE_3) 100.0 else 40.0
        val fuelUsage = if (type == VehicleType.CODE_3) 10.0 else 25.0
        val total = base + (distanceKm * (BENZINE_PRICE / fuelUsage) * 1.5)
        return (Math.round(total / 5.0) * 5.0).toDouble()
    }
}
