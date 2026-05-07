package com.supermarket.repositories

import com.supermarket.database.Products
import com.supermarket.database.Categories
import com.supermarket.database.WarehouseStock
import com.supermarket.database.Warehouses
import com.supermarket.models.Product
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

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
    fun addProduct(
        name: String,
        description: String,
        categorySlug: String,
        price: Double,
        sku: String,
        stockQuantity: Int = 100
    ) = transaction {
        val categoryRow = Categories.selectAll()
            .map { it }
            .firstOrNull { it[Categories.slug] == categorySlug } ?: return@transaction

        val categoryId = categoryRow[Categories.id].value
        val warehouseId = Warehouses.selectAll()
            .map { it[Warehouses.id].value }
            .firstOrNull()
            ?: Warehouses.insertAndGetId {
                it[Warehouses.name] = "Main Warehouse"
                it[Warehouses.address] = "1 Fulfilment Way"
                it[Warehouses.postcode] = "SW1A 1AA"
                it[Warehouses.isActive] = true
            }.value

        val productId = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.description] = description
            it[Products.categoryId] = categoryId
            it[Products.basePrice] = price.toBigDecimal()
            it[Products.sku] = sku
            it[Products.isActive] = true
            it[Products.createdAt] = OffsetDateTime.now()
        }.value

        WarehouseStock.insert {
            it[WarehouseStock.warehouseId] = warehouseId
            it[WarehouseStock.productId] = productId
            it[WarehouseStock.quantityAvailable] = stockQuantity.coerceAtLeast(0)
            it[WarehouseStock.quantityReserved] = 0
            it[WarehouseStock.lowStockThreshold] = 10
        }
    }

    fun seedAdditionalProducts(): Int = transaction {
        val warehouseId = Warehouses.selectAll()
            .map { it[Warehouses.id].value }
            .firstOrNull()
            ?: Warehouses.insertAndGetId {
                it[Warehouses.name] = "Main Warehouse"
                it[Warehouses.address] = "1 Fulfilment Way"
                it[Warehouses.postcode] = "SW1A 1AA"
                it[Warehouses.isActive] = true
            }.value
        val categoriesBySlug = ensureSeedCategories()
        val existingSkus = Products.selectAll()
            .map { it[Products.sku] }
            .toSet()
        var inserted = 0

        seedProducts.filterNot { it.sku in existingSkus }.forEach { seed ->
            val categoryId = categoriesBySlug[seed.categorySlug] ?: return@forEach
            val productId = Products.insertAndGetId {
                it[Products.name] = seed.name
                it[Products.description] = seed.description
                it[Products.categoryId] = categoryId
                it[Products.basePrice] = seed.price.toBigDecimal()
                it[Products.sku] = seed.sku
                it[Products.isActive] = true
                it[Products.createdAt] = OffsetDateTime.now()
            }.value

            WarehouseStock.insert {
                it[WarehouseStock.warehouseId] = warehouseId
                it[WarehouseStock.productId] = productId
                it[WarehouseStock.quantityAvailable] = seed.stock
                it[WarehouseStock.quantityReserved] = 0
                it[WarehouseStock.lowStockThreshold] = 10
            }
            inserted++
        }

        inserted
    }

    fun findProduct(productId: Int): Product? = transaction {
        (Products innerJoin Categories innerJoin WarehouseStock)
            .selectAll()
            .mapIndexed { index, row -> rowToProduct(row, index + 1) }
            .find { it.id == productId }
        
    }

    private fun ensureSeedCategories() =
        seedCategories.associate { seed ->
            val id = Categories.selectAll()
                .map { it }
                .firstOrNull { it[Categories.slug] == seed.slug }
                ?.get(Categories.id)
                ?.value
                ?: Categories.insertAndGetId {
                    it[Categories.name] = seed.name
                    it[Categories.slug] = seed.slug
                }.value

            seed.slug to id
        }

    private val seedCategories = listOf(
        SeedCategory("Fruit & Vegetables", "fruit-vegetables"),
        SeedCategory("Dairy & Eggs", "dairy-eggs"),
        SeedCategory("Bakery", "bakery"),
        SeedCategory("Meat & Fish", "meat-fish"),
        SeedCategory("Drinks", "drinks"),
        SeedCategory("Pantry", "pantry"),
        SeedCategory("Frozen", "frozen"),
        SeedCategory("Household", "household")
    )

    private val seedProducts = listOf(
        SeedProduct("Bananas Bunch", "Fresh ripe bananas, ideal for lunchboxes and smoothies.", "fruit-vegetables", 1.25, "SKU-BAN-001", 85),
        SeedProduct("Baby Spinach", "Washed spinach leaves for salads and cooking.", "fruit-vegetables", 1.80, "SKU-SPN-001", 42),
        SeedProduct("Cherry Tomatoes", "Sweet cherry tomatoes in a recyclable punnet.", "fruit-vegetables", 2.10, "SKU-TOM-001", 38),
        SeedProduct("Semi-Skimmed Milk 2L", "Chilled British semi-skimmed milk.", "dairy-eggs", 1.65, "SKU-MLK-002", 64),
        SeedProduct("Free Range Eggs 12 Pack", "Large free range eggs from British farms.", "dairy-eggs", 3.20, "SKU-EGG-012", 28),
        SeedProduct("Greek Style Yoghurt", "Thick plain yoghurt for breakfast and cooking.", "dairy-eggs", 2.35, "SKU-YOG-001", 31),
        SeedProduct("Sourdough Loaf", "Crusty bakery sourdough loaf.", "bakery", 2.75, "SKU-BRD-004", 24),
        SeedProduct("Croissants 4 Pack", "Buttery all-butter croissants.", "bakery", 2.60, "SKU-CRS-004", 20),
        SeedProduct("Chicken Breast Fillets", "Fresh skinless chicken breast fillets.", "meat-fish", 5.75, "SKU-CHK-001", 18),
        SeedProduct("Salmon Fillets 2 Pack", "Responsibly sourced salmon fillets.", "meat-fish", 6.90, "SKU-SAL-002", 12),
        SeedProduct("Sparkling Water 6 Pack", "Lightly sparkling mineral water cans.", "drinks", 3.10, "SKU-WTR-006", 72),
        SeedProduct("Orange Juice 1L", "Smooth orange juice from concentrate.", "drinks", 1.95, "SKU-JCE-001", 44),
        SeedProduct("Basmati Rice 1kg", "Long grain basmati rice for everyday meals.", "pantry", 2.40, "SKU-RCE-001", 56),
        SeedProduct("Penne Pasta 500g", "Dried penne pasta made with durum wheat.", "pantry", 1.15, "SKU-PST-001", 68),
        SeedProduct("Tomato Pasta Sauce", "Rich tomato and basil pasta sauce.", "pantry", 1.70, "SKU-SCE-001", 47),
        SeedProduct("Frozen Peas 900g", "Garden peas frozen soon after picking.", "frozen", 1.85, "SKU-FPZ-001", 34),
        SeedProduct("Vanilla Ice Cream", "Creamy vanilla dairy ice cream.", "frozen", 3.25, "SKU-ICE-001", 22),
        SeedProduct("Kitchen Roll 4 Pack", "Absorbent household kitchen roll.", "household", 4.50, "SKU-KRL-004", 30),
        SeedProduct("Laundry Capsules 24 Pack", "Non-bio laundry capsules for family washes.", "household", 6.75, "SKU-LND-024", 16),
        SeedProduct("Washing Up Liquid", "Lemon washing up liquid for dishes.", "household", 1.35, "SKU-WUL-001", 39)
    )

    private data class SeedCategory(
        val name: String,
        val slug: String
    )

    private data class SeedProduct(
        val name: String,
        val description: String,
        val categorySlug: String,
        val price: Double,
        val sku: String,
        val stock: Int
    )
}
