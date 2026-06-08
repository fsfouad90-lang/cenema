package com.example.data.model

sealed interface UserSession {
    object LoggedOut : UserSession
    data class LoggedIn(
        val userId: String,
        val email: String,
        val subscriptionStatus: String
    ) : UserSession
}
