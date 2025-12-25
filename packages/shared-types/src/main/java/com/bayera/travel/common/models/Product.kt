package com.bayera.travel.common.models

data class Product(
    val id: String = "",
    val merchantId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "General", // Food, Clothes, Electronics
    val imageUrl: String = "",
    val stock: Int = 0
)

data class Merchant(
    val id: String = "",
    val name: String = "",
    val type: String = "Shop" // Shop, Restaurant, Supermarket
)
