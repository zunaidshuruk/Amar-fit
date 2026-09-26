package com.example.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600

import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: com.example.presentation.viewmodel.ShasthoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    authViewModel: com.example.presentation.auth.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = com.example.presentation.auth.AuthViewModelFactory()),
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var hasAcceptedTerms by remember { mutableStateOf(false) }
    var showPrivacyPolicyFromAuth by remember { mutableStateOf(false) }
    var showTermsFromAuth by remember { mutableStateOf(false) }
    
    val uiState by authViewModel.uiState.collectAsState()
    val isLoading = uiState is com.example.presentation.auth.AuthUiState.Loading
    val isVerificationRequired = uiState is com.example.presentation.auth.AuthUiState.EmailVerificationRequired
    var showEmailNotFoundDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resendCooldownSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            resendCooldownSeconds -= 1
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is com.example.presentation.auth.AuthUiState.Authenticated -> {
                viewModel.syncDataOnLogin { hasProfile ->
                    authViewModel.resetState()
                    if (hasProfile) {
                        onNavigateToDashboard()
                    } else {
                        onNavigateToOnboarding()
                    }
                }
            }
            is com.example.presentation.auth.AuthUiState.EmailNotFound -> {
                showEmailNotFoundDialog = true
            }
            is com.example.presentation.auth.AuthUiState.InvalidCredentials -> {
                android.widget.Toast.makeText(context, "Invalid email or password", android.widget.Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            is com.example.presentation.auth.AuthUiState.ValidationError -> {
                android.widget.Toast.makeText(context, state.msg, android.widget.Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            is com.example.presentation.auth.AuthUiState.PasswordResetEmailSent -> {
                showForgotPasswordDialog = false
                android.widget.Toast.makeText(context, "Password reset email sent. Check your inbox.", android.widget.Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            is com.example.presentation.auth.AuthUiState.Error -> {
                showErrorDialog = state.msg
            }
            else -> {}
        }
    }
    
    if (showEmailNotFoundDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEmailNotFoundDialog = false
                authViewModel.resetState()
            },
            title = { Text("Account Not Found") },
            text = { Text("No account exists with this email. Would you like to sign up instead?") },
            confirmButton = {
                TextButton(onClick = { 
                    showEmailNotFoundDialog = false
                    isLogin = false
                    authViewModel.resetState()
                }) {
                    Text("Sign Up")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEmailNotFoundDialog = false
                    authViewModel.resetState()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showErrorDialog != null) {
        AlertDialog(
            onDismissRequest = { 
                showErrorDialog = null 
                authViewModel.resetState()
            },
            title = { Text("Authentication Error") },
            text = { Text(showErrorDialog ?: "An unknown error occurred.") },
            confirmButton = {
                TextButton(onClick = { 
                    showErrorDialog = null
                    authViewModel.resetState()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                authViewModel.resetState()
            },
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your email address and we'll send you a link to reset your password.")
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { authViewModel.sendPasswordReset(resetEmail) }) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    authViewModel.resetState()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    val txtWelcome = if (isLogin) "Welcome back" else "Create an account"
    val txtEmail = "Email"
    val txtPassword = "Password"
    val txtBtn = if (isLogin) "Login" else "Sign Up"
    val txtOr = " OR "
    val txtSso = "Continue with Google (SSO)"
    val txtPrompt = if (isLogin) "Don't have an account? " else "Already have an account? "
    val txtToggle = if (isLogin) "Sign Up" else "Login"


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (isVerificationRequired) {
            val userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: email
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Verify Email",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Verify your email",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We've sent a verification link to $userEmail. Please check your inbox and verify your email to continue.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { authViewModel.checkVerificationStatus() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I've verified",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        if (resendCooldownSeconds == 0) {
                            resendCooldownSeconds = 30
                            authViewModel.resendVerificationEmail()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = resendCooldownSeconds == 0 && !isLoading
                ) {
                    Text(
                        text = if (resendCooldownSeconds > 0) {
                            "Resend email (${resendCooldownSeconds}s)"
                        } else {
                            "Resend email"
                        },
                        color = if (resendCooldownSeconds == 0 && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Use a different account",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        viewModel.logout {
                            authViewModel.resetState()
                        }
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Icon",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "KardIQ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = txtWelcome,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(txtEmail) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(txtPassword) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLogin) {
                    TextButton(
                        onClick = {
                            resetEmail = email
                            showForgotPasswordDialog = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot Password?", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = hasAcceptedTerms, onCheckedChange = { hasAcceptedTerms = it })
                        Text("I agree to the ", fontSize = 11.sp)
                        Text(
                            "Privacy Policy",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showPrivacyPolicyFromAuth = true }
                        )
                        Text(" and ", fontSize = 11.sp)
                        Text(
                            "Terms of Service",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showTermsFromAuth = true }
                        )
                    }
                }
                
                Button(
                    onClick = { 
                        if (isLogin) {
                            authViewModel.signIn(email, password)
                        } else {
                            authViewModel.signUp(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && (isLogin || hasAcceptedTerms)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = txtBtn,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(txtOr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        if (isLoading) return@OutlinedButton
                        coroutineScope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(context.getString(R.string.default_web_client_id))
                                    .build()
                                    
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                    
                                val credentialManager = CredentialManager.create(context)
                                val result = credentialManager.getCredential(context = context, request = request)
                                authViewModel.signInWithGoogle(result.credential)
                            } catch (e: GetCredentialCancellationException) {
                                authViewModel.resetState()
                            } catch (e: NoCredentialException) {
                                authViewModel.setCustomError("No Google account found on this device. Add one in your device's Settings > Accounts, or sign in with email instead.")
                            } catch (e: GetCredentialException) {
                                authViewModel.setCustomError(e.message ?: "Google Sign-In failed")
                            } catch (e: Exception) {
                                authViewModel.setCustomError(e.message ?: "Unknown error")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = txtSso,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = txtPrompt,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = txtToggle,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isLogin = !isLogin }
                    )
                }
            }
        }
    }

    if (showPrivacyPolicyFromAuth) {
        Dialog(onDismissRequest = { showPrivacyPolicyFromAuth = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            com.example.presentation.settings.LegalDocumentScreen(
                title = "Privacy Policy",
                content = com.example.data.model.LegalContent.PRIVACY_POLICY,
                onNavigateBack = { showPrivacyPolicyFromAuth = false }
            )
        }
    }
    if (showTermsFromAuth) {
        Dialog(onDismissRequest = { showTermsFromAuth = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            com.example.presentation.settings.LegalDocumentScreen(
                title = "Terms of Service",
                content = com.example.data.model.LegalContent.TERMS_OF_SERVICE,
                onNavigateBack = { showTermsFromAuth = false }
            )
        }
    }
}
