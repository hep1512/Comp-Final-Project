package com.supermarket.models


import kotlinx.serialization.Serializable

enum class Role {
    USER, EMPLOYEE, ADMIN
    }

    data class User(
        val username: String,
            val passwordHash: String,
                val role: Role
                )

                @Serializable
                data class LoginRequest(val username: String, val password: String)

                @Serializable
                data class LoginResponse(val token: String, val role: String)
                