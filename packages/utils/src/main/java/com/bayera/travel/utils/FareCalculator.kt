package com.bayera.travel.utils

import kotlin.math.*

object FareCalculator {
    private const val BASE_FARE = 50.0 
    private const val RATE_PER_KM = 30.0 
    private const val MINIMUM_FARE = 100.0

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun calculatePrice(distanceKm: Double): Double {
        val rawPrice = BASE_FARE + (distanceKm * RATE_PER_KM)
        val finalPrice = if (rawPrice < MINIMUM_FARE) MINIMUM_FARE else rawPrice
        return (finalPrice / 10).roundToInt() * 10.0
    }
}
