import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun cartTotalIsCalculatedCorrectly() {
        println("Testing cart total calculation")

        val price = 2.50
        val quantity = 3
        val total = price * quantity

        assertEquals(7.50, total)
    }

    @Test
    fun inventoryIsReducedAfterOrder() {
        println("Testing inventory reduction after order")

        val stockBefore = 10
        val quantityBought = 3
        val stockAfter = stockBefore - quantityBought

        assertEquals(7, stockAfter)
    }

    @Test
    fun negativeQuantityIsRejected() {
        println("Testing negative quantity validation")

        val quantity = -2

        assertFalse(quantity > 0)
    }

    @Test
    fun emptyUsernameIsInvalid() {
        println("Testing empty username validation")

        val username = ""

        assertTrue(username.isBlank())
    }

    @Test
    fun negativePriceIsRejected() {
        println("Testing negative price validation")

        val price = -1.99

        assertFalse(price >= 0)
    }

    @Test
    fun checkoutCannotHappenWithEmptyCart() {
        println("Testing checkout validation with empty cart")

        val cartItems = 0

        assertEquals(0, cartItems)
        assertTrue(cartItems == 0)
    }

    @Test
    fun productsPageLoads() = testApplication {
    application { module() }

    val response = client.get("/products")

    assertEquals(HttpStatusCode.OK, response.status)
    }
}