package com.example.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LegalContent
import com.example.presentation.settings.LegalDocumentScreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsentGateScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var hasChecked by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Privacy Policy & Terms Update",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "We have updated our Privacy Policy (October 13, 2026). Please review and accept to continue using the app.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "What is new:\n" +
                        "\u2022 Your data stays in sync across your phones, including AI Assistant chats and workout history\n" +
                        "\u2022 Items you delete are removed on your other phones too\n" +
                        "\u2022 Every reminder and alert can be switched in Settings > Notifications\n" +
                        "\u2022 A clear explanation of what deleting your account removes",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = hasChecked,
                    onCheckedChange = { hasChecked = it }
                )
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text("I agree to the ", fontSize = 12.sp)
                    Text(
                        "Privacy Policy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showPrivacyPolicy = true }
                    )
                    Text(" and ", fontSize = 12.sp)
                    Text(
                        "Terms of Service",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showTerms = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAccept,
                enabled = hasChecked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Accept & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onDecline) {
                Text("Log Out", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showPrivacyPolicy) {
        Dialog(
            onDismissRequest = { showPrivacyPolicy = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LegalDocumentScreen(
                title = "Privacy Policy",
                content = LegalContent.PRIVACY_POLICY,
                onNavigateBack = { showPrivacyPolicy = false }
            )
        }
    }

    if (showTerms) {
        Dialog(
            onDismissRequest = { showTerms = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LegalDocumentScreen(
                title = "Terms of Service",
                content = LegalContent.TERMS_OF_SERVICE,
                onNavigateBack = { showTerms = false }
            )
        }
    }
}
