package com.supermarket.repositories

import com.supermarket.database.DbOrderItems
import com.supermarket.database.DbOrders
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

data class OrderSpendBand(
    val label: String,
    val orderCount: Int
)

data class MarketingDashboardStats(
    val bestSellers: List<ProductSales>,
    val categorySales: List<CategorySales>,
    val orderSpendBands: List<OrderSpendBand>,
    val totalOrders: Int,
    val totalRevenue: Double,
    val averageOrderValue: Double,
    val totalUnitsSold: Int,
    val averageItemsPerOrder: Double
)

object AnalyticsRepository {

    fun marketingDashboardStats(): MarketingDashboardStats {
        val bestSellers = bestSellers()
        val categorySales = salesByCategory()
        val orderSpendBands = orderSpendBands()
        val summary = marketingSummary()

        return MarketingDashboardStats(
            bestSellers = bestSellers,
            categorySales = categorySales,
            orderSpendBands = orderSpendBands,
            totalOrders = summary.totalOrders,
            totalRevenue = summary.totalRevenue,
            averageOrderValue = summary.averageOrderValue,
            totalUnitsSold = summary.totalUnitsSold,
            averageItemsPerOrder = summary.averageItemsPerOrder
        )
    }

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

    private fun marketingSummary(): MarketingSummary = transaction {
        val orderTotals = DbOrders.selectAll()
            .map { it[DbOrders.totalAmount].toDouble() }
        val totalOrders = orderTotals.size
        val totalRevenue = orderTotals.sum()
        val totalUnitsSold = DbOrderItems.selectAll()
            .sumOf { it[DbOrderItems.quantity] }

        MarketingSummary(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            averageOrderValue = if (totalOrders == 0) 0.0 else totalRevenue / totalOrders,
            totalUnitsSold = totalUnitsSold,
            averageItemsPerOrder = if (totalOrders == 0) 0.0 else totalUnitsSold.toDouble() / totalOrders
        )
    }

    private fun orderSpendBands(): List<OrderSpendBand> = transaction {
        val totals = DbOrders.selectAll()
            .map { it[DbOrders.totalAmount].toDouble() }

        listOf(
            OrderSpendBand("Under GBP 10", totals.count { it < 10.0 }),
            OrderSpendBand("GBP 10 to GBP 25", totals.count { it >= 10.0 && it < 25.0 }),
            OrderSpendBand("GBP 25 to GBP 50", totals.count { it >= 25.0 && it < 50.0 }),
            OrderSpendBand("GBP 50 plus", totals.count { it >= 50.0 })
        )
    }

    private data class MarketingSummary(
        val totalOrders: Int,
        val totalRevenue: Double,
        val averageOrderValue: Double,
        val totalUnitsSold: Int,
        val averageItemsPerOrder: Double
    )
}
