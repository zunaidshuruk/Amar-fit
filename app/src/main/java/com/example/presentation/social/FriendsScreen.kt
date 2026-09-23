package com.example.presentation.social

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private fun generateQrBitmap(text: String, sizePx: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val matrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val incoming by viewModel.incomingFriendRequests.collectAsState()
    val outgoing by viewModel.outgoingFriendRequests.collectAsState()
    val actionMessage by viewModel.friendActionMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var codeInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.refreshFriendRequests() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearFriendActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Code") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Add Friend") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Requests") })
            }

            when (selectedTab) {
                0 -> {
                    val code = profile?.friendCode ?: ""
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (code.isNotBlank()) {
                            val qrBitmap = remember(code) { generateQrBitmap(code) }
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Your QR code",
                                modifier = Modifier.size(220.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(code, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Share this code so a friend can add you", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Your code is being generated -- try again in a moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                1 -> {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Text("Enter a friend's code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = it.uppercase() },
                            label = { Text("Friend Code") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.sendFriendRequestByCode(codeInput); codeInput = "" },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = codeInput.isNotBlank()
                        ) {
                            Text("Send Request")
                        }
                    }
                }
                2 -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (incoming.isNotEmpty()) {
                            item { Text("Incoming", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            items(incoming) { req ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(req.otherName, fontWeight = FontWeight.Medium)
                                    Row {
                                        IconButton(onClick = { viewModel.acceptFriendRequest(req.pairId) }) {
                                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.declineOrCancelFriendRequest(req.pairId) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                        if (outgoing.isNotEmpty()) {
                            item { Text("Sent", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            items(outgoing) { req ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(req.otherName, fontWeight = FontWeight.Medium)
                                    TextButton(onClick = { viewModel.declineOrCancelFriendRequest(req.pairId) }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                        if (incoming.isEmpty() && outgoing.isEmpty()) {
                            item {
                                Text(
                                    "No pending requests.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
