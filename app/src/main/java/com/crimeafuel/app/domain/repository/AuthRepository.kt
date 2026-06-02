package com.crimeafuel.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val currentUserId: String?
    val currentUserEmail: String?

    suspend fun loginWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>
    suspend fun loginWithPhone(phoneNumber: String, verificationCode: String): Result<Unit>
    suspend fun sendPhoneVerification(phoneNumber: String): Result<String> // returns verificationId
    fun logout()
}
