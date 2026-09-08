package com.example.presentation.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.components.MarkdownText
import com.example.ui.components.MealTypeSelector
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.Background
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate900
import com.example.ui.theme.TextPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.Executors

import org.json.JSONObject

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val scanResult by viewModel.scanResult.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var parsedName by remember { mutableStateOf("") }
    var parsedCategory by remember { mutableStateOf("") }
    var parsedCalories by remember { mutableStateOf(0) }
    var parsedDescription by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf("Snack") }

    LaunchedEffect(scanResult) {
        if (scanResult != null) {
            selectedMealType = "Snack"
            try {
                // Find JSON block if AI wrapped it in markdown or something
                val jsonString = scanResult!!.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$jsonString}")
                parsedName = json.optString("name", "Unknown Food")
                parsedCategory = json.optString("category", "Uncategorized")
                parsedCalories = json.optInt("calories", 0)
                parsedDescription = json.optString("description", "")
            } catch (e: Exception) {
                parsedName = "Scan Failed"
                parsedDescription = "Could not parse AI response: ${e.message}"
                parsedCategory = "Error"
                parsedCalories = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
            CameraPreviewView(
                onImageCaptured = { bitmap ->
                    val base64 = encodeBitmapToBase64(bitmap)
                    viewModel.analyzeImage(base64)
                },
                onClose = onNavigateBack
            )

            // Overlay for scanning progress
            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Emerald500)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing Bangladeshi Dish...", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }

            // Overlay for scan result
            if (scanResult != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scan Result", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Emerald900)
                            IconButton(onClick = { 
                                viewModel.clearScanResult() 
                                selectedMealType = "Snack"
                            }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Slate100)
                        
                        MarkdownText(
                            text = "$parsedName ($parsedCalories kcal)\n\nCategory: $parsedCategory\n\n$parsedDescription",
                            color = TextPrimary,
                            modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        MealTypeSelector(
                            selectedMealType = selectedMealType,
                            onMealTypeSelected = { selectedMealType = it }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                if (parsedCalories > 0) {
                                    viewModel.logScannedFood(
                                        name = parsedName,
                                        category = parsedCategory,
                                        calories = parsedCalories,
                                        description = parsedDescription,
                                        mealType = selectedMealType
                                    )
                                }
                                viewModel.clearScanResult() 
                                selectedMealType = "Snack"
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                        ) {
                            Text("Awesome! Log this meal")
                        }
                    }
                }
            }
        }
    } else {
        // Permission Denied View
        Column(
            modifier = Modifier.fillMaxSize().background(Background).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is required to scan food.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Request Permission")
            }
        }
    }
}

@Composable
fun CameraPreviewView(
    onImageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier.padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // Capture button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
                .padding(8.dp)
                .background(Emerald500, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = imageProxyToBitmap(image)
                                image.close()
                                if (bitmap != null) {
                                    onImageCaptured(bitmap)
                                }
                            }
                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Capture", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

// Convert ImageProxy to Bitmap. Handling YUV_420_888 to JPEG usually provided by takePicture
private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    // Handle rotation
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun encodeBitmapToBase64(bitmap: Bitmap): String {
    // Resize bitmap to reduce payload size (Gemini API limit)
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, (512.toFloat() / bitmap.width * bitmap.height).toInt(), true)
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
