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
    val paymentStatus: String = "PENDING" 
)

data class Location(val lat: Double = 0.0, val lng: Double = 0.0, val address: String = "")
enum class TripStatus { REQUESTED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }
enum class VehicleType { BAJAJ, COMFORT, LUXURY, POOL }

data class Delivery(
    val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val restaurantName: String = "",
    val items: String = "",
    val price: Double = 0.0,
    val location: String = "",
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val driverId: String? = null
)
enum class DeliveryStatus { PENDING, ACCEPTED, PICKED_UP, DELIVERED, CANCELLED }

data class Hotel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val pricePerNight: Double = 0.0,
    val rating: Double = 0.0,
    val imageUrl: String = "",
    val location: String = "",
    val amenities: List<String> = emptyList()
)
// Mock Data for Hotels
object HotelData {
    val list = listOf(
        Hotel("H01", "Haile Resort", "Luxury resort with lake view.", 4500.0, 4.8, "https://cf.bstatic.com/xdata/images/hotel/max1024x768/12345678.jpg", "Arba Minch"),
        Hotel("H02", "Paradise Lodge", "Traditional lodge.", 3800.0, 4.7, "https://example.com/paradise.jpg", "Arba Minch"),
        Hotel("H03", "Saro Lodge", "Eco-friendly lodge.", 3000.0, 4.5, "https://example.com/saro.jpg", "Nech Sar")
    )
}

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val category: String = "General",
    val imageUrl: String = ""
)
