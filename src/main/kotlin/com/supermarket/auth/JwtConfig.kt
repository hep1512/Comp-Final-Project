package com.supermarket.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.supermarket.models.Role
import java.util.Date

object JwtConfig {
    lateinit var secret: String
    lateinit var issuer: String
    lateinit var audience: String

    private val expiresInMs = 3_600_000L // this is 1 hour in seconds

    fun generateToken(username: String, role: Role): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(username)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMs))
            .sign(Algorithm.HMAC256(secret))
    }

    fun getVerifier() = JWT.require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
}
