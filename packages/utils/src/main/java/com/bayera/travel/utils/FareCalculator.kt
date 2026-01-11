package com.bayera.travel.utils

object FareCalculator {
    private const val BENZINE_PRICE = 300.0 // ETB per Liter

    fun calculatePrice(distanceKm: Double, vehicleType: String): Double {
        val baseFare = if (vehicleType == "CODE_3") 100.0 else 40.0
        
        // Estimated Fuel Consumption
        // Code 3: ~10km per Liter -> 30 ETB/km fuel cost
        // Bajaj: ~25km per Liter -> 12 ETB/km fuel cost
        val fuelMultiplier = if (vehicleType == "CODE_3") (BENZINE_PRICE / 10) else (BENZINE_PRICE / 25)
        
        val total = baseFare + (distanceKm * fuelMultiplier * 1.5) // 1.5 multiplier for driver profit
        return (Math.round(total / 5.0) * 5.0).toDouble() // Round to nearest 5 ETB
    }
}
