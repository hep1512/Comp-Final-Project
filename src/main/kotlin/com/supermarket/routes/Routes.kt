package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.registerRoutes() {
    routing {
        get("/") {
            call.respondText("Welcome to the Supermarket System")
        }

        get("/login") {
            call.respondText(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Login</title>
                </head>
                <body>
                    <h1>Login</h1>
                    <form>
                        <input type="text" placeholder="Username"><br><br>
                        <input type="password" placeholder="Password"><br><br>
                        <button>Login</button>
                    </form>
                </body>
                </html>
                """.trimIndent(),
                ContentType.Text.Html
            )
        }
    }
}
