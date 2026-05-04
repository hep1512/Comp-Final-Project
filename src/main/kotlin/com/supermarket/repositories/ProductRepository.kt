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
    fun addProduct(name: String, description: String, categorySlug: String, price: Double, sku: String) = transaction {
    // Find category by slug
    val categoryRow = Categories.selectAll()
        .map { it }
        .firstOrNull { it[Categories.slug] == categorySlug } ?: return@transaction

    val categoryId = categoryRow[Categories.id].value
    val warehouseRow = WarehouseStock.selectAll().map { it }.firstOrNull()

    Products.insert {
        it[Products.name] = name
        it[Products.description] = description
        it[Products.categoryId] = categoryId
        it[Products.basePrice] = price.toBigDecimal()
        it[Products.sku] = sku
        it[Products.isActive] = true
        it[Products.createdAt] = java.time.OffsetDateTime.now()
    }

    // Add stock for this product in the warehouse
    val newProductRow = Products.selectAll()
        .map { it }
        .firstOrNull { it[Products.sku] == sku } ?: return@transaction

    val warehouseId = Categories.selectAll().map { it }.firstOrNull()
        ?.let {
            org.jetbrains.exposed.sql.transactions.transaction {
                com.supermarket.database.Warehouses.selectAll()
                    .map { it[com.supermarket.database.Warehouses.id].value }
                    .firstOrNull()
            }
        }

    if (warehouseId != null) {
        WarehouseStock.insert {
            it[WarehouseStock.warehouseId] = warehouseId
            it[WarehouseStock.productId] = newProductRow[Products.id].value
            it[WarehouseStock.quantityAvailable] = 100
            it[WarehouseStock.quantityReserved] = 0
            it[WarehouseStock.lowStockThreshold] = 10
        }
    }
}
    fun findProduct(productId: Int): Product? = transaction {
        (Products innerJoin Categories innerJoin WarehouseStock)
            .selectAll()
            .mapIndexed { index, row -> rowToProduct(row, index + 1) }
            .find { it.id == productId }
        
    }
}