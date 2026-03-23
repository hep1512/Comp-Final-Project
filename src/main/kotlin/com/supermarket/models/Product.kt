package com.supermarket.models

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    var stock: String,
    val description: String
)