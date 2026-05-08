package com.supermarket

import com.supermarket.auth.JwtConfig
import com.supermarket.database.configureDatabase
import com.supermarket.models.UserSession
import com.supermarket.routes.registerRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // loadup the session cookies
    configureDatabase()

    install(Sessions) {
        cookie<UserSession>("SESSION", SessionStorageMemory()) {
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = 3600
        }
    }

    // Load JWT config from yaml
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    JwtConfig.secret = jwtSecret
    JwtConfig.issuer = jwtIssuer
    JwtConfig.audience = jwtAudience

    // JSON serialisation
    install(ContentNegotiation) {
        json()
    }

    // error responses
    install(StatusPages) {
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respondText(
                "401 Unauthorized — please log in.",
                status = HttpStatusCode.Unauthorized,
            )
        }

        status(HttpStatusCode.Forbidden) { call, _ ->
            call.respondText(
                "403 Forbidden — you don't have permission.",
                status = HttpStatusCode.Forbidden,
            )
        }
    }

    // JWT authentication
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(JwtConfig.getVerifier())

            validate { credential ->
                if (credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token missing or invalid.")
            }
        }
    }

    registerRoutes()
}
