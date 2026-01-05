package com.bayera.travel.common.models

data class Trip(
    val tripId: String = "",
    val customerId: String = "",
    val customerPhone: String = "",
    val driverId: String? = null,
    val driverName: String? = null,
    val pickupLocation: Location = Location(),
    val dropoffLocation: Location = Location(),
    val price: Double = 0.0,
    val status: TripStatus = TripStatus.REQUESTED,
    val paymentMethod: String = "CASH", // "CASH" or "WALLET"
    val paymentStatus: String = "PENDING",
    val notes: String = ""
)

data class Location(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = ""
)

enum class TripStatus {
    REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}
