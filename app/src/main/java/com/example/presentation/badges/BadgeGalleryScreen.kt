package com.example.presentation.badges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ALL_BADGES
import com.example.data.model.BadgeDefinition
import com.example.data.model.BadgeTier
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.components.HeroStatCard
import com.example.ui.theme.SignatureAccent
import kotlinx.coroutines.delay

/** Medallion ring color per tier, mirroring Apple Fitness' bronze/silver/gold styling
 * plus a "platinum" tier in the app's own signature lime for the rarest badges. */
private fun tierColor(tier: BadgeTier): Color = when (tier) {
    BadgeTier.BRONZE -> Color(0xFFCD7F32)
    BadgeTier.SILVER -> Color(0xFFC7D0D6)
    BadgeTier.GOLD -> Color(0xFFFFD54A)
    BadgeTier.PLATINUM -> SignatureAccent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeGalleryScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {
    val profile by viewModel.userProfile.collectAsState()
    val earnedBadgeIds = profile?.badges?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    val earnedCount = ALL_BADGES.count { it.id in earnedBadgeIds }
    val totalPoints = profile?.points ?: 0

    // Single shared rotation driver for every earned badge's shine sweep, instead of
    // one infinite animation per medallion.
    val infiniteTransition = rememberInfiniteTransition(label = "badgeShine")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    var selectedBadge by remember { mutableStateOf<BadgeDefinition?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Badges",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Earn badges and reward points by completing healthy daily habits.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HeroStatCard(
                    label = "BADGES EARNED",
                    value = "$earnedCount / ${ALL_BADGES.size}",
                    caption = "$totalPoints points earned",
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Detail panel for a tapped badge.
                selectedBadge?.let { badge ->
                    val isEarned = badge.id in earnedBadgeIds
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedBadge = null }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = badge.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${badge.points} pts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = tierColor(badge.tier)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = badge.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEarned) "Earned" else "Locked · tap again to close",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEarned) tierColor(badge.tier) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        itemsIndexed(ALL_BADGES) { index, badge ->
            BadgeMedallion(
                badge = badge,
                isEarned = badge.id in earnedBadgeIds,
                isSelected = selectedBadge?.id == badge.id,
                shineRotation = shineRotation,
                entranceDelayMs = (index % 12) * 30L,
                onClick = { selectedBadge = if (selectedBadge?.id == badge.id) null else badge }
            )
        }
    }
}

@Composable
private fun BadgeMedallion(
    badge: BadgeDefinition,
    isEarned: Boolean,
    isSelected: Boolean,
    shineRotation: Float,
    entranceDelayMs: Long,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(badge.id) {
        delay(entranceDelayMs)
        visible = true
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isSelected) 1.06f else 1f,
        label = "badgePress"
    )

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.6f) + fadeIn()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer ring: a slow-rotating shine sweep for earned badges, a flat dim ring for locked ones.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isEarned) {
                                Modifier.graphicsLayer { rotationZ = shineRotation }
                            } else Modifier
                        )
                        .clip(CircleShape)
                        .background(
                            if (isEarned) {
                                Brush.sweepGradient(
                                    listOf(
                                        tierColor(badge.tier).copy(alpha = 0.35f),
                                        tierColor(badge.tier),
                                        tierColor(badge.tier).copy(alpha = 0.5f),
                                        tierColor(badge.tier).copy(alpha = 0.9f),
                                        tierColor(badge.tier).copy(alpha = 0.35f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                                    )
                                )
                            }
                        )
                )

                // Inner disc, sized to leave the outer ring visible as a colored border.
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badge.icon,
                        contentDescription = badge.displayName,
                        tint = if (isEarned) tierColor(badge.tier) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                if (!isEarned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = badge.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isEarned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            if (badge.points > 0) {
                Text(
                    text = "${badge.points} pts",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEarned) tierColor(badge.tier) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
