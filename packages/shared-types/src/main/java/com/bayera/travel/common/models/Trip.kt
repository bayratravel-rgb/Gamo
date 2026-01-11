package com.bayera.travel.common.models
enum class VehicleType { BAJAJ, CODE_3 }
data class Trip(
    val tripId: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerName: String = "",
    val driverName: String? = null,
    val vehicleType: VehicleType = VehicleType.BAJAJ,
    val status: TripStatus = TripStatus.REQUESTED,
    val price: Double = 0.0,
    val pickupLocation: Location = Location(),
    val dropoffLocation: Location = Location(),
    val notes: String = ""
)
data class Location(val lat: Double = 0.0, val lng: Double = 0.0, val address: String = "")
enum class TripStatus { REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
