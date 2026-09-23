package com.example.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.presentation.scanner.CameraPreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@Composable
fun QrScannerScreen(
    onCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraPreviewView(
                onImageCaptured = { bitmap ->
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val scanner = BarcodeScanning.getClient()
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                            if (!value.isNullOrBlank()) {
                                onCodeScanned(value)
                            } else {
                                errorMessage = "No QR code detected -- try again."
                            }
                        }
                        .addOnFailureListener { e ->
                            errorMessage = "Scan failed: ${e.message}"
                        }
                },
                onClose = onClose
            )

            errorMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2000)
                    errorMessage = null
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
