package com.supermarket.repositories

import com.supermarket.database.Categories
import com.supermarket.database.DbOrderItems
import com.supermarket.database.DbOrders
import com.supermarket.database.Products
import com.supermarket.database.Users
import com.supermarket.database.WarehouseStock
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.format.DateTimeFormatter
import java.util.UUID

data class PickListLine(
    val productName: String,
    val category: String,
    val sku: String,
    val quantity: Int,
    val unitPrice: Double,
    val stockAvailable: Int,
    val status: String
)

data class PickListOrder(
    val id: String,
    val displayId: String,
    val customerName: String,
    val placedAt: String,
    val status: String,
    val total: Double,
    val lines: List<PickListLine>
) {
    val itemCount: Int = lines.sumOf { it.quantity }
    val stockIssueCount: Int = lines.count { it.stockAvailable < it.quantity }
}

object PickListRepository {
    private val activeStatuses = setOf("placed", "picking", "packed", "pending_payment")
    val statusOptions = listOf("placed", "picking", "packed", "delivered")

    fun activePickLists(): List<PickListOrder> = transaction {
        val usersById = Users.selectAll()
            .associate { it[Users.id].value to it[Users.firstName] }
        val stockByProduct = WarehouseStock.selectAll()
            .groupBy { it[WarehouseStock.productId].value }
            .mapValues { (_, rows) -> rows.sumOf { it[WarehouseStock.quantityAvailable] } }
        val linesByOrder = (DbOrderItems innerJoin Products innerJoin Categories)
            .selectAll()
            .map { row ->
                val productId = row[DbOrderItems.productId].value
                row[DbOrderItems.orderId].value to PickListLine(
                    productName = row[Products.name],
                    category = row[Categories.name],
                    sku = row[Products.sku],
                    quantity = row[DbOrderItems.quantity],
                    unitPrice = row[DbOrderItems.unitPrice].toDouble(),
                    stockAvailable = stockByProduct[productId] ?: 0,
                    status = row[DbOrderItems.status]
                )
            }
            .groupBy({ it.first }, { it.second })

        DbOrders.selectAll()
            .map { row ->
                val orderId = row[DbOrders.id].value
                PickListOrder(
                    id = orderId.toString(),
                    displayId = "ORD-${orderId.toString().take(8).uppercase()}",
                    customerName = usersById[row[DbOrders.userId].value] ?: "Unknown",
                    placedAt = row[DbOrders.placedAt]?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        ?: "Unknown",
                    status = row[DbOrders.status],
                    total = row[DbOrders.totalAmount].toDouble(),
                    lines = linesByOrder[orderId].orEmpty()
                )
            }
            .filter { it.status in activeStatuses }
            .sortedWith(compareBy<PickListOrder> { statusSortOrder(it.status) }.thenBy { it.placedAt })
    }

    fun updateOrderStatus(orderId: String, newStatus: String): Boolean = transaction {
        if (newStatus !in statusOptions) return@transaction false

        val uuid = runCatching { UUID.fromString(orderId) }.getOrNull() ?: return@transaction false
        DbOrders.update({ DbOrders.id eq uuid }) {
            it[DbOrders.status] = newStatus
        } > 0
    }

    private fun statusSortOrder(status: String) = when (status) {
        "placed" -> 0
        "pending_payment" -> 1
        "picking" -> 2
        "packed" -> 3
        else -> 4
    }
}
