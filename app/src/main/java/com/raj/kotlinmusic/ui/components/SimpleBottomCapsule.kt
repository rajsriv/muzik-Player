package com.raj.kotlinmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.raj.kotlinmusic.MusicViewModel
import androidx.compose.ui.graphics.Color

@Composable
fun SimpleBottomCapsule(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {    val theme = viewModel.currentPalette
    val isPlaying = viewModel.isPlaying
    val liquidGlassColors = if (theme.id == "liquid_glass") viewModel.getLiquidGlassColors(theme) else emptyList()
    val activeAccent = if (theme.id == "liquid_glass") liquidGlassColors[2] else theme.accent1

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
            .let {
                if (theme.id == "liquid_glass") {
                    it.background(liquidGlassColors[2].copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                } else {
                    it.background(theme.surface)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Pad content, let background bleed
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = { viewModel.shuffleEnabled = !viewModel.shuffleEnabled }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (viewModel.shuffleEnabled) activeAccent else theme.mutedText,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = { viewModel.previousSong() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = theme.text,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = theme.text,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = { viewModel.nextSong() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = theme.text,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Favorite Button
                val isFav = viewModel.currentSong?.isFavorite == true
                IconButton(onClick = { viewModel.currentSong?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) activeAccent else theme.mutedText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(24.dp) // Touch target height
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            viewModel.seekTo(fraction)
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            viewModel.seekTo(fraction)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(theme.mutedText.copy(alpha = 0.3f))
                )
                val safeProgress = if (viewModel.currentProgress.isNaN()) 0f else viewModel.currentProgress.coerceIn(0f, 1f)
                if (safeProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeProgress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(theme.text)
                    )
                }
            }
        }
    }
}
