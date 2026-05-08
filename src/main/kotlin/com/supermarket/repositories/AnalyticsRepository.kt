package com.supermarket.repositories

import com.supermarket.database.Categories
import com.supermarket.database.DbOrderItems
import com.supermarket.database.DbOrders
import com.supermarket.database.Products
import com.supermarket.database.WarehouseStock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// Represents sales performance for a single product.
data class ProductSales(
    val name: String,
    val category: String,
    val unitsSold: Int,
    val revenue: Double,
)

// Represents aggregated sales data for a category.
data class CategorySales(
    val category: String,
    val unitsSold: Int,
    val revenue: Double,
)

// Represents grouped order totals by spending range.
data class OrderSpendBand(
    val label: String,
    val orderCount: Int,
)

// Stores key marketing dashboard metrics and analytics.
data class MarketingDashboardStats(
    val bestSellers: List<ProductSales>,
    val categorySales: List<CategorySales>,
    val orderSpendBands: List<OrderSpendBand>,
    val totalOrders: Int,
    val totalRevenue: Double,
    val averageOrderValue: Double,
    val totalUnitsSold: Int,
    val averageItemsPerOrder: Double,
)

// Filters used when generating analytics reports.
data class AnalyticsFilter(
    val search: String = "",
    val category: String = "",
    val from: LocalDate? = null,
    val to: LocalDate? = null,
)

// Summary section for analytics reports.
data class AnalyticsSummary(
    val totalOrders: Int,
    val totalRevenue: Double,
    val totalUnitsSold: Int,
    val averageOrderValue: Double,
)

// Complete analytics report structure.
data class AnalyticsReport(
    val filter: AnalyticsFilter,
    val summary: AnalyticsSummary,
    val bestSellers: List<ProductSales>,
    val categorySales: List<CategorySales>,
    val lowStock: List<Triple<String, String, Int>>,
    val categories: List<String>,
)

// Handles analytics calculations and reporting queries.
object AnalyticsRepository {
    // Generates dashboard statistics for the marketing overview page.
    fun marketingDashboardStats(): MarketingDashboardStats {
        val report = report()
        val orderSpendBands = orderSpendBands()

        val averageItemsPerOrder =
            if (report.summary.totalOrders == 0) {
                0.0
            } else {
                report.summary.totalUnitsSold.toDouble() / report.summary.totalOrders
            }

        return MarketingDashboardStats(
            bestSellers = report.bestSellers,
            categorySales = report.categorySales,
            orderSpendBands = orderSpendBands,
            totalOrders = report.summary.totalOrders,
            totalRevenue = report.summary.totalRevenue,
            averageOrderValue = report.summary.averageOrderValue,
            totalUnitsSold = report.summary.totalUnitsSold,
            averageItemsPerOrder = averageItemsPerOrder,
        )
    }

    // Builds a filtered analytics report from database sales data.
    fun report(filter: AnalyticsFilter = AnalyticsFilter()): AnalyticsReport =
        transaction {
            val rows = filteredSalesRows(filter)

            val bestSellers =
                rows.groupBy { it.productName }
                    .map { (name, group) ->
                        ProductSales(
                            name = name,
                            category = group.first().category,
                            unitsSold = group.sumOf { it.quantity },
                            revenue = group.sumOf { it.revenue },
                        )
                    }
                    .sortedByDescending { it.unitsSold }
                    .take(10)

            val categorySales =
                rows.groupBy { it.category }
                    .map { (category, group) ->
                        CategorySales(
                            category = category,
                            unitsSold = group.sumOf { it.quantity },
                            revenue = group.sumOf { it.revenue },
                        )
                    }
                    .sortedByDescending { it.revenue }

            val totalOrders = rows.map { it.orderId }.toSet().size
            val totalRevenue = rows.sumOf { it.revenue }
            val totalUnits = rows.sumOf { it.quantity }

            AnalyticsReport(
                filter = filter,
                summary =
                    AnalyticsSummary(
                        totalOrders = totalOrders,
                        totalRevenue = totalRevenue,
                        totalUnitsSold = totalUnits,
                        averageOrderValue =
                            if (totalOrders == 0) {
                                0.0
                            } else {
                                totalRevenue / totalOrders
                            },
                    ),
                bestSellers = bestSellers,
                categorySales = categorySales,
                lowStock = lowStockProducts(filter),
                categories = categoryNames(),
            )
        }

    // Returns the top-selling products.
    fun bestSellers(): List<ProductSales> = report().bestSellers

    // Returns revenue and sales grouped by category.
    fun salesByCategory(): List<CategorySales> = report().categorySales

    // Retrieves products with low inventory levels.
    fun lowStockProducts(filter: AnalyticsFilter = AnalyticsFilter()): List<Triple<String, String, Int>> =
        transaction {
            val search = filter.search.lowercase()

            (WarehouseStock innerJoin Products innerJoin Categories)
                .selectAll()
                .map { row ->
                    LowStockRow(
                        name = row[Products.name],
                        sku = row[Products.sku],
                        category = row[Categories.name],
                        quantity = row[WarehouseStock.quantityAvailable],
                    )
                }
                .filter { it.quantity <= 10 }
                .filter {
                    filter.search.isBlank() ||
                        it.name.lowercase().contains(search) ||
                        it.sku.lowercase().contains(search)
                }
                .filter { filter.category.isBlank() || it.category == filter.category }
                .sortedBy { it.quantity }
                .map { Triple(it.name, it.sku, it.quantity) }
        }

    // Groups orders into predefined spending ranges.
    private fun orderSpendBands(): List<OrderSpendBand> =
        transaction {
            val totals =
                DbOrders.selectAll()
                    .map { it[DbOrders.totalAmount].toDouble() }

            listOf(
                OrderSpendBand("Under GBP 10", totals.count { it < 10.0 }),
                OrderSpendBand("GBP 10 to GBP 25", totals.count { it >= 10.0 && it < 25.0 }),
                OrderSpendBand("GBP 25 to GBP 50", totals.count { it >= 25.0 && it < 50.0 }),
                OrderSpendBand("GBP 50 plus", totals.count { it >= 50.0 }),
            )
        }

    // Converts an analytics report into CSV format.
    fun toCsv(report: AnalyticsReport): String {
        val lines = mutableListOf<String>()

        lines += "SuperMarket Marketing Analytics"
        lines += "Search,${csv(report.filter.search.ifBlank { "All" })}"
        lines += "Category,${csv(report.filter.category.ifBlank { "All" })}"
        lines += "From,${csv(report.filter.from?.toString() ?: "All")}"
        lines += "To,${csv(report.filter.to?.toString() ?: "All")}"
        lines += ""

        lines += "Summary"
        lines += "Total orders,${report.summary.totalOrders}"
        lines += "Total revenue,${"%.2f".format(java.util.Locale.UK, report.summary.totalRevenue)}"
        lines += "Units sold,${report.summary.totalUnitsSold}"
        lines += "Average order value,${"%.2f".format(java.util.Locale.UK, report.summary.averageOrderValue)}"
        lines += ""

        lines += "Best selling products"
        lines += "Product,Category,Units sold,Revenue"

        report.bestSellers.forEach {
            lines +=
                "${csv(it.name)},${csv(it.category)},${it.unitsSold}," +
                "${"%.2f".format(java.util.Locale.UK, it.revenue)}"
        }

        lines += ""
        lines += "Sales by category"
        lines += "Category,Units sold,Revenue"

        report.categorySales.forEach {
            lines +=
                "${csv(it.category)},${it.unitsSold}," +
                "${"%.2f".format(java.util.Locale.UK, it.revenue)}"
        }

        lines += ""
        lines += "Low stock"
        lines += "Product,SKU,Quantity available"

        report.lowStock.forEach { (name, sku, qty) ->
            lines += "${csv(name)},${csv(sku)},$qty"
        }

        return lines.joinToString("\n")
    }

    // Retrieves and filters sales rows from joined database tables.
    private fun filteredSalesRows(filter: AnalyticsFilter): List<SalesRow> {
        val search = filter.search.lowercase()

        return (DbOrderItems innerJoin DbOrders innerJoin Products innerJoin Categories)
            .selectAll()
            .map { row ->
                SalesRow(
                    orderId = row[DbOrders.id].value,
                    placedAt = row[DbOrders.placedAt],
                    productName = row[Products.name],
                    category = row[Categories.name],
                    quantity = row[DbOrderItems.quantity],
                    revenue = row[DbOrderItems.unitPrice].toDouble() * row[DbOrderItems.quantity],
                )
            }
            .filter { row ->
                val placedDate = row.placedAt?.toLocalDate()

                val matchesSearch =
                    filter.search.isBlank() ||
                        row.productName.lowercase().contains(search) ||
                        row.category.lowercase().contains(search)

                val matchesCategory =
                    filter.category.isBlank() || row.category == filter.category

                val matchesFrom =
                    filter.from == null ||
                        (placedDate != null && !placedDate.isBefore(filter.from))

                val matchesTo =
                    filter.to == null ||
                        (placedDate != null && !placedDate.isAfter(filter.to))

                matchesSearch && matchesCategory && matchesFrom && matchesTo
            }
    }

    // Returns all category names sorted alphabetically.
    private fun categoryNames(): List<String> =
        Categories.selectAll()
            .map { it[Categories.name] }
            .sorted()

    // Escapes values for safe CSV formatting.
    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    // Internal model representing a sales row after database mapping.
    private data class SalesRow(
        val orderId: UUID,
        val placedAt: OffsetDateTime?,
        val productName: String,
        val category: String,
        val quantity: Int,
        val revenue: Double,
    )

    // Internal model representing low-stock inventory records.
    private data class LowStockRow(
        val name: String,
        val sku: String,
        val category: String,
        val quantity: Int,
    )
}
