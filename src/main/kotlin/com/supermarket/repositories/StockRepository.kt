package com.supermarket.repositories

import com.supermarket.database.Products
import com.supermarket.database.WarehouseStock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object StockRepository {

    fun updateStock(productIndex: Int, newStock: String) = transaction {
        // Get product by index (same way ProductRepository does it)
        val allProducts = Products.selectAll().map { it }
        val productRow = allProducts.getOrNull(productIndex - 1) ?: return@transaction
        val productId = productRow[Products.id].value

        // Convert stock string to quantity
        val newQuantity = when (newStock) {
            "Out of stock" -> 0
            "Low stock"    -> 5
            "In stock"     -> 100
            else           -> return@transaction
        }

        WarehouseStock.update({ WarehouseStock.productId eq productId }) {
            it[quantityAvailable] = newQuantity
        }
    }

    fun getStockQuantity(productId: UUID): Int = transaction {
        WarehouseStock.selectAll()
            .map { it }
            .firstOrNull { it[WarehouseStock.productId].value == productId }
            ?.get(WarehouseStock.quantityAvailable) ?: 0
    }
}