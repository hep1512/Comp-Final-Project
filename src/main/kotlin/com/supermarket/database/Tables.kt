package com.supermarket.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

// Database table definitions for the supermarket application using Exposed ORM
// and Neon PostgreSQL.

// Stores registered user accounts and authentication details.
object Users : UUIDTable("users") {
    val email = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val firstName = text("first_name")
    val lastName = text("last_name")
    val phone = text("phone").nullable()
    val role = text("role").default("customer")
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
}

// Stores product information available in the supermarket catalog.
object Products : UUIDTable("products") {
    val name = text("name")
    val description = text("description").nullable()
    val categoryId = reference("category_id", Categories)
    val basePrice = decimal("base_price", 10, 2)
    val sku = text("sku").uniqueIndex()
    val imageUrl = text("image_url").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestampWithTimeZone("created_at")
}

// Stores product categories and supports nested parent-child categories.
object Categories : UUIDTable("categories") {
    val name = text("name").uniqueIndex()
    val slug = text("slug").uniqueIndex()
    val parentId = reference("parent_id", Categories).nullable()
}

// Stores customer order records.
object Orders : UUIDTable("orders") {
    val userId = reference("user_id", Users)
    val addressId = uuid("address_id")
    val warehouseId = uuid("warehouse_id").nullable()
    val status = text("status").default("pending_payment")
    val totalAmount = decimal("total_amount", 10, 2)
    val placedAt = timestampWithTimeZone("placed_at").nullable()
    val deliveredAt = timestampWithTimeZone("delivered_at").nullable()
}

// Stores individual items associated with each order.
object OrderItems : UUIDTable("order_items") {
    val orderId = reference("order_id", Orders)
    val productId = reference("product_id", Products)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2)
    val status = text("status").default("pending")
}

// Tracks product inventory levels across warehouses.
object WarehouseStock : UUIDTable("warehouse_stock") {
    val warehouseId = uuid("warehouse_id")
    val productId = reference("product_id", Products)
    val quantityAvailable = integer("quantity_available")
    val quantityReserved = integer("quantity_reserved")
    val lowStockThreshold = integer("low_stock_threshold")
}

// Alternative orders table used for database-specific order operations.
object DbOrders : UUIDTable("orders") {
    val userId = reference("user_id", Users)
    val addressId = uuid("address_id")
    val status = text("status").default("pending_payment")
    val totalAmount = decimal("total_amount", 10, 2)
    val placedAt = timestampWithTimeZone("placed_at").nullable()
}

// Alternative order items table linked to DbOrders.
object DbOrderItems : UUIDTable("order_items") {
    val orderId = reference("order_id", DbOrders)
    val productId = reference("product_id", Products)
    val quantity = integer("quantity")
    val unitPrice = decimal("unit_price", 10, 2)
    val status = text("status").default("pending")
}

// Stores delivery and billing addresses for users.
object Addresses : UUIDTable("addresses") {
    val userId = reference("user_id", Users)
    val line1 = text("line1")
    val city = text("city")
    val postcode = text("postcode")
    val isDefault = bool("is_default").default(false)
}

// Stores warehouse locations and operational status.
object Warehouses : UUIDTable("warehouses") {
    val name = text("name")
    val address = text("address")
    val postcode = text("postcode")
    val isActive = bool("is_active").default(true)
}
