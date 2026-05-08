package com.supermarket.repositories

import com.supermarket.database.Addresses
import com.supermarket.database.DbOrderItems
import com.supermarket.database.DbOrders
import com.supermarket.database.Products
import com.supermarket.database.Users
import com.supermarket.models.CartLine
import com.supermarket.models.OrderSummary
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object OrderRepository {
    fun getOrders(username: String): List<OrderSummary> =
        transaction {
            // Find user id by username
            val userId =
                Users.selectAll()
                    .map { Pair(it[Users.firstName], it[Users.id].value) }
                    .firstOrNull { it.first == username }?.second ?: return@transaction emptyList()

            DbOrders.selectAll()
                .map { it }
                .filter { it[DbOrders.userId].value == userId }
                .map { row ->
                    val orderId = row[DbOrders.id].value.toString().take(8).uppercase()
                    val date =
                        row[DbOrders.placedAt]?.format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                        ) ?: "Unknown"
                    OrderSummary(
                        orderId = "ORD-$orderId",
                        date = date,
                        status = row[DbOrders.status],
                        total = row[DbOrders.totalAmount].toDouble(),
                    )
                }
        }

    fun checkout(
        username: String,
        cart: List<CartLine>,
    ) = transaction {
        if (cart.isEmpty()) return@transaction

        // Find user
        val userRow =
            Users.selectAll()
                .map { it }
                .firstOrNull { it[Users.firstName] == username } ?: return@transaction

        val userId = userRow[Users.id].value

        // Get or create a default address for this user
        val existingAddress =
            Addresses.selectAll()
                .map { it }
                .firstOrNull { it[Addresses.userId].value == userId }

        val addressId =
            if (existingAddress != null) {
                existingAddress[Addresses.id].value
            } else {
                Addresses.insertAndGetId {
                    it[Addresses.userId] = userId
                    it[line1] = "123 Default Street"
                    it[city] = "London"
                    it[postcode] = "SW1A 1AA"
                    it[isDefault] = true
                }.value
            }

        // Get all products to map Int id to UUID
        val allProducts = Products.selectAll().map { it }

        val calculatedTotal =
            cart.sumOf { line ->
                val productRow = allProducts.getOrNull(line.productId - 1)
                val price = productRow?.get(Products.basePrice)?.toDouble() ?: 0.0
                price * line.quantity
            }

        // Insert order
        val orderId =
            DbOrders.insertAndGetId {
                it[DbOrders.userId] = userId
                it[DbOrders.addressId] = addressId
                it[status] = "placed"
                it[totalAmount] = BigDecimal.valueOf(calculatedTotal)
                it[placedAt] = OffsetDateTime.now()
            }.value

        // Insert order items
        cart.forEach { line ->
            val productRow = allProducts.getOrNull(line.productId - 1) ?: return@forEach
            val productId = productRow[Products.id].value
            val unitPrice = productRow[Products.basePrice]

            DbOrderItems.insert {
                it[DbOrderItems.orderId] = orderId
                it[DbOrderItems.productId] = productId
                it[quantity] = line.quantity
                it[DbOrderItems.unitPrice] = unitPrice
            }
        }
    }

    fun totalOrdersCount(): Int =
        transaction {
            DbOrders.selectAll().count().toInt()
        }

    fun totalSalesAmount(): Double =
        transaction {
            DbOrders.selectAll()
                .sumOf { it[DbOrders.totalAmount].toDouble() }
        }
}
