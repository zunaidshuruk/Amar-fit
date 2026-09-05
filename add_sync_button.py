import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add context and launcher to BentoDashboard parameters
# Actually, the best place is to add it inside BentoDashboard body.
# We need permission launcher for health connect inside BentoDashboard.
# But `syncWithHealthConnect` just attempts to sync if permission is granted.
# Let's see how sync is done: `viewModel.syncWithHealthConnect(context)`
# If not granted, they can go to settings.
# Let's add a sync icon.
sync_icon = """      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var isHealthConnectAvailable by remember { mutableStateOf(androidx.health.connect.client.HealthConnectClient.getSdkStatus(context) == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) }
        
        if (isHealthConnectAvailable) {
          IconButton(
            onClick = {
              android.widget.Toast.makeText(context, "Syncing with Health Connect...", android.widget.Toast.LENGTH_SHORT).show()
              viewModel.syncWithHealthConnect(context)
            },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color.White)
              .border(1.dp, Slate200, CircleShape)
          ) {
             Icon(Icons.Default.Sync, contentDescription = "Sync Health Connect", tint = Emerald600)
          }
        }

        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Emerald200)
            .border(2.dp, Color.White, CircleShape)
            .shadow(2.dp, CircleShape)
            .clickable { navController.navigate("settings") },
          contentAlignment = Alignment.Center
        ) {
          if (!profile?.profilePictureUri.isNullOrEmpty()) {
              coil.compose.AsyncImage(
                  model = profile?.profilePictureUri,
                  contentDescription = "Profile Picture",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = androidx.compose.ui.layout.ContentScale.Crop
              )
          } else {
              Text(
                  text = initial,
                  fontWeight = FontWeight.Bold,
                  color = Emerald800,
                  fontSize = 18.sp
              )
          }
        }
      }"""

old_box = """      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Emerald200)
          .border(2.dp, Color.White, CircleShape)
          .shadow(2.dp, CircleShape)
          .clickable { navController.navigate("settings") },
        contentAlignment = Alignment.Center
      ) {
        if (!profile?.profilePictureUri.isNullOrEmpty()) {
            coil.compose.AsyncImage(
                model = profile?.profilePictureUri,
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                color = Emerald800,
                fontSize = 18.sp
            )
        }
      }"""

if old_box in content:
    content = content.replace(old_box, sync_icon)
    content = content.replace('import androidx.compose.material.icons.filled.Star', 'import androidx.compose.material.icons.filled.Star\nimport androidx.compose.material.icons.filled.Sync')
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
