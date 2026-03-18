package com.supermarket.data

import com.supermarket.models.Role
import com.supermarket.models.User
import java.security.MessageDigest

// ***********someone can update this to something better****************
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// basic users for testing
val userStore: MutableMap<String, User> = mutableMapOf(
    "user" to User("user",   hashPassword("password"), Role.USER),
    "employee123" to User("employee123",     hashPassword("employee"),      Role.EMPLOYEE),
    "adminuser" to User("adminuser", hashPassword("admin"),    Role.ADMIN)
)
