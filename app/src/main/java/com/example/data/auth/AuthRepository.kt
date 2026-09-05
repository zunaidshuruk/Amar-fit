package com.example.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data object Success : AuthResult()
    data object EmailNotFound : AuthResult()
    data object InvalidCredentials : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
}

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.EmailNotFound
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Note: Firebase changed how exceptions are thrown recently, but this matches typical requested handling.
            AuthResult.InvalidCredentials
        } catch (e: Exception) {
            // Further fallback for errorCode if it's a generic Exception wrap
            if (e.message?.contains("ERROR_USER_NOT_FOUND") == true) {
                AuthResult.EmailNotFound
            } else if (e.message?.contains("ERROR_WRONG_PASSWORD") == true || e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                AuthResult.InvalidCredentials
            } else {
                AuthResult.Error(e.message ?: "Unknown login error")
            }
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown sign up error")
        }
    }
}
