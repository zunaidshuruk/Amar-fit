package com.example.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.auth.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (!validate(trimmedEmail, trimmedPassword)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.signIn(trimmedEmail, trimmedPassword)) {
                is AuthResult.Success -> _uiState.value = AuthUiState.Authenticated
                is AuthResult.EmailNotFound -> _uiState.value = AuthUiState.EmailNotFound
                is AuthResult.InvalidCredentials -> _uiState.value = AuthUiState.InvalidCredentials
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
            }
        }
    }

    fun signUp(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (!validate(trimmedEmail, trimmedPassword)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.signUp(trimmedEmail, trimmedPassword)) {
                is AuthResult.Success -> _uiState.value = AuthUiState.Authenticated
                is AuthResult.Error -> _uiState.value = AuthUiState.Error(result.message)
                else -> _uiState.value = AuthUiState.Error("Unexpected error during sign up")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validate(email: String, password: String): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = AuthUiState.ValidationError("Please enter email and password")
            return false
        }
        if (!"^[A-Za-z0-9+_.-]+@(.+)\$".toRegex().matches(email)) {
            _uiState.value = AuthUiState.ValidationError("Please enter a valid email format")
            return false
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.ValidationError("Password must be at least 6 characters")
            return false
        }
        return true
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository = FirebaseAuthRepository(FirebaseAuth.getInstance())
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
