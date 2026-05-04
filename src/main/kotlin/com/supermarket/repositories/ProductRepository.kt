package com.supermarket.repositories

import com.supermarket.database.Products
import com.supermarket.database.Categories
import com.supermarket.database.WarehouseStock
import com.supermarket.models.Product
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object ProductRepository {

    private fun rowToProduct(row: ResultRow, index: Int): Product {
        val quantity = row[WarehouseStock.quantityAvailable]
        val stock = when {
            quantity <= 0 -> "Out of stock"
            quantity <= 10 -> "Low stock"
            else -> "In stock"
        }
        return Product(
            id = index,
            name = row[Products.name],
            category = row[Categories.name],
            price = row[Products.basePrice].toDouble(),
            stock = stock,
            description = row[Products.description] ?: ""
        )
    }

    fun allProducts(): List<Product> = transaction {
        (Products innerJoin Categories innerJoin WarehouseStock)
            .selectAll()
            .mapIndexed { index, row -> rowToProduct(row, index + 1) }
    }

    fun findProduct(productId: Int): Product? = transaction {
        (Products innerJoin Categories innerJoin WarehouseStock)
            .selectAll()
            .mapIndexed { index, row -> rowToProduct(row, index + 1) }
            .find { it.id == productId }
    }
}