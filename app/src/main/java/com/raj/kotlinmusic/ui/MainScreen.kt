package com.raj.kotlinmusic.ui

import com.raj.kotlinmusic.MusicPlayerController
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raj.kotlinmusic.MusicViewModel
import com.raj.kotlinmusic.CardMode
import com.raj.kotlinmusic.PlayerState
import com.raj.kotlinmusic.AppScreen
import com.raj.kotlinmusic.Song
import com.raj.kotlinmusic.ui.components.ControlCapsule
import com.raj.kotlinmusic.ui.theme.ColorPalette
import com.raj.kotlinmusic.ui.theme.ThemePalettes
import androidx.compose.ui.util.lerp as floatLerp
import kotlin.math.absoluteValue

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MusicViewModel = viewModel()) {
    val theme = viewModel.currentPalette
    val currentSong = viewModel.currentSong
    val playlist = if (viewModel.currentPlaybackQueue.isNotEmpty()) viewModel.currentPlaybackQueue else viewModel.playlist
    val isPlaying = viewModel.isPlaying

    var showSettingsDialog by remember { mutableStateOf(false) }

    // System Back Gesture/Key navigation handler to exit Cards Screen back to Home Screen
    BackHandler(enabled = viewModel.playerState == PlayerState.CARDS && viewModel.currentAppScreen == AppScreen.PLAYER) {
        if (viewModel.isCapsuleExpanded) {
            viewModel.isCapsuleExpanded = false
        } else {
            viewModel.currentAppScreen = AppScreen.HOME
        }
    }

    // System Back Gesture/Key navigation handler to exit Immersive Screen back to Cards Screen
    BackHandler(enabled = viewModel.playerState == PlayerState.IMMERSIVE && viewModel.currentAppScreen == AppScreen.PLAYER) {
        viewModel.playerState = PlayerState.CARDS
    }

    var isProgrammaticScrollActive by remember { androidx.compose.runtime.mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val currentSongIndex = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)

    // Cards wheel always starts centered on the current song
    val cardsInitialPage = remember { 5000 * playlist.size + currentSongIndex }
    val cardsLazyListState = remember { androidx.compose.foundation.lazy.LazyListState(firstVisibleItemIndex = cardsInitialPage) }
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Shake Detector for Cards Player Screen
    androidx.compose.runtime.DisposableEffect(viewModel.currentAppScreen, viewModel.playerState, viewModel.shakeToShuffleEnabled) {
        if (viewModel.shakeToShuffleEnabled && viewModel.currentAppScreen == AppScreen.PLAYER && viewModel.playerState == PlayerState.CARDS) {
            val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
            val accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            
            if (accelerometer != null) {
                val startTime = System.currentTimeMillis()
                var lastShakeTime = 0L
                val listener = object : android.hardware.SensorEventListener {
                    override fun onSensorChanged(event: android.hardware.SensorEvent) {
                        val now = System.currentTimeMillis()
                        // Ignore initial noisy/junk sensor readings in the first 500ms
                        if (now - startTime < 500L) return
                        
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        
                        val gX = x / android.hardware.SensorManager.GRAVITY_EARTH
                        val gY = y / android.hardware.SensorManager.GRAVITY_EARTH
                        val gZ = z / android.hardware.SensorManager.GRAVITY_EARTH
                        
                        val gForce = kotlin.math.sqrt(gX * gX + gY * gY + gZ * gZ)
                        
                        // Shake threshold: 3.2f is robust against false positives from normal UI tapping
                        if (gForce > 3.2f) {
                            // Debounce shakes to 2.0 seconds to prevent rapid double-skips
                            if (now - lastShakeTime > 2000L) {
                                lastShakeTime = now
                                val currentPlaylist = viewModel.playlist
                                if (currentPlaylist.isNotEmpty()) {
                                    val otherSongs = currentPlaylist.filter { it.id != viewModel.currentSong.id }
                                    val targetSong = if (otherSongs.isNotEmpty()) otherSongs.random() else currentPlaylist.firstOrNull()
                                    if (targetSong != null) {
                                        // Use selectSong instead of playSong to keep UI and play state synchronized!
                                        viewModel.selectSong(targetSong)
                                    }
                                }
                            }
                        }
                    }
                    
                    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
                }
                
                sensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_UI)
                
                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            } else {
                onDispose {}
            }
        } else {
            onDispose {}
        }
    }

    // Determine runtime permissions based on SDK version (including notification permission on API 33+)
    val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val permissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val audioPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isAudioGranted = (permissionsMap[audioPermission] == true) || 
            (androidx.core.content.ContextCompat.checkSelfPermission(context, audioPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        if (isAudioGranted) {
            viewModel.loadSongsFromStorage(context)
        }

        // Post-permission analysis for notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Check if notification permission is in request map and has been explicitly denied
            if (permissionsMap.containsKey(android.Manifest.permission.POST_NOTIFICATIONS) && 
                permissionsMap[android.Manifest.permission.POST_NOTIFICATIONS] == false
            ) {
                val activity = context as? android.app.Activity
                val shouldShowRationale = activity?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.POST_NOTIFICATIONS)
                } ?: false

                // If rationale is false, it means they clicked 'Don't allow' twice (permanently denied) or notifications are disabled in settings
                if (!shouldShowRationale) {
                    showSettingsDialog = true
                }
            }
        }
    }

    // Sync the overlay dismiss state back to ViewModel
    LaunchedEffect(Unit) {
        MusicPlayerController.onOverlayDismissed = {
            viewModel.isOverlayDismissedByUser = true
        }
    }

    // Floating overlay service lifecycle observer removed

    // Auto-request permissions and scan on first launch
    LaunchedEffect(Unit) {
        val ungrantedPermissions = permissionsToRequest.filter { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isEmpty()) {
            viewModel.loadSongsFromStorage(context)
        } else {
            permissionsLauncher.launch(ungrantedPermissions.toTypedArray())
        }
    }

    // Start or update Foreground Notification Service whenever playback is active, requesting notification permission if needed
    LaunchedEffect(currentSong, isPlaying) {
        if (isPlaying) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!hasNotificationPermission) {
                    permissionsLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
                }
            } else {
                // On older APIs, check if they disabled notifications in Settings and show rationale
                val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val areNotificationsEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    notificationManager.areNotificationsEnabled()
                } else {
                    true
                }
                if (!areNotificationsEnabled) {
                    showSettingsDialog = true
                }
            }

            // Start floating service if enabled
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (android.provider.Settings.canDrawOverlays(context)) {
                    viewModel.startFloatingService(context)
                }
            } else {
                viewModel.startFloatingService(context)
            }
        }
        viewModel.startForegroundService(context)
    }



    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        if (theme.id == "liquid_glass") {
            val liquidGlassColors = viewModel.getLiquidGlassColors(theme)
            // Vibrant sweeping background for true glassmorphic refraction
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Large sweeping diagonal gradient
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            liquidGlassColors[0].copy(alpha = 0.6f),
                            liquidGlassColors[1].copy(alpha = 0.5f),
                            liquidGlassColors[2].copy(alpha = 0.6f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    ),
                    size = size
                )
                // Sunset Red radial pop in the bottom left
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(liquidGlassColors[1].copy(alpha = 0.6f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f),
                        radius = size.width * 0.8f
                    ),
                    center = androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f),
                    radius = size.width * 0.8f
                )
                // Sunset Gold pop in top right
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(liquidGlassColors[2].copy(alpha = 0.5f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f),
                        radius = size.width * 0.7f
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f),
                    radius = size.width * 0.7f
                )
            }
        }
        val isSettledOnCards = true

        // Smoothly animate the control capsule expansion
        val capsuleExpansionProgress by animateFloatAsState(
            targetValue = if (viewModel.isCapsuleExpanded) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "CapsuleExpansion"
        )


        // Track current song changes (shuffle, next/prev, or home selection)
        LaunchedEffect(currentSong) {
            if (isSettledOnCards && !isProgrammaticScrollActive && playlist.isNotEmpty()) {
                val targetIndex = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                val currentLoopedIndex = cardsLazyListState.firstVisibleItemIndex % playlist.size
                if (currentLoopedIndex != targetIndex) {
                    try {
                        isProgrammaticScrollActive = true
                        val targetPage = cardsLazyListState.firstVisibleItemIndex + (targetIndex - currentLoopedIndex)
                        if (viewModel.currentAppScreen == AppScreen.PLAYER) {
                            cardsLazyListState.animateScrollToItem(targetPage)
                        } else {
                            cardsLazyListState.scrollToItem(targetPage)
                        }
                    } finally {
                        isProgrammaticScrollActive = false
                    }
                }
            }
        }

        // Also ensure the wheel is instantly snapped to the current song when entering the player screen
        LaunchedEffect(viewModel.currentAppScreen) {
            if (viewModel.currentAppScreen == AppScreen.PLAYER && playlist.isNotEmpty()) {
                val targetIndex = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
                val currentLoopedIndex = cardsLazyListState.firstVisibleItemIndex % playlist.size
                if (currentLoopedIndex != targetIndex) {
                    try {
                        isProgrammaticScrollActive = true
                        val targetPage = cardsLazyListState.firstVisibleItemIndex + (targetIndex - currentLoopedIndex)
                        cardsLazyListState.scrollToItem(targetPage)
                    } finally {
                        isProgrammaticScrollActive = false
                    }
                }
            }
        }

        // Snap to nearest card after user manually scrolls
        LaunchedEffect(cardsLazyListState.isScrollInProgress, isSettledOnCards) {
            if (!cardsLazyListState.isScrollInProgress && isSettledOnCards && !isProgrammaticScrollActive) {
                val firstVisibleIndex = cardsLazyListState.firstVisibleItemIndex
                val firstVisibleOffset = cardsLazyListState.firstVisibleItemScrollOffset.toFloat()
                val itemSizeDp = if (viewModel.cardMode == CardMode.WHEEL) (300.dp + 16.dp) else 96.dp // 300dp width + 16dp spacing
                val itemSizePx = with(density) { itemSizeDp.toPx() }
                val cardsScrollPosition = firstVisibleIndex.toFloat() + firstVisibleOffset / itemSizePx
                val nearestIndex = (cardsScrollPosition + 0.5f).toInt()

                try {
                    isProgrammaticScrollActive = true
                    if (firstVisibleOffset > 1f || firstVisibleIndex != nearestIndex) {
                        cardsLazyListState.animateScrollToItem(nearestIndex)
                    }
                } finally {
                    isProgrammaticScrollActive = false
                }

                if (playlist.isNotEmpty()) {
                    val activeIndex = nearestIndex % playlist.size
                    val selectedSong = playlist[activeIndex]
                    if (viewModel.currentSong != selectedSong) {
                        viewModel.currentSong = selectedSong
                        if (viewModel.isPlaying) {
                            viewModel.playSong(selectedSong)
                        }
                    }
                }
            }
        }


        // Collapse expanded control capsule when user scrolls jog wheel
        LaunchedEffect(cardsLazyListState.isScrollInProgress) {
            if (cardsLazyListState.isScrollInProgress) {
                viewModel.collapseCapsule()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.collapseCapsule()
                }
        ) {
            // --- Plain Theme Background ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(theme.background)
            )

            // --- Cards View ---
            if (viewModel.playerState == PlayerState.CARDS) {
                // Outer Black Screen Border
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black) // Hardware bezel is strictly black
                        .padding(6.dp) // Thin uniform border
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top Container (Gradient section)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Takes up top area
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
                                .background(theme.background)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (theme.id == "liquid_glass") {
                                    val liquidGlassColors = viewModel.getLiquidGlassColors(theme)
                                    drawRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                liquidGlassColors[0].copy(alpha = 0.6f),
                                                liquidGlassColors[1].copy(alpha = 0.5f),
                                                liquidGlassColors[2].copy(alpha = 0.6f)
                                            ),
                                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                                        ),
                                        size = size
                                    )
                                    // Theme-specific glass colors
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(liquidGlassColors[2].copy(alpha = 0.8f), Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            radius = size.width * 1.2f
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        radius = size.width * 1.2f
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(liquidGlassColors[1].copy(alpha = 0.7f), Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                            radius = size.width * 1.2f
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        radius = size.width * 1.2f
                                    )
                                } else {
                                    // 1. Top-Left Pink Gradient
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(theme.accent1.copy(alpha = 0.8f), Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                            radius = size.width * 1.2f
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        radius = size.width * 1.2f
                                    )
                                    // 2. Top-Right Yellow Gradient
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(theme.accent2.copy(alpha = 0.7f), Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                            radius = size.width * 1.2f
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                        radius = size.width * 1.2f
                                    )
                                }
                            }
                            
                            
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // 1. Card Container (List/Wheel) - Placed first to draw underneath
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val boxHeight = maxHeight
                                    val boxWidth = maxWidth
                                    
                                    if (viewModel.cardMode == CardMode.WHEEL) {
                                        val itemWidthDp = 300.dp

                                        LazyRow(
                                            state = cardsLazyListState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(start = 24.dp, end = 24.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(
                                                count = if (playlist.isEmpty()) 0 else 10000 * playlist.size,
                                                key = { it }
                                            ) { page ->
                                                val song = playlist[page % playlist.size]
                                                Box(
                                                    modifier = Modifier
                                                        .width(itemWidthDp)
                                                        .fillMaxHeight()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(itemWidthDp)
                                                            .align(Alignment.CenterStart)
                                                            .shadow(
                                                                elevation = 12.dp,
                                                                shape = RoundedCornerShape(2.dp),
                                                                spotColor = Color.Black.copy(alpha = 0.2f)
                                                            )
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(viewModel.getSongAccentColor(song))
                                                            .clickable {
                                                                if (song.id == currentSong.id && viewModel.playerState == PlayerState.CARDS) {
                                                                    viewModel.playerState = PlayerState.IMMERSIVE
                                                                } else {
                                                                    viewModel.selectSong(song)
                                                                    coroutineScope.launch {
                                                                        cardsLazyListState.animateScrollToItem(page)
                                                                    }
                                                                }
                                                            }
                                                    ) {
                                                        Text(
                                                            text = song.title.take(1).uppercase(),
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 140.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.align(Alignment.Center)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val itemHeightDp = 96.dp
                                        val itemHeightPx = with(density) { itemHeightDp.toPx() }
                                        
                                        val topFade = Brush.verticalGradient(
                                            0.18f to Color.Transparent, 
                                            0.35f to Color.Black
                                        )
                                        val bottomFade = Brush.verticalGradient(
                                            0.65f to Color.Black, 
                                            0.85f to Color.Transparent
                                        )

                                        LazyColumn(
                                            state = cardsLazyListState,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                .drawWithContent {
                                                    drawContent()
                                                    drawRect(brush = topFade, blendMode = BlendMode.DstIn)
                                                    drawRect(brush = bottomFade, blendMode = BlendMode.DstIn)
                                                },
                                        contentPadding = PaddingValues(
                                            top = (boxHeight - itemHeightDp) / 2,
                                            bottom = (boxHeight - itemHeightDp) / 2
                                        )
                                    ) {
                                        items(
                                            count = if (playlist.isEmpty()) 0 else 10000 * playlist.size,
                                            key = { it }
                                        ) { page ->
                                            val song = playlist[page % playlist.size]
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(itemHeightDp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(260.dp)
                                                        .height(itemHeightDp)
                                                        .align(Alignment.CenterStart)
                                                        .graphicsLayer {
                                                            val scrollPosPx = cardsLazyListState.firstVisibleItemScrollOffset.toFloat()
                                                            val scrollPos = cardsLazyListState.firstVisibleItemIndex.toFloat() + scrollPosPx / itemHeightPx
                                                            val pageOffset = page.toFloat() - scrollPos

                                                            this.translationX = 24.dp.toPx()
                                                            this.translationY = 0f
                                                            this.rotationZ = 0f
                                                            
                                                            val scale = 1f - 0.2f * Math.abs(pageOffset).coerceIn(0f, 1f)
                                                            this.scaleX = scale
                                                            this.scaleY = scale
                                                            this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                                                            this.alpha = 1f - 0.4f * Math.abs(pageOffset).coerceIn(0f, 1f)
                                                        }
                                                        .shadow(
                                                            elevation = 8.dp,
                                                            shape = RoundedCornerShape(20.dp),
                                                            spotColor = Color.Black.copy(alpha = 0.15f)
                                                        )
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(if (theme.id == "white_candy") theme.accent3 else theme.surface)
                                                        .clickable {
                                                            if (song.id == currentSong.id && viewModel.playerState == PlayerState.CARDS) {
                                                                viewModel.playerState = PlayerState.IMMERSIVE
                                                            } else {
                                                                viewModel.selectSong(song)
                                                                coroutineScope.launch {
                                                                    cardsLazyListState.animateScrollToItem(page)
                                                                }
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(start = 20.dp, end = 12.dp)
                                                        ) {
                                                            Text(
                                                                text = song.artist,
                                                                color = theme.text,
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = song.title,
                                                                color = theme.mutedText,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(end = 12.dp)
                                                                .size(64.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(viewModel.getSongAccentColor(song)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = song.title.take(1).uppercase(),
                                                                color = Color.White.copy(alpha = 0.5f),
                                                                fontSize = 32.sp,
                                                                fontWeight = FontWeight.Black
                                                            )
                                                        }
                                                    }
                                                    }
                                                }
                                            }
                                        }
                                    } // End else
                                } // End BoxWithConstraints

                                // 2. Giant title and artist header at the top
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopStart)
                                        .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = currentSong.title.replaceFirst(" ", "\n"), // Split first space to match "Exploring Minds" multiline style
                                        color = theme.text,
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 38.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (viewModel.cardMode == CardMode.LIST) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentSong.artist,
                                            color = theme.text.copy(alpha = 0.85f),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (viewModel.cardMode == CardMode.WHEEL) {
                                    Text(
                                        text = currentSong.artist,
                                        color = theme.text.copy(alpha = 0.85f),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(start = 32.dp, bottom = 80.dp)
                                    )
                                }
                                val liquidGlassColors = if (theme.id == "liquid_glass") viewModel.getLiquidGlassColors(theme) else emptyList()
                                val activeAccent = if (theme.id == "liquid_glass") liquidGlassColors[2] else theme.accent1
                                val lyricsBg = if (theme.id == "liquid_glass") liquidGlassColors[2] else theme.accent1

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(start = 32.dp, end = 32.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(theme.surface)
                                            .let {
                                                if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)) else it
                                            }
                                            .clickable { viewModel.repeatEnabled = !viewModel.repeatEnabled },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Repeat", color = theme.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                                                contentDescription = "Repeat",
                                                tint = if (viewModel.repeatEnabled) activeAccent else theme.text,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(lyricsBg)
                                            .let {
                                                if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)) else it
                                            }
                                            .clickable { viewModel.currentAppScreen = AppScreen.LYRICS },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Lyrics", color = if (theme.id == "white_candy") theme.text else theme.background, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    val mainBeltHeight by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (viewModel.currentAppScreen == AppScreen.PLAYER) 10.dp else 24.dp,
                        animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        label = "MainBeltAnim"
                    )
                    Spacer(modifier = Modifier.height(mainBeltHeight)) // The black belt

                    // 4. Bottom White Capsule
                    com.raj.kotlinmusic.ui.components.SimpleBottomCapsule(
                        viewModel = viewModel
                    )
                }
            }
        }

            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = theme.surface,
                    title = {
                        Text(
                            text = "Enable Notifications",
                            color = theme.text,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Notifications are disabled in settings. To see the media playback widget and control your music from the status bar, please enable notifications.",
                            color = theme.mutedText,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSettingsDialog = false
                                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = viewModel.getSongAccentColor(currentSong),
                                contentColor = theme.background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go to Settings", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showSettingsDialog = false }
                        ) {
                            Text("Cancel", color = theme.mutedText, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            // --- Immersive Mode View Overlay ---
            if (viewModel.playerState == PlayerState.IMMERSIVE) {
                ImmersivePlayerView(
                    viewModel = viewModel,
                    playlist = playlist,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onBack = { viewModel.playerState = PlayerState.CARDS }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ImmersivePlayerView(
    viewModel: MusicViewModel,
    playlist: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    onBack: () -> Unit
) {
    val theme = viewModel.currentPalette
    val currentSongIndex = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = currentSongIndex
    )
    
    // Register native system BackHandler to exit back to Cards list
    BackHandler(onBack = onBack)
    
    // Smoothly scroll the carousel when the track changes system-wide
    LaunchedEffect(currentSong) {
        val targetIdx = playlist.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
        if (pagerState.currentPage != targetIdx) {
            pagerState.animateScrollToPage(targetIdx)
        }
    }
    
    // When the user scrolls the carousel manually, update the active song in the view model once the scroll settles!
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val activeIndex = pagerState.currentPage
            if (activeIndex in playlist.indices && playlist[activeIndex].id != currentSong.id) {
                viewModel.selectSong(playlist[activeIndex])
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background) // Sleek theme-adaptive background
            .statusBarsPadding()
    ) {
        if (theme.id == "liquid_glass") {
            // Ambient glowing blobs for glassmorphic refraction in Immersive View
            val liquidGlassColors = viewModel.getLiquidGlassColors(theme)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            liquidGlassColors[0].copy(alpha = 0.6f),
                            liquidGlassColors[1].copy(alpha = 0.5f),
                            liquidGlassColors[2].copy(alpha = 0.6f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    ),
                    size = size
                )
                drawCircle(
                    color = liquidGlassColors[2].copy(alpha = 0.12f),
                    radius = 320.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height * 0.2f)
                )
                drawCircle(
                    color = liquidGlassColors[1].copy(alpha = 0.12f),
                    radius = 300.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.6f)
                )
            }
        }
        // --- 1. Top Header Section ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            // Giant pixelated header: TRACKS
            Text(
                text = "TRACKS",
                color = theme.text,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // Description subtitle
            Text(
                text = "作った曲たち", // Matching your Japanese OP-1 mockup exactly!
                color = theme.mutedText,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        // --- 2. Horizontal Snapping Carousel ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 60.dp, bottom = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                pageCount = playlist.size,
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 48.dp),
                pageSpacing = 24.dp,
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                val song = playlist[page]
                val isCurrent = song.id == currentSong.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Calculate distance from current page to apply scale & alpha dynamics
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val fraction = pageOffset.absoluteValue.coerceIn(0f, 1f)
                            val scale = 1f - (0.15f * fraction)
                            val alpha = 1f - (0.5f * fraction)
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AudioCassetteTape(
                        song = song,
                        isPlaying = isPlaying && isCurrent,
                        accentColor = viewModel.getSongAccentColor(song),
                        theme = theme
                    )
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Detailed Metadata Box underneath the cassette!
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .height(115.dp)
                            .border(1.dp, theme.surface, RoundedCornerShape(12.dp))
                            .background(theme.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        // Title row
                        Text(
                            text = "TITLE:  ${song.title.uppercase()}",
                            color = theme.text,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Artist row
                        Text(
                            text = "ARTIST: ${song.artist.uppercase()}",
                            color = viewModel.getSongAccentColor(song),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Tech Specs Row (Format, Lyrics)
                        val codec = song.path?.substringAfterLast('.')?.uppercase() ?: "PCM"
                        val lyrics = if (song.lyricsAvailable) "ACTIVE" else "NONE"
                        Text(
                            text = "CODEC:  $codec | LYRICS: $lyrics",
                            color = theme.mutedText.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // File Path / Storage Location (Beautifully clipped!)
                        val location = song.path ?: "local://assets/audio_track_${song.id}"
                        Text(
                            text = "URI:    $location",
                            color = theme.mutedText,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- 3. Bottom Controls Deck Panel ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.surface)
                    .let { 
                        if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)) else it 
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mini preview cassette image/icon
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(30.dp)
                        .border(1.dp, theme.text.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                        .background(viewModel.getSongAccentColor(currentSong).copy(alpha = 0.8f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .background(theme.text, RoundedCornerShape(1.dp)),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(theme.background, CircleShape))
                        Box(modifier = Modifier.size(4.dp).background(theme.background, CircleShape))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Prev/Rewind Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.background)
                        .clickable { viewModel.previousSong() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = theme.text,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play/Pause button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(viewModel.getSongAccentColor(currentSong))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (viewModel.getSongAccentColor(currentSong).luminance() > 0.5f) Color.Black else theme.background,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.background)
                        .clickable { viewModel.nextSong() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = theme.text,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioCassetteTape(
    song: Song,
    isPlaying: Boolean,
    accentColor: Color,
    theme: ColorPalette
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val isGlass = theme.id == "liquid_glass"

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isGlass) theme.accent3.copy(alpha = 0.15f) else accentColor)
            .border(
                width = if (isGlass) 1.dp else 1.5.dp,
                color = if (isGlass) Color.White.copy(alpha = 0.25f) else Color(0xFF1E272E),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.TopCenter)
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(width = 8.dp, height = 4.dp).background(if (isGlass) Color.White.copy(alpha = 0.3f) else Color(0xFF1E272E), RoundedCornerShape(2.dp)))
            Box(modifier = Modifier.size(width = 8.dp, height = 4.dp).background(if (isGlass) Color.White.copy(alpha = 0.3f) else Color(0xFF1E272E), RoundedCornerShape(2.dp)))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGlass) Color.White.copy(alpha = 0.15f) else Color.White)
                .border(
                    width = 1.dp,
                    color = if (isGlass) Color.White.copy(alpha = 0.2f) else Color(0xFFCECECE),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isGlass) theme.accent1.copy(alpha = 0.3f) else accentColor)
                    .let { if (isGlass) it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp)) else it }
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = song.title,
                color = if (isGlass) theme.text else Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 28.dp, top = 2.dp, end = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CassetteSpool(
                    rotationAngle = if (isPlaying) rotationAngle else 0f
                )

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isGlass) Color.Black.copy(alpha = 0.3f) else Color(0xFF1C2833))
                        .border(1.dp, if (isGlass) Color.White.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(3.dp)
                            .background(Color(0xFFD35400).copy(alpha = 0.7f))
                    )
                }

                CassetteSpool(
                    rotationAngle = if (isPlaying) rotationAngle else 0f
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(5.dp).background(if (isGlass) Color.White.copy(alpha = 0.3f) else Color(0xFF1E272E), CircleShape))
            Box(modifier = Modifier.size(5.dp).background(if (isGlass) Color.White.copy(alpha = 0.3f) else Color(0xFF1E272E), CircleShape))
            Box(modifier = Modifier.size(5.dp).background(if (isGlass) Color.White.copy(alpha = 0.3f) else Color(0xFF1E272E), CircleShape))
        }
    }
}

@Composable
fun CassetteSpool(
    rotationAngle: Float
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { rotationZ = rotationAngle }
            .clip(CircleShape)
            .background(Color(0xFF1E272E)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .border(2.dp, Color.White, CircleShape)
        )

        for (i in 0 until 6) {
            val angle = i * 60f
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(12.dp)
                    .graphicsLayer { rotationZ = angle }
                    .background(Color.White)
            )
        }
    }
}
