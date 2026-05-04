package com.supermarket.data

import com.supermarket.models.CartLine
import com.supermarket.models.OrderSummary
import com.supermarket.models.Product
import com.supermarket.repositories.ProductRepository
import com.supermarket.repositories.OrderRepository
import java.security.MessageDigest

private val cartsByUser = mutableMapOf<String, MutableList<CartLine>>()

fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun allProducts(): List<Product> = ProductRepository.allProducts()

fun findProduct(productId: Int): Product? = ProductRepository.findProduct(productId)

fun getCart(username: String): MutableList<CartLine> {
    return cartsByUser.getOrPut(username) { mutableListOf() }
}

fun getOrders(username: String): List<OrderSummary> = OrderRepository.getOrders(username)

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
    OrderRepository.checkout(username, cart)
    cart.clear()
}

fun updateProductStock(productId: Int, newStock: String) {
    // Now handled via database
}

fun totalOrdersCount(): Int = OrderRepository.totalOrdersCount()

fun totalSalesAmount(): Double = OrderRepository.totalSalesAmount()

fun totalProductsCount(): Int = ProductRepository.allProducts().size

fun lowStockCount(): Int = ProductRepository.allProducts().count { it.stock == "Low stock" }

fun outOfStockCount(): Int = ProductRepository.allProducts().count { it.stock == "Out of stock" }