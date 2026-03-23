package com.supermarket.routes

import com.supermarket.data.addProductToCart
import com.supermarket.data.checkoutCart
import com.supermarket.data.findProduct
import com.supermarket.data.hashPassword
import com.supermarket.data.removeFromCart
import com.supermarket.data.updateCartQuantity
import com.supermarket.data.userStore
import com.supermarket.models.Role
import com.supermarket.models.UserSession
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import com.supermarket.data.updateProductStock

fun Application.registerRoutes() {
    routing {
        get("/") {
            call.respondRedirect("/login")
        }

        get("/login") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respondRedirect("/dashboard")
                return@get
            }
            call.respondText(loginPageHtml(), ContentType.Text.Html)
        }

        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            val user = userStore[username]

            if (user == null || user.passwordHash != hashPassword(password)) {
                call.respondText(loginPageHtml("Invalid username or password."), ContentType.Text.Html)
                return@post
            }

            call.sessions.set(UserSession(username = user.username, role = user.role.name))
            call.respondRedirect("/dashboard")
        }

        get("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/login")
        }

        get("/dashboard") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            call.respondText(dashboardHtml(session), ContentType.Text.Html)
        }

        get("/products") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            call.respondText(productsHtml(session), ContentType.Text.Html)
        }

        get("/products/{id}") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            val id = call.parameters["id"]?.toIntOrNull()
            val product = if (id != null) findProduct(id) else null

            if (product == null) {
                call.respondText(
                    notFoundHtml(session, "Product not found."),
                    ContentType.Text.Html,
                    HttpStatusCode.NotFound
                )
            } else {
                call.respondText(productDetailHtml(session, product), ContentType.Text.Html)
            }
        }

        get("/cart") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            call.respondText(cartHtml(session), ContentType.Text.Html)
        }

        get("/checkout") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            call.respondText(checkoutHtml(session), ContentType.Text.Html)
        }

        get("/orders") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            call.respondText(ordersHtml(session), ContentType.Text.Html)
        }

        post("/cart/add/{id}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val productId = call.parameters["id"]?.toIntOrNull()
            val quantity = call.receiveParameters()["quantity"]?.toIntOrNull() ?: 1

            if (productId != null && findProduct(productId) != null) {
                addProductToCart(session.username, productId, quantity)
            }

            call.respondRedirect("/cart")
        }

        post("/cart/update/{id}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val productId = call.parameters["id"]?.toIntOrNull()
            val quantity = call.receiveParameters()["quantity"]?.toIntOrNull() ?: 1

            if (productId != null) {
                updateCartQuantity(session.username, productId, quantity)
            }

            call.respondRedirect("/cart")
        }

        post("/cart/remove/{id}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            val productId = call.parameters["id"]?.toIntOrNull()

            if (productId != null) {
                removeFromCart(session.username, productId)
            }

            call.respondRedirect("/cart")
        }

        post("/checkout") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            checkoutCart(session.username)
            call.respondRedirect("/orders")
        }

        get("/inventory") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.EMPLOYEE, Role.ADMIN) {
                call.respondText(inventoryHtml(session), ContentType.Text.Html)
            }
        }

        post("/inventory/update/{id}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            requireRole(call, session, Role.EMPLOYEE, Role.ADMIN) {
                val productId = call.parameters["id"]?.toIntOrNull()
                val newStock = call.receiveParameters()["stock"] ?: ""

                if (
                    productId != null &&
                    newStock in listOf("In stock", "Low stock", "Out of stock")
                ) {
                    updateProductStock(productId, newStock)
                }

                call.respondRedirect("/inventory")
            }
        }

        get("/admin") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                call.respondText(adminHtml(session), ContentType.Text.Html)
            }
        }

        get("/admin/users") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                val userList = userStore.values.joinToString("") {
                    "<tr><td>${it.username}</td><td>${it.role}</td></tr>"
                }
                call.respondText(adminUsersHtml(session, userList), ContentType.Text.Html)
            }
        }
    }
}

private suspend fun requireRole(
    call: ApplicationCall,
    session: UserSession,
    vararg allowed: Role,
    block: suspend () -> Unit
) {
    if (Role.valueOf(session.role) in allowed) {
        block()
    } else {
        call.respondText(forbiddenHtml(session), ContentType.Text.Html, HttpStatusCode.Forbidden)
    }
}