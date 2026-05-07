package com.supermarket.routes

import com.supermarket.data.addProductToCart
import com.supermarket.data.checkoutCart
import com.supermarket.data.findProduct
import com.supermarket.repositories.UserRepository
import com.supermarket.data.removeFromCart
import com.supermarket.data.updateCartQuantity
import com.supermarket.data.userStore
import com.supermarket.models.Role
import com.supermarket.models.UserSession
import com.supermarket.repositories.ProductRepository
import com.supermarket.repositories.AnalyticsRepository
import com.supermarket.repositories.AnalyticsFilter
import com.supermarket.repositories.PickListRepository
import com.supermarket.repositories.RecommendationRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
import java.time.LocalDate


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

            if (user == null || user.passwordHash != UserRepository.hashPassword(password)) {
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
            val isStaff = session.role == Role.EMPLOYEE.name || session.role == Role.ADMIN.name
            val marketingStats = if (isStaff) {
                AnalyticsRepository.marketingDashboardStats()
            } else {
                null
            }
            val recommendations = if (isStaff) {
                emptyList()
            } else {
                RecommendationRepository.recommendationsForCustomer(session.username)
            }
            call.respondText(dashboardHtml(session, marketingStats, recommendations), ContentType.Text.Html)
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

        get("/picklists") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.EMPLOYEE, Role.ADMIN) {
                call.respondText(
                    pickListsHtml(session, PickListRepository.activePickLists()),
                    ContentType.Text.Html
                )
            }
        }

        post("/picklists/update/{id}") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            requireRole(call, session, Role.EMPLOYEE, Role.ADMIN) {
                val orderId = call.parameters["id"] ?: ""
                val status = call.receiveParameters()["status"] ?: ""
                PickListRepository.updateOrderStatus(orderId, status)
                call.respondRedirect("/picklists")
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
                val seeded = call.request.queryParameters["seeded"]?.toIntOrNull()
                val message = seeded?.let {
                    if (it == 0) "Sample products were already present." else "$it sample products added to the database."
                } ?: ""
                call.respondText(adminHtml(session, message), ContentType.Text.Html)
            }
        }

        get("/analytics") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                val filter = analyticsFilterFromCall(call)
                call.respondText(
                    analyticsHtml(session, AnalyticsRepository.report(filter)),
                    ContentType.Text.Html
                )
            }
        }

        get("/analytics/download") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                val report = AnalyticsRepository.report(analyticsFilterFromCall(call))
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    "attachment; filename=\"marketing-analytics.csv\""
                )
                call.respondText(
                    AnalyticsRepository.toCsv(report),
                    ContentType.parse("text/csv")
                )
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
        get("/admin/products/add") {
            val session = call.sessions.get<UserSession>() ?: return@get call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                call.respondText(addProductHtml(session), ContentType.Text.Html)
            }
        }

        post("/admin/products/add") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                val params = call.receiveParameters()
                val name        = params["name"]        ?: ""
                val description = params["description"] ?: ""
                val category    = params["category"]    ?: ""
                val price       = params["price"]?.toDoubleOrNull()
                val sku         = params["sku"]         ?: ""
                val stock       = params["stock"]?.toIntOrNull() ?: 100

                if (name.isBlank() || sku.isBlank() || price == null) {
                    call.respondText(
                        addProductHtml(session, error = "Please fill in all required fields."),
                        ContentType.Text.Html
                    )
                    return@requireRole
                }

                try {
                    ProductRepository.addProduct(name, description, category, price, sku, stock)
                    call.respondText(
                        addProductHtml(session, success = "Product '$name' added successfully!"),
                        ContentType.Text.Html
                    )
                } catch (e: Exception) {
                    call.respondText(
                        addProductHtml(session, error = "Error adding product: ${e.message}"),
                        ContentType.Text.Html
                    )
                }
            }
        }

        post("/admin/products/seed") {
            val session = call.sessions.get<UserSession>() ?: return@post call.respondRedirect("/login")
            requireRole(call, session, Role.ADMIN) {
                val inserted = ProductRepository.seedAdditionalProducts()
                call.respondRedirect("/admin?seeded=$inserted")
            }
        }
        get("/register") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                call.respondRedirect("/dashboard")
                return@get
            }
            call.respondText(registerPageHtml(), ContentType.Text.Html)
        }

        post("/register") {
            val params    = call.receiveParameters()
            val username  = params["username"]  ?: ""
            val password  = params["password"]  ?: ""
            val password2 = params["password2"] ?: ""

            when {
                username.isBlank() || password.isBlank() -> {
                    call.respondText(
                        registerPageHtml(error = "Please fill in all fields."),
                        ContentType.Text.Html
                    )
                }
                password != password2 -> {
                    call.respondText(
                        registerPageHtml(error = "Passwords do not match."),
                        ContentType.Text.Html
                    )
                }
                password.length < 6 -> {
                    call.respondText(
                        registerPageHtml(error = "Password must be at least 6 characters."),
                        ContentType.Text.Html
                    )
                }
                else -> {
                    val created = UserRepository.registerUser(username, password)
                    if (created) {
                        call.respondText(
                            registerPageHtml(success = "Account created! You can now log in."),
                            ContentType.Text.Html
                        )
                    } else {
                        call.respondText(
                            registerPageHtml(error = "Username already taken."),
                            ContentType.Text.Html
                        )
                    }
                }
            }
        }
    }
}

private fun analyticsFilterFromCall(call: ApplicationCall): AnalyticsFilter {
    val query = call.request.queryParameters
    return AnalyticsFilter(
        search = query["search"]?.trim().orEmpty(),
        category = query["category"]?.trim().orEmpty(),
        from = parseDate(query["from"]),
        to = parseDate(query["to"])
    )
}

private fun parseDate(value: String?): LocalDate? =
    value?.takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
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
