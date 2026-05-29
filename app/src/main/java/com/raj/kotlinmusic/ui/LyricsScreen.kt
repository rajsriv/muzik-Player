package com.raj.kotlinmusic.ui

import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import com.raj.kotlinmusic.AppScreen
import com.raj.kotlinmusic.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(
    androidx.compose.ui.text.ExperimentalTextApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun LyricsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val theme = viewModel.currentPalette
    val currentSong = viewModel.currentSong
    val lazyListState = rememberLazyListState()

    val defaultCoral = Color(0xFFEE6557)
    val defaultLime = Color(0xFFD3E382)
    val defaultYellow = Color(0xFFF5C754)
    val resolvedAccentColor = remember(currentSong, theme) {
        when (currentSong.accentColor) {
            defaultCoral -> theme.accent1
            defaultLime -> theme.accent2
            defaultYellow -> theme.accent3
            else -> {
                val hash = (currentSong.title + currentSong.artist).hashCode()
                val accents = listOf(theme.accent1, theme.accent2, theme.accent3, theme.accent4)
                val index = Math.abs(hash) % accents.size
                accents[index]
            }
        }
    }

    val topBarTextColor = Color.White

    val showSyncControls = viewModel.showLyricsSyncControls

    // Keep screen awake while on lyrics page
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle back button to return to player
    BackHandler {
        viewModel.currentAppScreen = AppScreen.PLAYER
    }

    // Active line detection with user offset adjustment
    val activeLineIndex = remember(viewModel.playbackPositionMs, viewModel.syncedLyricsList, viewModel.lyricsOffsetMs) {
        viewModel.syncedLyricsList.indexOfLast { (viewModel.playbackPositionMs + viewModel.lyricsOffsetMs) >= it.timeMs }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val topPaddingPx = remember(density) { with(density) { 120.dp.toPx().toInt() } }

    // Auto-scroll when active line changes
    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0) {
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height
            val itemHeight = 150 // Approx pixel height of a line
            // Calculate absolute center accounting for the 120.dp top content padding
            val offset = if (viewportHeight > 0) {
                -(viewportHeight / 2) + topPaddingPx + (itemHeight / 2)
            } else {
                -350
            }
            lazyListState.animateScrollToItem(index = activeLineIndex, scrollOffset = offset)
        }
    }

    // Auto-hide Save Button 5 seconds after it appears on screen
    LaunchedEffect(viewModel.showSaveLyricsButton) {
        if (viewModel.showSaveLyricsButton) {
            kotlinx.coroutines.delay(5000)
            viewModel.showSaveLyricsButton = false
        }
    }

    var showLyricsMenu by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedLines by remember { mutableStateOf(setOf<Int>()) }
    var showSharePreview by remember { mutableStateOf(false) }
    var selectedLyricsTextForPreview by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastTouchPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    var showImportPill by remember { mutableStateOf(false) }
    var customVideoIndex by remember { mutableStateOf(0) }
    var forceVideoMode by remember { mutableStateOf(false) }
    
    val videoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            viewModel.customVideoUris = uris.map { it.toString() }
            customVideoIndex = 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.position?.let {
                            lastTouchPosition = it
                        }
                    }
                }
            }
            .background(Color.Black)
            .padding(6.dp)
    ) {
        // Main Layout Column
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar Header inside a themed capsule container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                    .background(Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(vertical = 20.dp)
                ) {
                    // Row 1: Artwork + Title/Artist + Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini Artwork
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            if (currentSong.artworkRes != null) {
                                Image(
                                    painter = painterResource(id = currentSong.artworkRes),
                                    contentDescription = "Album Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentSong.title.take(1),
                                        color = topBarTextColor,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Song Title & Artist Info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currentSong.title,
                                color = topBarTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = currentSong.artist,
                                color = topBarTextColor.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Sync Offset Toggle Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (showSyncControls) topBarTextColor.copy(alpha = 0.25f) else topBarTextColor.copy(alpha = 0.1f))

                                .clickable {
                                    viewModel.showLyricsSyncControls = !viewModel.showLyricsSyncControls
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Adjust Lyric Offset",
                                tint = topBarTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Play / Pause Toggle Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(topBarTextColor.copy(alpha = 0.1f))
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = topBarTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Chevron Down to Exit/Close
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(topBarTextColor.copy(alpha = 0.1f))

                                .clickable {
                                    viewModel.currentAppScreen = AppScreen.PLAYER
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize Lyrics",
                                tint = topBarTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Row 2: Sync Controls (Displayed underneath when toggled active)
                    if (showSyncControls) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(topBarTextColor.copy(alpha = 0.15f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Decrease Offset (-0.5s)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(topBarTextColor.copy(alpha = 0.15f))
                                    .clickable { viewModel.updateLyricsOffset(currentSong, viewModel.lyricsOffsetMs - 500L) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-0.5s",
                                    color = topBarTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Current Offset Info & Reset
                            val offsetSec = viewModel.lyricsOffsetMs / 1000f
                            val offsetText = if (offsetSec >= 0) "+${String.format("%.1f", offsetSec)}s" else "${String.format("%.1f", offsetSec)}s"

                            Column(
                                modifier = Modifier
                                    .clickable { viewModel.updateLyricsOffset(currentSong, 0L) }, // Reset on tap
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sync Offset: $offsetText",
                                    color = topBarTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Tap to reset to 0.0s",
                                    color = topBarTextColor.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Increase Offset (+0.5s)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(topBarTextColor.copy(alpha = 0.15f))
                                    .clickable { viewModel.updateLyricsOffset(currentSong, viewModel.lyricsOffsetMs + 500L) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+0.5s",
                                    color = topBarTextColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Save Lyrics Button (Vanishing)
                    AnimatedVisibility(
                        visible = viewModel.showSaveLyricsButton,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(topBarTextColor.copy(alpha = 0.15f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { viewModel.saveLyricsOffline(currentSong) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = theme.background
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Save Lyrics Offline",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Lyrics Body Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (theme.id == "liquid_glass") Color(0xFF0C0E17) else theme.background)
            ) {
                val hasLyrics = (viewModel.isLyricsLoading || viewModel.syncedLyricsList.isNotEmpty() || (!viewModel.lyricsText.isNullOrEmpty() && viewModel.lyricsText?.startsWith("No lyrics") == false && viewModel.lyricsText?.startsWith("No matching lyrics") == false)) && !forceVideoMode
                
                if (hasLyrics && theme.id == "white_candy") {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Top-Left Pink Gradient
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(theme.accent1.copy(alpha = 0.6f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                radius = size.width * 1.2f
                            ),
                            center = androidx.compose.ui.geometry.Offset(0f, 0f),
                            radius = size.width * 1.2f
                        )
                        // Top-Right Yellow Gradient
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(theme.accent2.copy(alpha = 0.5f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                radius = size.width * 1.2f
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            radius = size.width * 1.2f
                        )
                    }
                }
                if (hasLyrics && theme.id == "liquid_glass") {
                    // Blurred Neon Ambient Glow in Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .alpha(0.4f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .align(Alignment.TopStart)
                                .background(theme.accent1, shape = RoundedCornerShape(150.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .align(Alignment.BottomEnd)
                                .background(theme.accent2, shape = RoundedCornerShape(125.dp))
                        )
                    }
                }

                if (!forceVideoMode && viewModel.isLyricsLoading) {
                    // Loading State
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = theme.accent1)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Fetching lyrics from LRCLIB...",
                            color = theme.text.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (!forceVideoMode && viewModel.syncedLyricsList.isNotEmpty()) {
                    val effectiveStyle = if (selectionMode) com.raj.kotlinmusic.LyricsStyle.WORD_BY_WORD else viewModel.lyricsStyle
                    if (effectiveStyle == com.raj.kotlinmusic.LyricsStyle.TYPOGRAPHIC) {
                        val activeLine = viewModel.syncedLyricsList.getOrNull(activeLineIndex)
                        if (activeLine != null) {
                            val progress by remember(viewModel.playbackPositionMs, activeLineIndex, viewModel.lyricsOffsetMs) {
                                derivedStateOf {
                                    val nextLine = viewModel.syncedLyricsList.getOrNull(activeLineIndex + 1)
                                    val lineDuration = if (nextLine != null) nextLine.timeMs - activeLine.timeMs else 4000L
                                    if (lineDuration > 0) {
                                        val effectivePos = viewModel.playbackPositionMs + viewModel.lyricsOffsetMs
                                        ((effectivePos - activeLine.timeMs).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                }
                            }
                            
                            val words = remember(activeLine.text) { activeLine.text.split(" ") }
                            val activeWordIndex by remember(progress, words.size) {
                                derivedStateOf {
                                    (progress * words.size).toInt().coerceIn(0, words.size - 1)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val chunks = remember(words) { words.chunked(2) }
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    chunks.forEachIndexed { chunkIndex, chunkWords ->
                                        val baseFontSize = if (chunkIndex % 2 == 0) 48.sp else 28.sp
                                        
                                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            chunkWords.forEachIndexed { i, word ->
                                                val actualWordIndex = chunkIndex * 2 + i
                                                val isCurrent = actualWordIndex == activeWordIndex
                                                val isFuture = actualWordIndex > activeWordIndex
                                                
                                                val alpha by androidx.compose.animation.core.animateFloatAsState(
                                                    targetValue = if (isFuture) 0f else 1f,
                                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 150)
                                                )
                                                val color by androidx.compose.animation.animateColorAsState(
                                                    targetValue = if (isCurrent) theme.accent1 else theme.text
                                                )
                                                val scale by androidx.compose.animation.core.animateFloatAsState(
                                                    targetValue = if (isCurrent) 1.15f else if (isFuture) 0.8f else 1.0f,
                                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)
                                                )

                                                Text(
                                                    text = word,
                                                    color = color,
                                                    fontSize = baseFontSize,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.graphicsLayer {
                                                        scaleX = scale
                                                        scaleY = scale
                                                        this.alpha = alpha
                                                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Time Synced Lyrics List
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 120.dp, bottom = if (selectionMode) 100.dp else 280.dp),
                            verticalArrangement = if (effectiveStyle == com.raj.kotlinmusic.LyricsStyle.BUBBLE) Arrangement.spacedBy(10.dp) else Arrangement.spacedBy(28.dp)
                        ) {
                        itemsIndexed(viewModel.syncedLyricsList) { index, line ->
                            val isActive = index == activeLineIndex && !selectionMode
                            val isSelected = selectedLines.contains(index)

                            val progress by remember(viewModel.playbackPositionMs, activeLineIndex, viewModel.lyricsOffsetMs) {
                                derivedStateOf {
                                    when {
                                        index < activeLineIndex -> 1f
                                        index > activeLineIndex -> 0f
                                        else -> {
                                            val nextLine = viewModel.syncedLyricsList.getOrNull(index + 1)
                                            val lineDuration = if (nextLine != null) nextLine.timeMs - line.timeMs else 4000L
                                            if (lineDuration > 0) {
                                                val effectivePos = viewModel.playbackPositionMs + viewModel.lyricsOffsetMs
                                                ((effectivePos - line.timeMs).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)
                                            } else {
                                                0f
                                            }
                                        }
                                    }
                                }
                            }

                            val words = remember(line.text) { line.text.split(" ") }
                            val activeWordIndex by remember(progress, words.size) {
                                derivedStateOf {
                                    (progress * words.size).toInt().coerceIn(0, words.size - 1)
                                }
                            }

                            val clickModifier = Modifier.pointerInput(viewModel.isPlaying, selectionMode, isSelected) {
                                detectTapGestures(
                                    onTap = {
                                        if (selectionMode) {
                                            selectedLines = if (isSelected) {
                                                selectedLines - index
                                            } else if (selectedLines.size < 4) {
                                                selectedLines + index
                                            } else {
                                                selectedLines
                                            }
                                        } else if (viewModel.isPlaying) {
                                            viewModel.seekToPosition(line.timeMs - viewModel.lyricsOffsetMs)
                                        }
                                    },
                                    onLongPress = { _ ->
                                        if (!viewModel.isPlaying && !selectionMode) {
                                            showLyricsMenu = true
                                        }
                                    }
                                )
                            }

                            if (effectiveStyle == com.raj.kotlinmusic.LyricsStyle.BUBBLE) {
                                val distance = kotlin.math.abs(index - activeLineIndex)
                                
                                val boxAlpha = when {
                                    selectionMode && isSelected -> 0.45f
                                    selectionMode && !isSelected -> 0.1f
                                    distance <= 1 -> 0.45f
                                    distance == 2 -> 0.15f
                                    else -> 0f
                                }
                                
                                val textColor = when {
                                    selectionMode && isSelected -> theme.text
                                    selectionMode && !isSelected -> theme.text.copy(alpha = 0.4f)
                                    distance <= 1 -> theme.text
                                    distance == 2 -> theme.text.copy(alpha = 0.6f)
                                    else -> theme.text.copy(alpha = 0.3f)
                                }
                                
                                val lineAlpha = if (selectionMode) 1f else if (distance > 3) 0f else 1f
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(lineAlpha)
                                        .then(clickModifier)
                                        .padding(horizontal = 24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.Black.copy(alpha = boxAlpha))
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = line.text,
                                            color = textColor,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 30.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(clickModifier)
                                        .padding(horizontal = 24.dp)
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        words.forEachIndexed { wordIndex, word ->
                                            val wordColor = when {
                                                selectionMode && isSelected -> theme.text
                                                selectionMode && !isSelected -> theme.text.copy(alpha = 0.35f)
                                                !isActive -> theme.text.copy(alpha = 0.35f)
                                                wordIndex == activeWordIndex -> theme.accent1
                                                else -> theme.text
                                            }
    
                                            val wordShadow = if (!selectionMode && isActive && wordIndex == activeWordIndex) {
                                                androidx.compose.ui.graphics.Shadow(
                                                    color = theme.accent1.copy(alpha = 0.8f),
                                                    blurRadius = 14f,
                                                    offset = androidx.compose.ui.geometry.Offset(0f, 0f)
                                                )
                                            } else null
    
                                            Text(
                                                text = word,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 32.sp,
                                                style = LocalTextStyle.current.copy(
                                                    color = wordColor,
                                                    shadow = wordShadow
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                } else if (!forceVideoMode && !viewModel.lyricsText.isNullOrEmpty() && viewModel.lyricsText?.startsWith("No lyrics") == false && viewModel.lyricsText?.startsWith("No matching lyrics") == false) {
                    // Plain Unsynced Lyrics
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentPadding = PaddingValues(top = 40.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                text = viewModel.lyricsText ?: "",
                                color = theme.text.copy(alpha = 0.85f),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 34.sp
                            )
                        }
                    }
                } else {
                    // No lyrics state -> Fade in Video
                    var showVideo by remember { mutableStateOf(false) }
                    var hideText by remember { mutableStateOf(false) }
                    var showSecondaryVideo by remember { mutableStateOf(theme.id != "white_candy") }
                    
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        showVideo = true
                        kotlinx.coroutines.delay(2000)
                        hideText = true
                    }

                    val baseVideoAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showVideo) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500)
                    )
                    
                    val primaryVideoAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showSecondaryVideo) 0f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
                    )
                    
                    val secondaryVideoAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (showSecondaryVideo) 1f else 0f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
                    )

                    val textAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (hideText) 0f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
                    )
                    
                    // Determine background color in case the video alpha is transparent
                    val defaultFadeColor = if (theme.id == "liquid_glass") Color(0xFF0C0E17) else theme.background
                    val currentFadeColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (showSecondaryVideo) Color.Black else defaultFadeColor,
                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(currentFadeColor)
                            .alpha(baseVideoAlpha)
                            .pointerInput(viewModel.customVideoUris, showImportPill) {
                                detectTapGestures(
                                    onLongPress = { showImportPill = true },
                                    onTap = {
                                        if (showImportPill) {
                                            showImportPill = false
                                        } else {
                                            if (viewModel.customVideoUris.isNotEmpty()) {
                                                customVideoIndex = (customVideoIndex + 1) % viewModel.customVideoUris.size
                                            } else {
                                                showSecondaryVideo = !showSecondaryVideo
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        if (viewModel.customVideoUris.isNotEmpty()) {
                            val safeVideoIndex = if (viewModel.customVideoUris.isNotEmpty()) customVideoIndex % viewModel.customVideoUris.size else 0
                            val currentUri = viewModel.customVideoUris[safeVideoIndex]
                            
                            androidx.compose.runtime.key(currentUri) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { ctx ->
                                        android.view.TextureView(ctx).apply {
                                            val textureView = this
                                            var currentVideoWidth = 0
                                            var currentVideoHeight = 0
                                            
                                            fun updateTransform(viewWidth: Int, viewHeight: Int) {
                                                if (currentVideoWidth > 0 && currentVideoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                                                    val scaleX = viewWidth.toFloat() / currentVideoWidth.toFloat()
                                                    val scaleY = viewHeight.toFloat() / currentVideoHeight.toFloat()
                                                    val maxScale = maxOf(scaleX, scaleY)
                                                    val scaledWidth = maxScale * currentVideoWidth
                                                    val scaledHeight = maxScale * currentVideoHeight
                                                    
                                                    val matrix = android.graphics.Matrix()
                                                    val pivotX = viewWidth / 2f
                                                    val pivotY = viewHeight / 2f
                                                    matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, pivotX, pivotY)
                                                    textureView.setTransform(matrix)
                                                }
                                            }

                                            surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                                var mediaPlayer: android.media.MediaPlayer? = null
                                                override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                    try {
                                                        mediaPlayer = android.media.MediaPlayer().apply {
                                                            setOnVideoSizeChangedListener { _, vW, vH ->
                                                                currentVideoWidth = vW
                                                                currentVideoHeight = vH
                                                                updateTransform(textureView.width, textureView.height)
                                                            }
                                                            setDataSource(ctx, android.net.Uri.parse(currentUri))
                                                            setSurface(android.view.Surface(surface))
                                                            isLooping = true
                                                            setVolume(0f, 0f)
                                                            prepare()
                                                            start()
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                    updateTransform(width, height)
                                                }
                                                override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                                                    mediaPlayer?.release()
                                                    mediaPlayer = null
                                                    return true
                                                }
                                                override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            // Secondary Video (bg2.mp4)
                            androidx.compose.ui.viewinterop.AndroidView(
                                modifier = Modifier.fillMaxSize().alpha(secondaryVideoAlpha),
                                factory = { ctx ->
                                    android.view.TextureView(ctx).apply {
                                        val textureView = this
                                        var currentVideoWidth = 0
                                        var currentVideoHeight = 0
                                        
                                        fun updateTransform(viewWidth: Int, viewHeight: Int) {
                                            if (currentVideoWidth > 0 && currentVideoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                                                val scaleX = viewWidth.toFloat() / currentVideoWidth.toFloat()
                                                val scaleY = viewHeight.toFloat() / currentVideoHeight.toFloat()
                                                val maxScale = maxOf(scaleX, scaleY)
                                                val scaledWidth = maxScale * currentVideoWidth
                                                val scaledHeight = maxScale * currentVideoHeight
                                                
                                                val matrix = android.graphics.Matrix()
                                                val pivotX = viewWidth / 2f
                                                val pivotY = viewHeight / 2f
                                                matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, pivotX, pivotY)
                                                textureView.setTransform(matrix)
                                            }
                                        }

                                        surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                            var mediaPlayer: android.media.MediaPlayer? = null
                                            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                try {
                                                    mediaPlayer = android.media.MediaPlayer().apply {
                                                        setOnVideoSizeChangedListener { _, vW, vH ->
                                                            currentVideoWidth = vW
                                                            currentVideoHeight = vH
                                                            updateTransform(textureView.width, textureView.height)
                                                        }
                                                        val afd = ctx.resources.openRawResourceFd(com.raj.kotlinmusic.R.raw.bg2)
                                                        if (afd != null) {
                                                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                                            afd.close()
                                                        }
                                                        setSurface(android.view.Surface(surface))
                                                        isLooping = true
                                                        setVolume(0f, 0f)
                                                        prepare()
                                                        start()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                updateTransform(width, height)
                                            }
                                            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                return true
                                            }
                                            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                                        }
                                    }
                                }
                            )
    
                            // Primary Video (bg.mp4)
                            androidx.compose.ui.viewinterop.AndroidView(
                                modifier = Modifier.fillMaxSize().alpha(primaryVideoAlpha),
                                factory = { ctx ->
                                    android.view.TextureView(ctx).apply {
                                        val textureView = this
                                        var currentVideoWidth = 0
                                        var currentVideoHeight = 0
                                        
                                        fun updateTransform(viewWidth: Int, viewHeight: Int) {
                                            if (currentVideoWidth > 0 && currentVideoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                                                val scaleX = viewWidth.toFloat() / currentVideoWidth.toFloat()
                                                val scaleY = viewHeight.toFloat() / currentVideoHeight.toFloat()
                                                val maxScale = maxOf(scaleX, scaleY)
                                                val scaledWidth = maxScale * currentVideoWidth
                                                val scaledHeight = maxScale * currentVideoHeight
                                                
                                                val matrix = android.graphics.Matrix()
                                                val pivotX = viewWidth / 2f
                                                val pivotY = viewHeight / 2f
                                                matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, pivotX, pivotY)
                                                textureView.setTransform(matrix)
                                            }
                                        }

                                        surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                            var mediaPlayer: android.media.MediaPlayer? = null
                                            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                try {
                                                    mediaPlayer = android.media.MediaPlayer().apply {
                                                        setOnVideoSizeChangedListener { _, vW, vH ->
                                                            currentVideoWidth = vW
                                                            currentVideoHeight = vH
                                                            updateTransform(textureView.width, textureView.height)
                                                        }
                                                        val afd = ctx.resources.openRawResourceFd(com.raj.kotlinmusic.R.raw.bg)
                                                        if (afd != null) {
                                                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                                            afd.close()
                                                        }
                                                        setSurface(android.view.Surface(surface))
                                                        isLooping = true
                                                        setVolume(0f, 0f)
                                                        prepare()
                                                        start()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                                                updateTransform(width, height)
                                            }
                                            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                return true
                                            }
                                            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
                                        }
                                    }
                                }
                            )
                        }



                        Text(
                            text = "Lyrics not found for this track",
                            color = theme.text.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alpha(textAlpha)
                        )
                        
                        // Import Controls (Previews + Button)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showImportPill,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(6.dp),
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (viewModel.customVideoUris.isNotEmpty()) {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(viewModel.customVideoUris.size) { idx ->
                                            val uri = viewModel.customVideoUris[idx]
                                            val isSelected = (customVideoIndex % viewModel.customVideoUris.size) == idx
                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .height(84.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = if (isSelected) theme.accent1 else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { customVideoIndex = idx }
                                            ) {
                                                VideoThumbnail(uri = uri, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { 
                                        showImportPill = false
                                        videoPickerLauncher.launch(arrayOf("video/mp4"))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Import your video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                val isNoLyricsVideoState = !viewModel.isLyricsLoading && viewModel.syncedLyricsList.isNullOrEmpty() && (viewModel.lyricsText.isNullOrEmpty() || viewModel.lyricsText?.startsWith("No lyrics") == true || viewModel.lyricsText?.startsWith("No matching lyrics") == true)
                
                if (!isNoLyricsVideoState && !forceVideoMode) {
                    // Bottom Gradient Fade overlay matching theme background color
                    val fadeColor = if (theme.id == "liquid_glass") Color(0xFF0C0E17) else theme.background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, fadeColor)
                                )
                            )
                    )
                }
            }
        }

        // Floating Anchor Box for the DropdownMenu
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(lastTouchPosition.x.toInt(), lastTouchPosition.y.toInt() - 100) }
        ) {
            MaterialTheme(
                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp)),
                colorScheme = MaterialTheme.colorScheme.copy(
                    surface = if (theme.id == "liquid_glass") resolvedAccentColor.copy(alpha = 0.95f) else resolvedAccentColor,
                    onSurface = Color.White
                )
            ) {
                DropdownMenu(
                    expanded = showLyricsMenu,
                    onDismissRequest = { showLyricsMenu = false },
                    modifier = Modifier.width(180.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Share lyrics", color = Color.White, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showLyricsMenu = false
                            selectionMode = true
                            selectedLines = emptySet()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Video mode", color = Color.White, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showLyricsMenu = false
                            forceVideoMode = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete saved lyrics", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold) },
                        onClick = {
                            showLyricsMenu = false
                            viewModel.deleteSavedLyrics()
                        }
                    )
                }
            }
        }

        // Selection Mode Bottom Bar
        AnimatedVisibility(
            visible = selectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(theme.accent1)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${selectedLines.size}/4 Selected",
                    color = theme.background,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(theme.background.copy(alpha = 0.2f))
                        .clickable(enabled = selectedLines.isNotEmpty()) {
                            val selectedLyricsText = selectedLines
                                .sorted()
                                .mapNotNull { viewModel.syncedLyricsList.getOrNull(it)?.text }
                                .toList()
                            selectedLyricsTextForPreview = selectedLyricsText
                            showSharePreview = true
                            selectionMode = false
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Share",
                        color = if (selectedLines.isNotEmpty()) theme.background else theme.background.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        selectionMode = false
                        selectedLines = emptySet()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = theme.background
                    )
                }
            }
        }
        
        if (showSharePreview && selectedLyricsTextForPreview.isNotEmpty()) {
            SharePreviewOverlay(
                viewModel = viewModel,
                lines = selectedLyricsTextForPreview,
                onDismiss = { showSharePreview = false }
            )
        }
    }
}

@Composable
fun SharePreviewOverlay(
    viewModel: com.raj.kotlinmusic.MusicViewModel,
    lines: List<String>,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var previewBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var rawBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    
    var aspectRatio by remember { mutableStateOf(0) } // 0 = 1:1, 1 = 9:16, 2 = 16:9
    var fontId by remember { mutableStateOf(0) } // 0 = Standard, 1 = Writer, 2 = Aesthetic
    var fontSize by remember { mutableStateOf(72f) }
    
    val colorPresets = listOf(
        Pair(Color(0xFF0C0E17), Color(0xFF0C0E17)), // Midnight
        Pair(Color(0xFF1E1E1E), Color(0xFF1E1E1E)), // Dark Gray
        Pair(Color(0xFF2563EB), Color(0xFF2563EB)), // Solid Blue
        Pair(Color(0xFFDC2626), Color(0xFFDC2626)), // Solid Red
        Pair(Color(0xFF059669), Color(0xFF059669)), // Solid Green
        Pair(Color(0xFF8B5CF6), Color(0xFFEC4899)), // Purple-Pink Gradient
        Pair(Color(0xFFF59E0B), Color(0xFFEF4444)), // Orange-Red Gradient
        Pair(Color(0xFF3B82F6), Color(0xFF10B981))  // Blue-Green Gradient
    )
    var selectedColorIndex by remember { mutableStateOf(0) }
    
    androidx.compose.runtime.LaunchedEffect(aspectRatio, fontId, selectedColorIndex, lines, fontSize) {
        isGenerating = true
        val preset = colorPresets[selectedColorIndex]
        val bmp = viewModel.generateShareBitmap(
            context, lines, preset.first, preset.second, aspectRatio, fontId, fontSize
        )
        if (bmp != null) {
            rawBitmap = bmp
            previewBitmap = bmp.asImageBitmap()
        }
        isGenerating = false
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .pointerInput(Unit) {} // Consume clicks
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(top = 16.dp)
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
                Text("Customize Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.align(Alignment.Center))
            }
            
            // Preview Image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = "Share Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                if (isGenerating) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                }
            }
            
            // Controls Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Aspect Ratio
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)),
                ) {
                    listOf("1:1", "9:16", "16:9").forEachIndexed { index, label ->
                        val selected = aspectRatio == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { aspectRatio = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                // Fonts
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)),
                ) {
                    listOf("Standard", "Writer", "Aesthetic").forEachIndexed { index, label ->
                        val selected = fontId == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Color.White else Color.Transparent)
                                .clickable { fontId = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val fontFamily = when(index) {
                                1 -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.raj.kotlinmusic.R.font.writer))
                                2 -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.raj.kotlinmusic.R.font.korvich_slam))
                                else -> androidx.compose.ui.text.font.FontFamily.SansSerif
                            }
                            Text(label, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontFamily = fontFamily)
                        }
                    }
                }
                
                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("A", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 32f..140f,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Text("A", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                // Colors
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorPresets.size) { index ->
                        val preset = colorPresets[index]
                        val isSelected = selectedColorIndex == index
                        val brush = if (preset.first == preset.second) {
                            androidx.compose.ui.graphics.SolidColor(preset.first)
                        } else {
                            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(preset.first, preset.second))
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(brush)
                                .border(if (isSelected) 3.dp else 0.dp, Color.White, androidx.compose.foundation.shape.CircleShape)
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
                
                // Share Button
                androidx.compose.material3.Button(
                    onClick = {
                        rawBitmap?.let { viewModel.exportSharedBitmap(context, it) }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Share to Apps", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun VideoThumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by androidx.compose.runtime.remember(uri) { androidx.compose.runtime.mutableStateOf<android.graphics.Bitmap?>(null) }

    androidx.compose.runtime.LaunchedEffect(uri) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(context, android.net.Uri.parse(uri))
                // Extract frame at 1,000,000 microseconds (1 second)
                val frame = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    bitmap = frame
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Video Thumbnail",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color.DarkGray))
    }
}
