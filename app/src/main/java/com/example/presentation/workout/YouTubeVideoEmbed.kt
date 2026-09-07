package com.example.presentation.workout

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.Orange500
import com.example.ui.theme.Red500

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeVideoEmbed(
    videoId: String?,
    isLoading: Boolean,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(color = Orange500, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading video demonstration...",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            !videoId.isNullOrBlank() -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webChromeClient = WebChromeClient()
                            webViewClient = WebViewClient()
                            loadUrl("https://www.youtube.com/embed/$videoId?autoplay=0&playsinline=1&rel=0")
                        }
                    },
                    update = { webView ->
                        val targetUrl = "https://www.youtube.com/embed/$videoId?autoplay=0&playsinline=1&rel=0"
                        if (webView.url != targetUrl) {
                            webView.loadUrl(targetUrl)
                        }
                    }
                )
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Red500,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No in-app preview available",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val query = searchQuery.ifBlank { "exercise tutorial" }
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Red500),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search YouTube Tutorial", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
