package com.bayera.travel.utils

import com.bayera.travel.common.models.VehicleType
import kotlin.math.*

object FareCalculator {
    
    // BASE FARES (Starting Price)
    private fun getBaseFare(type: VehicleType): Double {
        return when (type) {
            VehicleType.BAJAJ -> 30.0
            VehicleType.POOL -> 20.0
            VehicleType.COMFORT -> 100.0
            VehicleType.LUXURY -> 250.0
        }
    }

    // COST PER KM (Fuel + Profit)
    private fun getRatePerKm(type: VehicleType): Double {
        return when (type) {
            VehicleType.BAJAJ -> 25.0
            VehicleType.POOL -> 15.0
            VehicleType.COMFORT -> 50.0
            VehicleType.LUXURY -> 150.0 // Expensive fuel!
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun calculatePrice(distanceKm: Double, type: VehicleType): Double {
        val price = getBaseFare(type) + (distanceKm * getRatePerKm(type))
        // Round to nearest 10
        return (ceil(price / 10) * 10)
    }
}
