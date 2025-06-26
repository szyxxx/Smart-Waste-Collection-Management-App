package com.bluebin.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.TPS,
    val approved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    ADMIN, TPS, DRIVER
} 