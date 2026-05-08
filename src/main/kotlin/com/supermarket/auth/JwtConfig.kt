package com.supermarket.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.supermarket.models.Role
import java.util.Date

// Handles JWT token creation and verification for authentication.
object JwtConfig {
    // Secret key used to sign JWT tokens.
    lateinit var secret: String

    // JWT issuer identifier.
    lateinit var issuer: String

    // Intended audience for the JWT token.
    lateinit var audience: String

    // Token expiration time in milliseconds (1 hour).
    private val expiresInMs = 3_600_000L

    // Generates a signed JWT token containing the username and user role.
    fun generateToken(
        username: String,
        role: Role,
    ): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(username)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMs))
            .sign(Algorithm.HMAC256(secret))
    }

    // Returns a JWT verifier configured with issuer, audience, and secret.
    fun getVerifier() =
        JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
}
