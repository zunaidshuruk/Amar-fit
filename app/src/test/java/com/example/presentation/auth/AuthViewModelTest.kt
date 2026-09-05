package com.example.presentation.auth

import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private lateinit var repository: FakeAuthRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAuthRepository()
        viewModel = AuthViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn with valid credentials emits Authenticated`() {
        viewModel.signIn("test@test.com", "password123")
        assertEquals(AuthUiState.Authenticated, viewModel.uiState.value)
    }

    @Test
    fun `signIn with non-existent email emits EmailNotFound`() {
        repository.shouldReturnEmailNotFound = true
        viewModel.signIn("test@test.com", "password123")
        assertEquals(AuthUiState.EmailNotFound, viewModel.uiState.value)
    }

    @Test
    fun `signIn with invalid password emits InvalidCredentials`() {
        repository.shouldReturnInvalidCredentials = true
        viewModel.signIn("test@test.com", "wrongpass")
        assertEquals(AuthUiState.InvalidCredentials, viewModel.uiState.value)
    }

    @Test
    fun `empty email emits ValidationError`() {
        viewModel.signIn("", "password123")
        assertTrue(viewModel.uiState.value is AuthUiState.ValidationError)
    }

    @Test
    fun `invalid email format emits ValidationError`() {
        viewModel.signIn("invalidemail", "password123")
        assertTrue(viewModel.uiState.value is AuthUiState.ValidationError)
    }

    @Test
    fun `short password emits ValidationError`() {
        viewModel.signIn("test@test.com", "123")
        assertTrue(viewModel.uiState.value is AuthUiState.ValidationError)
    }
}

class FakeAuthRepository : AuthRepository {
    var shouldReturnEmailNotFound = false
    var shouldReturnInvalidCredentials = false
    var shouldReturnError = false

    override suspend fun signIn(email: String, password: String): AuthResult {
        return when {
            shouldReturnEmailNotFound -> AuthResult.EmailNotFound
            shouldReturnInvalidCredentials -> AuthResult.InvalidCredentials
            shouldReturnError -> AuthResult.Error("Test Error")
            else -> AuthResult.Success
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return if (shouldReturnError) AuthResult.Error("Test Error") else AuthResult.Success
    }
}
