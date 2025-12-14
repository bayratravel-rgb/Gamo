package com.bayera.travel.common.models

data class Hotel(
    val hotelId: String,
    val name: String,
    val location: Location,
    val rating: Float,
    val imageUrl: String,
    val pricePerNight: Double,
    val amenities: List<String>
)
