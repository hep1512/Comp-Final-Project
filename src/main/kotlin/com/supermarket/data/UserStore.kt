package com.supermarket.data

import com.supermarket.models.User
import com.supermarket.repositories.UserRepository

// userStore now queries Neon instead of in-memory map
val userStore: Map<String, User>
    get() = UserRepository.getAllUsers().associateBy { it.username }