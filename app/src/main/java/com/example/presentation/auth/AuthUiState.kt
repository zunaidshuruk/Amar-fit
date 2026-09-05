package com.example.presentation.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Authenticated : AuthUiState
    data object EmailNotFound : AuthUiState
    data object InvalidCredentials : AuthUiState
    data class ValidationError(val msg: String) : AuthUiState
    data class Error(val msg: String) : AuthUiState
}
