package com.bayera.travel.common.models
data class Trip(
    val tripId: String = "",
    val customerPhone: String = "",
    val driverName: String? = null,
    val driverId: String? = null,
    val vehicleType: String = "COMFORT",
    val pickupLocation: Location = Location(),
    val dropoffLocation: Location = Location(),
    val price: Double = 110.0,
    val status: TripStatus = TripStatus.REQUESTED,
    val paymentMethod: String = "CASH",
    val notes: String = ""
)
data class Location(val lat: Double = 0.0, val lng: Double = 0.0, val address: String = "")
enum class TripStatus { REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
