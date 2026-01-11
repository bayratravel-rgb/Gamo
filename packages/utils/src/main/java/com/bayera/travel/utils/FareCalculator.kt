package com.bayera.travel.utils
import com.bayera.travel.common.models.VehicleType
import kotlin.math.*

object FareCalculator {
    private const val BENZINE_PRICE = 300.0 

    fun calculatePrice(distanceKm: Double, vehicleType: VehicleType): Double {
        val baseFare = if (vehicleType == VehicleType.CODE_3) 100.0 else 40.0
        val fuelConsumption = if (vehicleType == VehicleType.CODE_3) 10.0 else 25.0
        val fuelCostPerKm = BENZINE_PRICE / fuelConsumption
        
        val total = baseFare + (distanceKm * fuelCostPerKm * 1.5)
        return (Math.round(total / 5.0) * 5.0).toDouble()
    }
}
