package com.supermarket.models

// Data class definiton
data class OrderSummary(
    val orderId: String,
    val date: String,
    val status: String,
    val total: Double,
)
