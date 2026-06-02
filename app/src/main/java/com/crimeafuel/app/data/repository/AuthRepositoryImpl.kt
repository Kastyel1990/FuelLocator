package com.crimeafuel.app.data.repository

import com.crimeafuel.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _isLoggedIn.value = auth.currentUser != null
        }
    }

    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override val currentUserEmail: String?
        get() = firebaseAuth.currentUser?.email

    override suspend fun loginWithEmail(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginWithPhone(
        phoneNumber: String,
        verificationCode: String
    ): Result<Unit> {
        return Result.failure(UnsupportedOperationException("Phone auth not implemented"))
    }

    override suspend fun sendPhoneVerification(phoneNumber: String): Result<String> {
        return Result.failure(UnsupportedOperationException("Phone auth not implemented"))
    }

    override fun logout() {
        firebaseAuth.signOut()
    }
}
