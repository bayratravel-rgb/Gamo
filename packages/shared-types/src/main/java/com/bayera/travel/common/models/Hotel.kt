package com.bayera.travel.common.models

data class Hotel(
    val id: String,
    val name: String,
    val description: String,
    val pricePerNight: Double,
    val rating: Double,
    val imageUrl: String, // URL to photo
    val location: String, // e.g., "Arba Minch, Secha"
    val amenities: List<String> = emptyList() // e.g., ["Wifi", "Pool", "Gym"]
)

// Mock Data for Arba Minch Hotels
object HotelData {
    val list = listOf(
        Hotel("H01", "Haile Resort", "Luxury resort with lake view.", 4500.0, 4.8, "https://cf.bstatic.com/xdata/images/hotel/max1024x768/12345678.jpg", "Arba Minch", listOf("Pool", "Spa", "Wifi")),
        Hotel("H02", "Paradise Lodge", "Beautiful traditional style lodge.", 3800.0, 4.7, "https://example.com/paradise.jpg", "Arba Minch", listOf("Bar", "View", "Breakfast")),
        Hotel("H03", "Mora Heights", "Affordable comfort with city view.", 1500.0, 4.2, "https://example.com/mora.jpg", "Secha", listOf("Wifi", "Parking")),
        Hotel("H04", "Derik Hotel", "Modern rooms in the city center.", 2200.0, 4.3, "https://example.com/derik.jpg", "Sikela", listOf("Restaurant", "Wifi")),
        Hotel("H05", "Saro Lodge", "Eco-friendly lodge near the park.", 3000.0, 4.5, "https://example.com/saro.jpg", "Nech Sar", listOf("Hiking", "Eco")),
        Hotel("H06", "Lewi Resort", "Family friendly resort with pool.", 2800.0, 4.4, "https://example.com/lewi.jpg", "Arba Minch", listOf("Pool", "Kids Area"))
    )
}
