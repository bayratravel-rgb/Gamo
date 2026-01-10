package com.bayera.travel.common.models
data class Trip(
    val tripId: String = "",
    val customerPhone: String = "",
    val driverName: String? = null,
    val vehicleType: VehicleType = VehicleType.COMFORT,
    val status: TripStatus = TripStatus.REQUESTED,
    val price: Double = 110.0,
    val notes: String = ""
)
data class Location(val lat: Double = 0.0, val lng: Double = 0.0, val address: String = "")
enum class TripStatus { REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class VehicleType { COMFORT, LUXURY, POOL }
