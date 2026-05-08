package com.supermarket.models

import kotlinx.serialization.Serializable

// Defines the different access levels available in the system
enum class Role {
    USER,
    EMPLOYEE,
    ADMIN,
}

// Represents a user account stored in the application
data class User(
    // Unique username used for login
    val username: String,
    // Securely hashed password for authentication
    val passwordHash: String,
    // Role assigned to the user for authorization
    val role: Role,
)

// Request body used when a user attempts to log in
@Serializable
data class LoginRequest(
    // Username entered by the user
    val username: String,
    // Plain text password submitted during login
    val password: String,
)

// Response returned after successful authentication
@Serializable
data class LoginResponse(
    // JWT token generated for authenticated access
    val token: String,
    // User role included for frontend authorization handling
    val role: String,
)
