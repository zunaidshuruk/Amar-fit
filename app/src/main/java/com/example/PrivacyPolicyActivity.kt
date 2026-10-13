package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.data.model.LegalContent
import com.example.presentation.settings.LegalDocumentScreen
import com.example.ui.theme.MyApplicationTheme

/** Opened from Health Connect's permission screen ("how is my data used"): shows the full privacy policy. */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme(darkTheme = true) {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    content = LegalContent.PRIVACY_POLICY,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
