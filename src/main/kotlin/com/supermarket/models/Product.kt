package com.supermarket.models

// Product data structure
data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    var stock: String,
    val description: String,
    val imageUrl: String = "", // URL to product image, empty string if none
)
