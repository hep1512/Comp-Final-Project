package com.supermarket.repositories

import com.supermarket.database.Users
import com.supermarket.models.Role
import com.supermarket.models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest

object UserRepository {

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun rowToUser(row: ResultRow): User {
        val role = when (row[Users.role]) {
            "admin" -> Role.ADMIN
            "picker", "warehouse_manager" -> Role.EMPLOYEE
            else -> Role.USER
        }
        return User(
            username = row[Users.firstName],
            passwordHash = row[Users.passwordHash],
            role = role
        )
    }

    fun findByUsername(username: String): User? = transaction {
        Users.selectAll()
            .map { rowToUser(it) }
            .firstOrNull { it.username == username }
    }

    fun getAllUsers(): List<User> = transaction {
        Users.selectAll()
            .map { rowToUser(it) }
    }

    fun createUser(username: String, password: String, role: Role) = transaction {
        val exists = Users.selectAll()
            .map { it[Users.firstName] }
            .any { it == username }

        if (!exists) {
            val dbRole = when (role) {
                Role.ADMIN -> "admin"
                Role.EMPLOYEE -> "picker"
                Role.USER -> "customer"
            }
            Users.insert {
                it[firstName] = username
                it[lastName] = username
                it[email] = "$username@supermarket.com"
                it[passwordHash] = hashPassword(password)
                it[Users.role] = dbRole
            }
        }
    }
}