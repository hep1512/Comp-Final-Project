package com.supermarket.data

import com.supermarket.models.User
import com.supermarket.repositories.UserRepository

// Provides a read-only map of users keyed by username.
// The map is rebuilt each time the property is accessed
// using all users returned from the repository.
val userStore: Map<String, User>
    get() = UserRepository.getAllUsers().associateBy { it.username }
