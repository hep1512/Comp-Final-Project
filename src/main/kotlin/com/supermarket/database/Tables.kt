package com.supermarket.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Users : UUIDTable("users") {
    val email        = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val firstName    = text("first_name")
    val lastName     = text("last_name")
    val phone        = text("phone").nullable()
    val role         = text("role").default("customer")
    val isActive     = bool("is_active").default(true)
    val createdAt    = timestampWithTimeZone("created_at")
}

object Products : UUIDTable("products") {
    val name        = text("name")
    val description = text("description").nullable()
    val categoryId  = reference("category_id", Categories)
    val basePrice   = decimal("base_price", 10, 2)
    val sku         = text("sku").uniqueIndex()
    val imageUrl    = text("image_url").nullable()
    val isActive    = bool("is_active").default(true)
    val createdAt   = timestampWithTimeZone("created_at")
}

object Categories : UUIDTable("categories") {
    val name     = text("name").uniqueIndex()
    val slug     = text("slug").uniqueIndex()
    val parentId = reference("parent_id", Categories).nullable()
}

object Orders : UUIDTable("orders") {
    val userId      = reference("user_id", Users)
    val addressId   = uuid("address_id")
    val warehouseId = uuid("warehouse_id").nullable()
    val status      = text("status").default("pending_payment")
    val totalAmount = decimal("total_amount", 10, 2)
    val placedAt    = timestampWithTimeZone("placed_at").nullable()
    val deliveredAt = timestampWithTimeZone("delivered_at").nullable()
}

object OrderItems : UUIDTable("order_items") {
    val orderId       = reference("order_id", Orders)
    val productId     = reference("product_id", Products)
    val quantity      = integer("quantity")
    val unitPrice     = decimal("unit_price", 10, 2)
    val status        = text("status").default("pending")
}

object WarehouseStock : UUIDTable("warehouse_stock") {
    val warehouseId        = uuid("warehouse_id")
    val productId          = reference("product_id", Products)
    val quantityAvailable  = integer("quantity_available")
    val quantityReserved   = integer("quantity_reserved")
    val lowStockThreshold  = integer("low_stock_threshold")
}