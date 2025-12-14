package com.bayera.travel.common.models

data class Trip(
    val tripId: String,
    val customerId: String,
    val driverId: String? = null,
    val pickupLocation: Location,
    val dropoffLocation: Location,
    val status: TripStatus = TripStatus.REQUESTED,
    val price: Double,
    val estimatedTime: Int // minutes
)

data class Location(
    val lat: Double,
    val lng: Double,
    val address: String
)

enum class TripStatus {
    REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}
