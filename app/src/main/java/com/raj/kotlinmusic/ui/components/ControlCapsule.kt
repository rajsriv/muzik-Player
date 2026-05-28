package com.raj.kotlinmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as floatLerp
import com.raj.kotlinmusic.Song
import com.raj.kotlinmusic.ui.theme.ColorPalette
import kotlin.math.roundToInt

@Composable
fun ControlCapsule(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onStop: () -> Unit,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    transitionProgress: Float, // 0.0 = Mini, 1.0 = Full
    currentProgress: Float,
    currentTimeString: String,
    durationString: String,
    onSeek: (Float) -> Unit,
    theme: ColorPalette,
    onLyricsClick: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},

    modifier: Modifier = Modifier
) {
    // Interpolated values
    val capsuleHeight = lerp(80.dp, 180.dp, transitionProgress)
    val capsuleWidthPercent = 0.92f
    val cornerRadius = 20.dp
    
    val miniAlpha = 1f - transitionProgress
    val fullAlpha = transitionProgress

    val defaultCoral = Color(0xFFEE6557)
    val defaultLime = Color(0xFFD3E382)
    val defaultYellow = Color(0xFFF5C754)
    val resolvedAccentColor = when (song.accentColor) {
        defaultCoral -> theme.accent1
        defaultLime -> theme.accent2
        defaultYellow -> theme.accent3
        else -> {
            val hash = (song.title + song.artist).hashCode()
            val accents = listOf(theme.accent1, theme.accent2, theme.accent3, theme.accent4)
            val index = Math.abs(hash) % accents.size
            accents[index]
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(capsuleWidthPercent)
            .height(capsuleHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (theme.id == "liquid_glass") resolvedAccentColor.copy(alpha = 0.8f) else resolvedAccentColor)
            .let {
                if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(cornerRadius)) else it
            }
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        // --- Full Player Controls (Visible when transitionProgress > 0) ---
        if (transitionProgress > 0.1f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fullAlpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Seek Bar Area
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = currentProgress,
                        onValueChange = onSeek,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.background,
                            activeTrackColor = theme.background,
                            inactiveTrackColor = theme.background.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(currentTimeString, color = theme.background.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text(durationString, color = theme.background.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }

                // Middle Row: Lyrics, Prev, Play/Pause, Next, Spacer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Lyrics */ }) {
                        if (song.lyricsAvailable) {
                            Icon(Icons.Default.TextSnippet, "Lyrics", tint = theme.background)
                        }
                    }
                    
                    IconButton(onClick = onPrevious) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = theme.background,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Main Play/Pause
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(theme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = theme.text,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = theme.background,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = onStop) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = theme.background,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Row: Like, Repeat, Shuffle, More
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (song.isFavorite) theme.accent1 else theme.background.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onRepeatToggle) {
                        Icon(
                            Icons.Default.Repeat, 
                            "Repeat", 
                            tint = if (repeatEnabled) theme.background else theme.background.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onShuffleToggle) {
                        Icon(
                            Icons.Default.Shuffle, 
                            "Shuffle", 
                            tint = if (shuffleEnabled) theme.background else theme.background.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(
                        onClick = onLyricsClick
                    ) {
                        Icon(Icons.Default.Lyrics, "Lyrics", tint = theme.background.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // --- Mini Player Controls (Visible when transitionProgress < 1) ---
        if (transitionProgress < 0.9f) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(miniAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Artwork
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.background)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = theme.background,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onStop) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = theme.background
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = theme.background
                    )
                }
            }
        }
    }
}
