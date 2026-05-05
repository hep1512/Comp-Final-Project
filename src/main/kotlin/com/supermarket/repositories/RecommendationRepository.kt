package com.supermarket.repositories

import com.supermarket.database.Categories
import com.supermarket.database.DbOrderItems
import com.supermarket.database.DbOrders
import com.supermarket.database.Products
import com.supermarket.database.Users
import com.supermarket.models.Product
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.abs

data class ProductRecommendation(
    val product: Product,
    val score: Double,
    val matchPercent: Int,
    val reasons: List<String>
)

object RecommendationRepository {

    fun recommendationsForCustomer(username: String, limit: Int = 4): List<ProductRecommendation> {
        val products = ProductRepository.allProducts()
            .filter { it.stock != "Out of stock" }

        if (products.isEmpty()) return emptyList()

        val marketingStats = AnalyticsRepository.marketingDashboardStats()
        val history = customerHistory(username)
        val maxUnitsSold = marketingStats.bestSellers.maxOfOrNull { it.unitsSold }?.coerceAtLeast(1) ?: 1
        val maxCategoryRevenue = marketingStats.categorySales.maxOfOrNull { it.revenue }?.coerceAtLeast(1.0) ?: 1.0
        val maxCustomerCategoryQuantity = history.categoryQuantities.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val salesByProduct = marketingStats.bestSellers.associateBy { it.name }
        val salesByCategory = marketingStats.categorySales.associateBy { it.category }
        val targetItemPrice = targetItemPrice(history, marketingStats)

        return products.map { product ->
            val productSales = salesByProduct[product.name]
            val categorySales = salesByCategory[product.category]
            val popularityScore = (productSales?.unitsSold ?: 0).toDouble() / maxUnitsSold
            val categoryScore = (categorySales?.revenue ?: 0.0) / maxCategoryRevenue
            val personalScore = if (history.categoryQuantities.isEmpty()) {
                categoryScore * 0.5
            } else {
                (history.categoryQuantities[product.category] ?: 0).toDouble() / maxCustomerCategoryQuantity
            }
            val priceScore = priceFitScore(product.price, targetItemPrice)
            val noveltyScore = if (product.name in history.productNames) -0.08 else 0.08
            val stockScore = if (product.stock == "In stock") 0.08 else 0.03
            val score = (
                popularityScore * 0.34 +
                    categoryScore * 0.24 +
                    personalScore * 0.24 +
                    priceScore * 0.10 +
                    noveltyScore +
                    stockScore
                ).coerceAtLeast(0.0)

            ProductRecommendation(
                product = product,
                score = score,
                matchPercent = (score.coerceIn(0.0, 1.0) * 100).toInt().coerceAtLeast(25),
                reasons = recommendationReasons(
                    product = product,
                    productSales = productSales,
                    categorySales = categorySales,
                    history = history,
                    priceScore = priceScore
                )
            )
        }
            .sortedWith(compareByDescending<ProductRecommendation> { it.score }.thenBy { it.product.price })
            .take(limit)
    }

    private fun customerHistory(username: String): CustomerHistory = transaction {
        val userId = Users.selectAll()
            .map { it }
            .firstOrNull { it[Users.firstName] == username }
            ?.get(Users.id)
            ?.value
            ?: return@transaction CustomerHistory(emptySet(), emptyMap(), 0.0)

        val orders = DbOrders.selectAll()
            .map { it }
            .filter { it[DbOrders.userId].value == userId }
        val orderIds = orders.map { it[DbOrders.id].value }.toSet()
        val averageOrderValue = if (orders.isEmpty()) {
            0.0
        } else {
            orders.sumOf { it[DbOrders.totalAmount].toDouble() } / orders.size
        }

        if (orderIds.isEmpty()) {
            return@transaction CustomerHistory(emptySet(), emptyMap(), averageOrderValue)
        }

        val productLookup = (Products innerJoin Categories)
            .selectAll()
            .map { row ->
                row[Products.id].value to ProductIdentity(
                    name = row[Products.name],
                    category = row[Categories.name]
                )
            }
            .toMap()
        val purchasedNames = mutableSetOf<String>()
        val categoryQuantities = mutableMapOf<String, Int>()

        DbOrderItems.selectAll()
            .map { it }
            .filter { it[DbOrderItems.orderId].value in orderIds }
            .forEach { row ->
                val identity = productLookup[row[DbOrderItems.productId].value] ?: return@forEach
                purchasedNames += identity.name
                categoryQuantities[identity.category] =
                    (categoryQuantities[identity.category] ?: 0) + row[DbOrderItems.quantity]
            }

        CustomerHistory(
            productNames = purchasedNames,
            categoryQuantities = categoryQuantities,
            averageOrderValue = averageOrderValue
        )
    }

    private fun targetItemPrice(
        history: CustomerHistory,
        marketingStats: MarketingDashboardStats
    ): Double {
        val orderValue = when {
            history.averageOrderValue > 0.0 -> history.averageOrderValue
            marketingStats.averageOrderValue > 0.0 -> marketingStats.averageOrderValue
            else -> 8.0
        }
        val basketSize = marketingStats.averageItemsPerOrder.coerceAtLeast(1.0)

        return (orderValue / basketSize).coerceAtLeast(1.0)
    }

    private fun priceFitScore(productPrice: Double, targetItemPrice: Double): Double {
        if (targetItemPrice <= 0.0) return 0.5

        val distance = abs(productPrice - targetItemPrice) / targetItemPrice
        return (1.0 - distance).coerceIn(0.0, 1.0)
    }

    private fun recommendationReasons(
        product: Product,
        productSales: ProductSales?,
        categorySales: CategorySales?,
        history: CustomerHistory,
        priceScore: Double
    ): List<String> {
        val reasons = mutableListOf<String>()

        if ((history.categoryQuantities[product.category] ?: 0) > 0) {
            reasons += "Matches your ${product.category} choices"
        }
        if ((productSales?.unitsSold ?: 0) > 0) {
            reasons += "Popular with shoppers"
        }
        if ((categorySales?.revenue ?: 0.0) > 0.0) {
            reasons += "Trending category"
        }
        if (priceScore >= 0.7) {
            reasons += "Fits typical basket spend"
        }
        if (product.stock == "In stock") {
            reasons += "Available now"
        }

        return reasons.take(3).ifEmpty { listOf("Suggested from current catalogue data") }
    }

    private data class CustomerHistory(
        val productNames: Set<String>,
        val categoryQuantities: Map<String, Int>,
        val averageOrderValue: Double
    )

    private data class ProductIdentity(
        val name: String,
        val category: String
    )
}
