package com.example.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Background
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.TextPrimary

import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: com.example.presentation.viewmodel.ShasthoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onNavigateToOnboarding: () -> Unit, onNavigateToDashboard: () -> Unit = {}) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var isLoading by remember { mutableStateOf(false) }
    val auth: FirebaseAuth? = remember { 
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
    
    val isBn = selectedLanguage == "বাংলা (Bengali)"
    val txtWelcome = if (isBn) (if (isLogin) "আবার স্বাগতম" else "অ্যাকাউন্ট তৈরি করুন") else (if (isLogin) "Welcome back" else "Create an account")
    val txtEmail = if (isBn) "ইমেইল" else "Email"
    val txtPassword = if (isBn) "পাসওয়ার্ড" else "Password"
    val txtBtn = if (isBn) (if (isLogin) "লগইন" else "সাইন আপ") else (if (isLogin) "Login" else "Sign Up")
    val txtOr = if (isBn) " অথবা " else " OR "
    val txtSso = if (isBn) "গুগল দিয়ে চালিয়ে যান (SSO)" else "Continue with Google (SSO)"
    val txtPrompt = if (isBn) (if (isLogin) "অ্যাকাউন্ট নেই? " else "ইতিমধ্যেই অ্যাকাউন্ট আছে? ") else (if (isLogin) "Don't have an account? " else "Already have an account? ")
    val txtToggle = if (isBn) (if (isLogin) "সাইন আপ" else "লগইন") else (if (isLogin) "Sign Up" else "Login")


    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = "Language", tint = Emerald600)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedLanguage, color = Emerald600, fontWeight = FontWeight.Medium)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("English") },
                                onClick = { selectedLanguage = "English"; showLanguageMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("বাংলা (Bengali)") },
                                onClick = { selectedLanguage = "বাংলা (Bengali)"; showLanguageMenu = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
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
                    .background(Color.White)
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
                text = "আমার Fit",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Emerald600
            )
            
            Text(
                text = txtWelcome,
                fontSize = 16.sp,
                color = Slate500,
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
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
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (auth == null) {
                        Toast.makeText(context, "Firebase is not configured. Please upload google-services.json.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    isLoading = true
                    if (isLogin) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModel.syncDataOnLogin { hasProfile ->
                                        isLoading = false
                                        if (hasProfile) {
                                            onNavigateToDashboard()
                                        } else {
                                            onNavigateToOnboarding()
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModel.syncDataOnLogin { hasProfile ->
                                        isLoading = false
                                        if (hasProfile) {
                                            onNavigateToDashboard()
                                        } else {
                                            onNavigateToOnboarding()
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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
                Divider(modifier = Modifier.weight(1f), color = Slate100)
                Text(txtOr, color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(modifier = Modifier.weight(1f), color = Slate100)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { onNavigateToOnboarding() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = txtSso,
                    color = TextPrimary,
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
                    color = Slate500
                )
                Text(
                    text = txtToggle,
                    color = Emerald600,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isLogin = !isLogin }
                )
            }
        }
    }
}
