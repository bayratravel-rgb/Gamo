package com.bayera.travel.common.models

data class Delivery(
    val id: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val restaurantName: String = "",
    val items: String = "", // e.g., "2 Burgers, 1 Coke"
    val price: Double = 0.0,
    val location: String = "",
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val driverId: String? = null
)

enum class DeliveryStatus {
    PENDING, ACCEPTED, PICKED_UP, DELIVERED, CANCELLED
}
