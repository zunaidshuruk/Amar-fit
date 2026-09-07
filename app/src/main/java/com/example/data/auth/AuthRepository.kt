package com.example.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data object Success : AuthResult()
    data object EmailNotFound : AuthResult()
    data object InvalidCredentials : AuthResult()
    data object EmailVerificationRequired : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signInWithGoogle(credential: androidx.credentials.Credential): AuthResult
    suspend fun resendVerificationEmail(): AuthResult
    suspend fun checkEmailVerified(): AuthResult
}

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            if (user?.providerData?.any { it.providerId == "password" } == true && !user.isEmailVerified) {
                AuthResult.EmailVerificationRequired
            } else {
                AuthResult.Success
            }
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
            try {
                auth.currentUser?.sendEmailVerification()?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            AuthResult.EmailVerificationRequired
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown sign up error")
        }
    }

    override suspend fun resendVerificationEmail(): AuthResult {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.sendEmailVerification().await()
                AuthResult.Success
            } else {
                AuthResult.Error("No user currently signed in.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send verification email.")
        }
    }

    override suspend fun checkEmailVerified(): AuthResult {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.reload().await()
                if (user.isEmailVerified) {
                    AuthResult.Success
                } else {
                    AuthResult.EmailVerificationRequired
                }
            } else {
                AuthResult.Error("No user currently signed in.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to verify email status.")
        }
    }

    override suspend fun signInWithGoogle(credential: androidx.credentials.Credential): AuthResult {
        return try {
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = com.google.firebase.auth.GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                auth.signInWithCredential(firebaseCredential).await()
                
                if (auth.currentUser != null) {
                    AuthResult.Success
                } else {
                    AuthResult.Error("Google Sign-In succeeded, but no current user found.")
                }
            } else {
                AuthResult.Error("Invalid Google credential type or not a custom credential.")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown Google sign-in error")
        }
    }
}
