package com.bayera.travel.common.models

data class Trip(
    val tripId: String = "",
    val customerId: String = "",
    val driverId: String? = null,
    val pickupLocation: Location = Location(),
    val dropoffLocation: Location = Location(),
    val status: TripStatus = TripStatus.REQUESTED,
    val price: Double = 0.0,
    val estimatedTime: Int = 0,
    val pickupNotes: String = "",
    val vehicleType: VehicleType = VehicleType.BAJAJ,
    // --- NEW FIELD ---
    val paymentStatus: String = "PENDING" // PENDING, PAID_CASH, PAID_WALLET
)

data class Location(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = ""
)

enum class TripStatus {
    REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
}

enum class VehicleType {
    BAJAJ, COMFORT, LUXURY, POOL
}
