package com.supermarket.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val username: String, val role: String)
