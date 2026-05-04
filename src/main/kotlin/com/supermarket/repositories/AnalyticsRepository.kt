package com.supermarket.repositories

import com.supermarket.database.DbOrderItems
import com.supermarket.database.Products
import com.supermarket.database.Categories
import com.supermarket.database.WarehouseStock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

data class ProductSales(
    val name: String,
    val category: String,
    val unitsSold: Int,
    val revenue: Double
)

data class CategorySales(
    val category: String,
    val unitsSold: Int,
    val revenue: Double
)

object AnalyticsRepository {

    fun bestSellers(): List<ProductSales> = transaction {
        (DbOrderItems innerJoin Products innerJoin Categories)
            .selectAll()
            .map { row ->
                ProductSales(
                    name = row[Products.name],
                    category = row[Categories.name],
                    unitsSold = row[DbOrderItems.quantity],
                    revenue = row[DbOrderItems.unitPrice].toDouble() * row[DbOrderItems.quantity]
                )
            }
            .groupBy { it.name }
            .map { (name, rows) ->
                ProductSales(
                    name = name,
                    category = rows.first().category,
                    unitsSold = rows.sumOf { it.unitsSold },
                    revenue = rows.sumOf { it.revenue }
                )
            }
            .sortedByDescending { it.unitsSold }
            .take(10)
    }

    fun salesByCategory(): List<CategorySales> = transaction {
        (DbOrderItems innerJoin Products innerJoin Categories)
            .selectAll()
            .map { row ->
                CategorySales(
                    category = row[Categories.name],
                    unitsSold = row[DbOrderItems.quantity],
                    revenue = row[DbOrderItems.unitPrice].toDouble() * row[DbOrderItems.quantity]
                )
            }
            .groupBy { it.category }
            .map { (category, rows) ->
                CategorySales(
                    category = category,
                    unitsSold = rows.sumOf { it.unitsSold },
                    revenue = rows.sumOf { it.revenue }
                )
            }
            .sortedByDescending { it.revenue }
    }

    fun lowStockProducts(): List<Triple<String, String, Int>> = transaction {
        (WarehouseStock innerJoin Products)
            .selectAll()
            .map { row ->
                Triple(
                    row[Products.name],
                    row[Products.sku],
                    row[WarehouseStock.quantityAvailable]
                )
            }
            .filter { it.third <= 10 }
            .sortedBy { it.third }
    }
}