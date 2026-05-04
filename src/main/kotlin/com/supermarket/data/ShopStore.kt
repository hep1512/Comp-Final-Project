package com.supermarket.data

import com.supermarket.models.CartLine
import com.supermarket.models.OrderSummary
import com.supermarket.models.Product
import com.supermarket.repositories.ProductRepository

private val cartsByUser = mutableMapOf<String, MutableList<CartLine>>()
private val ordersByUser = mutableMapOf<String, MutableList<OrderSummary>>()

fun allProducts(): List<Product> = ProductRepository.allProducts()

fun findProduct(productId: Int): Product? = ProductRepository.findProduct(productId)

fun getCart(username: String): MutableList<CartLine> {
    return cartsByUser.getOrPut(username) { mutableListOf() }
}

fun getOrders(username: String): MutableList<OrderSummary> {
    return ordersByUser.getOrPut(username) { mutableListOf() }
}

fun addProductToCart(username: String, productId: Int, quantity: Int) {
    if (quantity <= 0) return
    val cart = getCart(username)
    val existing = cart.find { it.productId == productId }
    if (existing != null) {
        existing.quantity += quantity
    } else {
        cart.add(CartLine(productId, quantity))
    }
}

fun updateCartQuantity(username: String, productId: Int, quantity: Int) {
    val cart = getCart(username)
    val item = cart.find { it.productId == productId } ?: return
    if (quantity <= 0) {
        cart.removeIf { it.productId == productId }
    } else {
        item.quantity = quantity
    }
}

fun removeFromCart(username: String, productId: Int) {
    val cart = getCart(username)
    cart.removeIf { it.productId == productId }
}

fun checkoutCart(username: String) {
    val cart = getCart(username)
    if (cart.isEmpty()) return
    val total = cart.sumOf { line ->
        val product = findProduct(line.productId) ?: return@sumOf 0.0
        product.price * line.quantity
    }
    val orders = getOrders(username)
    val nextId = "ORD-" + (1000 + orders.size + 1)
    orders.add(
        OrderSummary(
            orderId = nextId,
            date = "2026-03-23",
            status = "Placed",
            total = total
        )
    )
    cart.clear()
}

fun updateProductStock(productId: Int, newStock: String) {
}

fun totalOrdersCount(): Int = ordersByUser.values.sumOf { it.size }

fun totalSalesAmount(): Double = ordersByUser.values.flatten().sumOf { it.total }
fun totalProductsCount(): Int = ProductRepository.allProducts().size

fun lowStockCount(): Int = ProductRepository.allProducts().count { it.stock == "Low stock" }

fun outOfStockCount(): Int = ProductRepository.allProducts().count { it.stock == "Out of stock" }