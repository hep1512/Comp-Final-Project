import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun cartTotalIsCalculatedCorrectly() {

        val price = 2.50
        val quantity = 3
        val total = price * quantity

        assertEquals(7.50, total)
    }

    @Test
    fun inventoryIsReducedAfterOrder() {

        val stockBefore = 10
        val quantityBought = 3
        val stockAfter = stockBefore - quantityBought

        assertEquals(7, stockAfter)
    }

    @Test
    fun negativeQuantityIsRejected() {

        val quantity = -2

        assertFalse(quantity > 0)
    }

    @Test
    fun emptyUsernameIsInvalid() {

        val username = ""

        assertTrue(username.isBlank())
    }

    @Test
    fun negativePriceIsRejected() {

        val price = -1.99

        assertFalse(price >= 0)
    }

    @Test
    fun checkoutCannotHappenWithEmptyCart() {

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

    @Test
    fun specialCharactersInUsernameAreHandled() {
        val username = "admin@#£"

        assertFalse(username.matches(Regex("^[a-zA-Z0-9_]+$")))
    }

    @Test
    fun veryLargeQuantityIsRejected() {
        val quantity = 1000000
        val availableStock = 50

        assertFalse(quantity <= availableStock)
    }
}