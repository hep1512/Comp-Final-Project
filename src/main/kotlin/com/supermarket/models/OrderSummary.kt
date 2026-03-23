package com.supermarket.models

data class OrderSummary(
    val orderId: String,
    val date: String,
    val status: String,
    val total: Double
)