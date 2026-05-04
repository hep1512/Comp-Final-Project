package com.supermarket.data

import com.supermarket.models.User
import com.supermarket.repositories.UserRepository

val userStore: Map<String, User>
    get() = UserRepository.getAllUsers().associateBy { it.username }