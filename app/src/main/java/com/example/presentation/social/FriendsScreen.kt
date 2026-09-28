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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SportsScore
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
import com.example.data.repository.FirebaseManager
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
fun FriendsScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToDm: (String, String) -> Unit = { _, _ -> }
) {
    val profile by viewModel.userProfile.collectAsState()
    val incoming by viewModel.incomingFriendRequests.collectAsState()
    val outgoing by viewModel.outgoingFriendRequests.collectAsState()
    val actionMessage by viewModel.friendActionMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var codeInput by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }
    var selectedFriendUid by remember { mutableStateOf<String?>(null) }
    val friendsList by viewModel.friendsList.collectAsState()
    val selectedFriendStats by viewModel.selectedFriendStats.collectAsState()
    val challenges by viewModel.challenges.collectAsState()
    val challengeActionMessage by viewModel.challengeActionMessage.collectAsState()
    val activityFeed by viewModel.activityFeed.collectAsState()
    val kudosSentTo by viewModel.kudosSentTo.collectAsState()
    var friendPendingRemoval by remember { mutableStateOf<FirebaseManager.FriendInfo?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshFriendRequests() }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) viewModel.fetchFriendsList()
        if (selectedTab == 4) viewModel.fetchChallenges()
        if (selectedTab == 5) viewModel.fetchActivityFeed()
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearFriendActionMessage()
        }
    }

    LaunchedEffect(challengeActionMessage) {
        challengeActionMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearChallengeActionMessage()
            selectedTab = 4
            selectedFriendUid = null
            viewModel.clearSelectedFriendStats()
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
                },
                actions = {
                    IconButton(onClick = onNavigateToLeaderboard) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Code", maxLines = 1) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Add Friend", maxLines = 1) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Requests", maxLines = 1) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Friends", maxLines = 1) })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Challenges", maxLines = 1) })
                Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Feed", maxLines = 1) })
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
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showQrScanner = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR Code")
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
                3 -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (friendsList.isEmpty()) {
                            item {
                                Text(
                                    "No friends yet -- add one from the Add Friend tab.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(friendsList) { friend ->
                            LaunchedEffect(friend.uid) { viewModel.checkKudosSentToday(friend.uid) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (selectedFriendUid == friend.uid) {
                                            selectedFriendUid = null
                                            viewModel.clearSelectedFriendStats()
                                        } else {
                                            selectedFriendUid = friend.uid
                                            viewModel.fetchFriendStats(friend.uid)
                                        }
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(friend.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${friend.currentStreak} day streak · ${friend.badgeCount} badges", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${friend.points} pts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                if (selectedFriendUid == friend.uid) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val stats = selectedFriendStats
                                    if (stats == null) {
                                        Text("Loading stats...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("Today's progress (${stats.date})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Steps: ${stats.steps} / ${stats.stepGoal}", fontSize = 13.sp)
                                        Text("Water: ${stats.waterLiters}L / ${stats.waterGoal}L", fontSize = 13.sp)
                                        Text("Sleep: ${stats.sleepHours}h / ${stats.sleepGoal}h", fontSize = 13.sp)
                                        Text("Calories: ${stats.caloriesConsumed} / ${stats.calorieGoal}", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.createChallengeWithFriend(friend.uid) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Challenge to a 7-Day Steps Race")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val alreadySentKudos = kudosSentTo.contains(friend.uid)
                                            OutlinedButton(
                                                onClick = { if (!alreadySentKudos) viewModel.sendKudosToFriend(friend.uid) },
                                                enabled = !alreadySentKudos,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (alreadySentKudos) "Kudos sent" else "Send Kudos")
                                            }
                                            OutlinedButton(
                                                onClick = { friendPendingRemoval = friend },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.PersonRemove, contentDescription = "Remove Friend")
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { onNavigateToDm(friend.pairId, friend.name) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Message ${friend.name}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (challenges.isEmpty()) {
                            item {
                                Text(
                                    "No challenges yet -- start one from a friend's expanded stats.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(challenges) { challenge ->
                            val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("vs ${challenge.otherName}", fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.SportsScore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Text("${challenge.startDate} to ${challenge.endDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("You: ${challenge.myProgress} steps", fontSize = 14.sp)
                                Text("${challenge.otherName}: ${challenge.otherProgress} steps", fontSize = 14.sp)
                                if (challenge.status == "completed") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val resultText = when (challenge.winnerUid) {
                                        myUid -> "You won! +150 pts"
                                        null -> "It's a tie."
                                        else -> "${challenge.otherName} won."
                                    }
                                    Text(resultText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("In progress", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                5 -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activityFeed.isEmpty()) {
                            item {
                                Text(
                                    "No activity yet -- badges, challenge wins, and Kudos from you and your friends will show up here.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 24.dp)
                                )
                            }
                        }
                        items(activityFeed) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DynamicFeed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("${entry.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(entry.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerScreen(
            onCodeScanned = { code ->
                showQrScanner = false
                viewModel.sendFriendRequestByCode(code)
            },
            onClose = { showQrScanner = false }
        )
    }

    friendPendingRemoval?.let { friend ->
        AlertDialog(
            onDismissRequest = { friendPendingRemoval = null },
            title = { Text("Remove ${friend.name}?") },
            text = { Text("You'll need to send a new friend request to reconnect.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFriend(friend.pairId)
                    friendPendingRemoval = null
                    selectedFriendUid = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { friendPendingRemoval = null }) { Text("Cancel") }
            }
        )
    }
}
