package com.bayera.travel.common.models

data class User(
    val id: String,
    val phoneNumber: String,
    val name: String,
    val email: String? = null,
    val profilePictureUrl: String? = null,
    val role: UserRole = UserRole.CUSTOMER
)

enum class UserRole {
    CUSTOMER, DRIVER, COURIER, HOTEL_ADMIN
}
