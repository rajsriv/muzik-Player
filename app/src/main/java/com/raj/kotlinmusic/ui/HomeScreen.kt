package com.raj.kotlinmusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.raj.kotlinmusic.AppScreen
import com.raj.kotlinmusic.CardMode
import com.raj.kotlinmusic.HomeViewState
import com.raj.kotlinmusic.MusicViewModel
import com.raj.kotlinmusic.Song
import com.raj.kotlinmusic.ui.components.SongListItem
import com.raj.kotlinmusic.ui.theme.ColorPalette
import com.raj.kotlinmusic.ui.theme.ThemePalettes
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.with


@OptIn(ExperimentalComposeUiApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(viewModel: MusicViewModel) {
    val theme = viewModel.currentPalette
    val bgColor = theme.background
    val textColor = theme.text
    val mutedTextColor = theme.mutedText
    val cardBg = theme.surface

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResults = if (searchQuery.isBlank()) emptyList()
    else viewModel.playlist.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.artist.contains(searchQuery, ignoreCase = true)
    }

    // Dismiss settings page on back
    BackHandler(enabled = viewModel.showSettingsPage) {
        viewModel.showSettingsPage = false
    }

    // Dismiss search on back
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
        focusManager.clearFocus()
    }

    var isExpandedByClick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(viewModel.isPlaying, isExpandedByClick, viewModel.currentAppScreen) {
        if (!viewModel.isPlaying && isExpandedByClick && viewModel.currentAppScreen == com.raj.kotlinmusic.AppScreen.HOME) {
            kotlinx.coroutines.delay(3000)
            isExpandedByClick = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(viewModel.currentAppScreen, viewModel.isPlaying, viewModel.isStopped) {
        if (viewModel.currentAppScreen == com.raj.kotlinmusic.AppScreen.PLAYER && !viewModel.isPlaying && !viewModel.isStopped) {
            isExpandedByClick = true
        }
    }

    val dashboardScrollState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Root Box for Overlays
    Box(modifier = Modifier.fillMaxSize()) {
        // The Bezel Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(6.dp)
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(
                    if (!viewModel.isStopped) RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    else RoundedCornerShape(28.dp)
                )
                .background(bgColor)
        ) {
        if (theme.id == "white_candy") {
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
        // ── Main scrollable content (blurred when search active) ──
        val blurRadius = if (isSearchActive) 16.dp else 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        ) {
        androidx.compose.animation.Crossfade(targetState = viewModel.homeViewState, label = "HomeStateRouting") { state ->
            when (state) {
                HomeViewState.DASHBOARD -> {
        LazyColumn(
            state = dashboardScrollState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ScrollAnimatedItem {
                    Column(
                        modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.45f)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Hello there!",
                        color = mutedTextColor,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start your\nmusic here!",
                        color = textColor,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 44.sp,
                        textAlign = TextAlign.Start,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                } // end hero Column
                }
            } // end hero item

            // Search Pill — shown right after the 85% hero block
            item {
                ScrollAnimatedItem(delayMillis = 100) {
                    Column {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(if (theme.id == "liquid_glass") Color.White.copy(alpha = 0.15f) else theme.surface)
                                .let { 
                                    if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)) else it 
                                }
                                .clickable { isSearchActive = true }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = mutedTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Search songs & artists…",
                                color = mutedTextColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Jog Wheel Player Button card
            item {
                val liquidGlassColors = if (theme.id == "liquid_glass") viewModel.getLiquidGlassColors(theme) else emptyList()
                ScrollAnimatedItem(delayMillis = 120) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (theme.id == "liquid_glass") liquidGlassColors[0].copy(alpha = 0.2f) else theme.accent3)
                            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)) else it }
                            .clickable {
                                viewModel.playerState = com.raj.kotlinmusic.PlayerState.CARDS
                                viewModel.currentAppScreen = com.raj.kotlinmusic.AppScreen.PLAYER
                            }
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("Jog Wheel", color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Icon(Icons.Filled.Album, contentDescription = null, tint = theme.text.copy(alpha = 0.9f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Open Player", color = theme.text.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        // Decorative icon
                        Icon(
                            Icons.Filled.Album,
                            contentDescription = null,
                            tint = theme.text.copy(alpha = 0.1f),
                            modifier = Modifier.size(110.dp).align(Alignment.BottomEnd).offset(x = 15.dp, y = 15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                ScrollAnimatedItem {
                    Text(
                    text = "For Good Mornings",
                    color = mutedTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            item {
                val suggestions = viewModel.morningSuggestions
                if (suggestions.size >= 5) {
                    val liquidGlassColors = if (theme.id == "liquid_glass") viewModel.getLiquidGlassColors(theme) else emptyList()
                    ScrollAnimatedItem(delayMillis = 50) {
                        Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Row 1: Card 1 & Card 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MorningSuggestionGridCard(
                                song = suggestions[0],
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
                                color = if (theme.id == "liquid_glass") liquidGlassColors[1] else theme.accent2,
                                icon = Icons.Filled.MusicNote,
                                patternType = 1,
                                onClick = { viewModel.selectSong(suggestions[0], suggestions) },
                                theme = theme,
                                modifier = Modifier.weight(1f)
                            )
                            
                            MorningSuggestionGridCard(
                                song = suggestions[1],
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 80.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
                                color = if (theme.id == "liquid_glass") liquidGlassColors[0] else theme.accent3,
                                icon = Icons.Filled.Album,
                                patternType = 2,
                                onClick = { viewModel.selectSong(suggestions[1], suggestions) },
                                theme = theme,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Row 2: Card 3 (Full Width)
                        MorningSuggestionHorizontalCard(
                            song = suggestions[2],
                            shape = RoundedCornerShape(24.dp),
                            color = if (theme.id == "liquid_glass") liquidGlassColors[2] else theme.accent1,
                            icon = Icons.Filled.PlayArrow,
                            onClick = { viewModel.selectSong(suggestions[2], suggestions) },
                            theme = theme
                        )
                        
                        // Row 3: Card 4 & Card 5
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MorningSuggestionGridCard(
                                song = suggestions[3],
                                shape = CircleShape,
                                color = if (theme.id == "liquid_glass") liquidGlassColors[1] else theme.accent2,
                                icon = Icons.Filled.Favorite,
                                patternType = 4,
                                onClick = { viewModel.selectSong(suggestions[3], suggestions) },
                                theme = theme,
                                modifier = Modifier.weight(1f)
                            )
                            
                            MorningSuggestionGridCard(
                                song = suggestions[4],
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 0.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
                                color = if (theme.id == "liquid_glass") liquidGlassColors[0] else theme.accent3,
                                icon = Icons.Filled.Person,
                                patternType = 5,
                                onClick = { viewModel.selectSong(suggestions[4], suggestions) },
                                theme = theme,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    }
                }
            }

            item {
                ScrollAnimatedItem {
                    Column {
                        Spacer(modifier = Modifier.height(32.dp))
                BentoBoxSection(viewModel)
                Spacer(modifier = Modifier.height(48.dp))

                Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
                }
                HomeViewState.CATEGORY_LIST -> {
                    CategoryListScreen(viewModel = viewModel)
                }
                HomeViewState.PLAYLISTS_OVERVIEW -> {
                    PlaylistsScreen(viewModel = viewModel)
                }
                HomeViewState.CREATE_PLAYLIST -> {
                    CreatePlaylistScreen(viewModel = viewModel)
                }
                HomeViewState.EDIT_PLAYLIST -> {
                    EditPlaylistScreen(viewModel = viewModel)
                }
            }
        }
        } // end blur Box
        } // close Box(weight(1f))
        
        // --- Dynamic Control Capsule Area ---
        if (!viewModel.isStopped) {
            val beltHeight by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (viewModel.currentAppScreen == com.raj.kotlinmusic.AppScreen.HOME) 24.dp else 10.dp,
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                label = "HomeBeltAnim"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(beltHeight),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = viewModel.currentAppScreen == com.raj.kotlinmusic.AppScreen.HOME,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400, delayMillis = 200)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                ) {
                    Text(
                        text = "${viewModel.currentSong.title}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            val isExpanded = viewModel.isPlaying || isExpandedByClick
            
            androidx.compose.animation.AnimatedContent(
                targetState = isExpanded,
                label = "CapsuleFade",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                    .clickable(
                        enabled = !isExpanded,
                        onClick = { isExpandedByClick = true }
                    ),
                contentAlignment = Alignment.BottomCenter,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) with
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) using
                    androidx.compose.animation.SizeTransform { _, _ ->
                        androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        )
                    }
                }
            ) { expanded ->
                if (expanded) {
                    com.raj.kotlinmusic.ui.components.SimpleBottomCapsule(
                        viewModel = viewModel,
                        modifier = Modifier
                    )
                } else {
                    // Collapsed state (White Capsule look)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let {
                                if (theme.id == "liquid_glass") {
                                    val liquidGlassColors = viewModel.getLiquidGlassColors(theme)
                                    it.background(liquidGlassColors[2].copy(alpha = 0.5f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
                                } else {
                                    it.background(theme.surface)
                                }
                            }
                            .navigationBarsPadding()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CONTROLS",
                            color = theme.mutedText,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    } // close Column


        var isBgSelectorExpanded by remember { mutableStateOf(false) }
        val previewSpacing by androidx.compose.animation.core.animateDpAsState(
            targetValue = if (isBgSelectorExpanded) 8.dp else (-14).dp,
            label = "previewSpacing"
        )

        val dashboardAlpha = if (viewModel.homeViewState == HomeViewState.DASHBOARD) {
            val offset = dashboardScrollState.firstVisibleItemScrollOffset
            val index = dashboardScrollState.firstVisibleItemIndex
            if (index > 0) 0f else (1f - (offset / 300f)).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Settings Gear Icon and Gradient previews placed at the top-right corner
        if (viewModel.homeViewState == HomeViewState.DASHBOARD) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .alpha(dashboardAlpha)
                .zIndex(5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (theme.id == "liquid_glass") {
                val styles = listOf(
                    com.raj.kotlinmusic.LiquidGlassBgStyle.SUNSET,
                    com.raj.kotlinmusic.LiquidGlassBgStyle.BLUE,
                    com.raj.kotlinmusic.LiquidGlassBgStyle.PINK,
                    com.raj.kotlinmusic.LiquidGlassBgStyle.FOREST
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(previewSpacing)
                ) {
                    styles.forEach { style ->
                        val isSelected = viewModel.liquidGlassBgStyle == style
                        val brush = when (style) {
                            com.raj.kotlinmusic.LiquidGlassBgStyle.SUNSET -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(theme.accent3, theme.accent1))
                            com.raj.kotlinmusic.LiquidGlassBgStyle.BLUE -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1E3C72), Color(0xFF00C6FF)))
                            com.raj.kotlinmusic.LiquidGlassBgStyle.PINK -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF7000FF), Color(0xFFFF758C)))
                            com.raj.kotlinmusic.LiquidGlassBgStyle.FOREST -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF0B4F30), Color(0xFF00FF87)))
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(brush = brush)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isBgSelectorExpanded) {
                                        isBgSelectorExpanded = true
                                    } else {
                                        viewModel.liquidGlassBgStyle = style
                                        isBgSelectorExpanded = false
                                    }
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(
                onClick = { viewModel.showSettingsPage = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        }

        // Full screen Settings Page overlay
        AnimatedVisibility(
            visible = viewModel.showSettingsPage,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            SettingsPage(
                viewModel = viewModel,
                theme = theme,
                onClose = { viewModel.showSettingsPage = false }
            )
        }

        // ── Search Overlay ──
        AnimatedVisibility(
            visible = isSearchActive,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor.copy(alpha = 0.93f))
            ) {
                // Dismiss tap area — covers top/bottom around the search box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            isSearchActive = false
                            searchQuery = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                )

                // Search content — sits on top, blocks dismiss taps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .pointerInput(Unit) { /* consume all touches so dismiss box below doesn't fire */ }
                ) {
                    // Live results (above search input bar)
                    if (searchQuery.isNotBlank()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(16.dp)),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            if (searchResults.isEmpty()) {
                                item {
                                    Text(
                                        text = "No songs found",
                                        color = mutedTextColor,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                items(searchResults, key = { it.id }) { song ->
                                    SongListItem(
                                        song = song,
                                        onClick = {
                                            isSearchActive = false
                                            searchQuery = ""
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.selectSong(song, searchResults)
                                        },
                                        theme = theme,
                                        dynamicAccentColor = viewModel.getSongAccentColor(song)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Search input bar (at the bottom, just above the keyboard)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBg)
                            .let { 
                                if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)) else it 
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = mutedTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Search songs & artists…", color = mutedTextColor, fontSize = 15.sp)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = textColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = mutedTextColor)
                            }
                        }
                    }
                }

                // Auto-focus keyboard when overlay opens
                androidx.compose.runtime.LaunchedEffect(isSearchActive) {
                    if (isSearchActive) {
                        kotlinx.coroutines.delay(150)
                        try {
                            focusRequester.requestFocus()
                        } catch (e: Exception) { /* TextField not yet composed */ }
                        kotlinx.coroutines.delay(50)
                        keyboardController?.show()
                    }
                }
            }
        }
    } // close root Box
}

@Composable
fun BentoBoxSection(viewModel: MusicViewModel) {
    val theme = viewModel.currentPalette
    val textColor = theme.text
    val subTextColor = theme.mutedText
    val liquidGlassColors = if (theme.id == "liquid_glass") viewModel.getLiquidGlassColors(theme) else emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your Library",
            color = subTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (theme.id == "liquid_glass") liquidGlassColors[2].copy(alpha = 0.2f) else theme.accent1)
                .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)) else it }
                .clickable {
                    viewModel.activeCategoryTitle = "Your Favorites"
                    viewModel.activeCategoryList = viewModel.favoritesList
                    viewModel.homeViewState = HomeViewState.CATEGORY_LIST
                }
                .padding(20.dp)
        ) {
            Column {
                Text("Your Favorites", color = theme.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Icon(Icons.Filled.Favorite, contentDescription = "Favorites", tint = theme.text, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.weight(1f))
                val favoritesCount = viewModel.playlist.count { it.isFavorite }
                Text("$favoritesCount Songs", color = theme.text.copy(alpha = 0.8f), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            // Add a large decorative icon on the right
            Icon(
                Icons.Filled.FavoriteBorder, 
                contentDescription = null, 
                tint = theme.text.copy(alpha = 0.1f), 
                modifier = Modifier.size(100.dp).align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Middle Row: Two square cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (theme.id == "liquid_glass") liquidGlassColors[1].copy(alpha = 0.2f) else theme.accent2)
                    .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)) else it }
                    .clickable {
                        viewModel.homeViewState = HomeViewState.PLAYLISTS_OVERVIEW
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Text("Playlists", color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${viewModel.customPlaylists.size} Lists", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(
                    Icons.Filled.List,
                    contentDescription = null,
                    tint = theme.text.copy(alpha = 0.1f),
                    modifier = Modifier.size(60.dp).align(Alignment.CenterEnd).offset(x = 10.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (theme.id == "liquid_glass") liquidGlassColors[0].copy(alpha = 0.2f) else theme.accent3)
                    .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp)) else it }
                    .clickable {
                        viewModel.activeCategoryTitle = "Top Artists"
                        viewModel.activeCategoryList = viewModel.artistsList
                        viewModel.homeViewState = HomeViewState.CATEGORY_LIST
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Text("Artists", color = theme.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${viewModel.playlist.size} Tracks", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = theme.text.copy(alpha = 0.1f),
                    modifier = Modifier.size(60.dp).align(Alignment.CenterEnd).offset(x = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Card: Categories
        val bottomCardBg = theme.surface
        var isCategoriesExpanded by remember { mutableStateOf(false) }

        if (viewModel.availableCategories.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(bottomCardBg)
                    .let { 
                        if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)) else it 
                    }
                    .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(theme.accent4), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Category, contentDescription = null, tint = theme.background, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("All Categories", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            if (isCategoriesExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = subTextColor
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = isCategoriesExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // FlowRow to display category chips
                            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.availableCategories.forEach { category ->
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 10.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(theme.background)
                                            .clickable {
                                                viewModel.activeCategoryTitle = "Category: $category"
                                                viewModel.activeCategoryList = viewModel.playlist.filter { it.categories.contains(category) }
                                                viewModel.homeViewState = HomeViewState.CATEGORY_LIST
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(text = category, color = textColor, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryListScreen(viewModel: MusicViewModel) {
    val theme = viewModel.currentPalette
    val bgColor = theme.background
    val textColor = theme.text

    BackHandler(enabled = viewModel.homeViewState == HomeViewState.CATEGORY_LIST) {
        viewModel.homeViewState = HomeViewState.DASHBOARD
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { viewModel.homeViewState = HomeViewState.DASHBOARD }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = viewModel.activeCategoryTitle,
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            val isCustomPlaylist = viewModel.customPlaylists.any { it.name == viewModel.activeCategoryTitle }
            if (isCustomPlaylist) {
                IconButton(onClick = { 
                    viewModel.playlistToEdit = viewModel.customPlaylists.find { it.name == viewModel.activeCategoryTitle }
                    viewModel.homeViewState = HomeViewState.EDIT_PLAYLIST
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Playlist", tint = textColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.activeCategoryList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tracks found.", color = theme.mutedText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(viewModel.activeCategoryList, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { viewModel.selectSong(song, viewModel.activeCategoryList) },
                        theme = theme,
                        dynamicAccentColor = viewModel.getSongAccentColor(song)
                    )
                }
            }
        }
    }
}


@Composable
fun BottomNavigationBar(theme: ColorPalette, modifier: Modifier = Modifier) {
    val navBg = if (theme.id == "liquid_glass") theme.surface else theme.surface.copy(alpha = 0.95f)
    val activeItemBg = theme.text
    val activeIconColor = theme.background
    val inactiveIconColor = theme.mutedText

    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(navBg)
            .let { 
                if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)) else it 
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Home (Active)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(activeItemBg)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Home, contentDescription = "Home", tint = activeIconColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Home", color = activeIconColor, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = inactiveIconColor)
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar", tint = inactiveIconColor)
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(Icons.Filled.Person, contentDescription = "Profile", tint = inactiveIconColor)
        }
    }
}

@Composable
fun MorningSuggestionGridCard(
    song: Song,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    patternType: Int,
    onClick: () -> Unit,
    theme: ColorPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (theme.id == "liquid_glass") color.copy(alpha = 0.2f) else color)
            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), shape) else it }
            .clickable { onClick() }
    ) {
        // Background Abstract Waveforms/Grid Pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val patternColor = theme.background.copy(alpha = 0.08f)
            
            when (patternType) {
                1 -> {
                    // Card 1 Waves (Top-Right Concentric Arcs)
                    for (r in 1..5) {
                        drawCircle(
                            color = patternColor,
                            radius = r * 18.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
                2 -> {
                    // Card 2 Waves (Bottom-Right/Top-Right Concentric Arcs)
                    for (r in 1..6) {
                        drawCircle(
                            color = patternColor,
                            radius = r * 16.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
                4 -> {
                    // Card 4 Circle (Concentric Internal Rings)
                    drawCircle(
                        color = patternColor,
                        radius = size.width / 2f - 12.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = patternColor,
                        radius = size.width / 2f - 24.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = strokeWidth)
                    )
                }
                5 -> {
                    // Card 5 Waves (Diagonal Spiderweb tr corner)
                    val tr = androidx.compose.ui.geometry.Offset(size.width, 0f)
                    for (i in 0..6) {
                        val x = size.width * (0.3f + i * 0.1f)
                        val y = size.height * (0.3f + (6 - i) * 0.1f)
                        drawLine(
                            color = patternColor,
                            start = tr,
                            end = androidx.compose.ui.geometry.Offset(x, y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
        }

        // Layout contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon slot
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = theme.text,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Texts
            Column {
                Text(
                    text = song.title,
                    color = theme.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = theme.text.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MorningSuggestionHorizontalCard(
    song: Song,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    theme: ColorPalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(shape)
            .background(if (theme.id == "liquid_glass") color.copy(alpha = 0.2f) else color)
            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), shape) else it }
            .clickable { onClick() }
    ) {
        // Background horizontal sine waves on the right
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val patternColor = theme.background.copy(alpha = 0.08f)
            val path = androidx.compose.ui.graphics.Path()
            
            // Draw 5 parallel horizontal sine waves
            for (w in 0..4) {
                val yOffset = size.height * 0.25f + w * 12.dp.toPx()
                path.reset()
                path.moveTo(size.width * 0.45f, yOffset)
                for (x in (size.width * 0.45f).toInt()..(size.width).toInt() step 5) {
                    val y = yOffset + Math.sin((x * 0.03) + w).toFloat() * 5.dp.toPx()
                    path.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path,
                    color = patternColor,
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // Layout contents
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = theme.text,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Texts
                Column {
                    Text(
                        text = song.title,
                        color = theme.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        color = theme.text.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Small premium play indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(theme.text),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = theme.background,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsPage(
    viewModel: MusicViewModel,
    theme: ColorPalette,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val initialPage = ThemePalettes.list.indexOfFirst { it.id == theme.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage)
    
    // Dynamically apply selected theme on swipe transition completion
    LaunchedEffect(pagerState.currentPage) {
        val targetPalette = ThemePalettes.list[pagerState.currentPage]
        if (targetPalette.id != theme.id) {
            viewModel.selectPalette(targetPalette)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
    ) {
        // --- Floating Ambient Blobs (Premium Design!) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (theme.id == "liquid_glass") {
                val liquidGlassColors = viewModel.getLiquidGlassColors(theme)
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
                    color = liquidGlassColors[2].copy(alpha = 0.08f),
                    radius = 350.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                )
                drawCircle(
                    color = liquidGlassColors[1].copy(alpha = 0.05f),
                    radius = 250.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.7f)
                )
            } else {
                drawCircle(
                    color = theme.accent1.copy(alpha = 0.08f),
                    radius = 350.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                )
                drawCircle(
                    color = theme.accent2.copy(alpha = 0.05f),
                    radius = 250.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.7f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .background(theme.surface, RoundedCornerShape(12.dp))

                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.text,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "SETTINGS",
                        color = theme.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "テーマ設定 / THEME CUSTOMIZER",
                        color = theme.mutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Label
            Text(
                text = "SWIPE TO CHANGE PALETTE",
                color = theme.mutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(horizontal = 28.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // The Swipeable Pager container with Left/Right chevrons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Chevron
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Theme",
                        tint = if (pagerState.currentPage > 0) theme.text else theme.mutedText.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Horizontal Pager for themes
                HorizontalPager(
                    pageCount = ThemePalettes.list.size,
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val palette = ThemePalettes.list[page]
                    val isCurrent = palette.id == theme.id
                    
                    // Card theme preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .graphicsLayer {
                                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val fraction = pageOffset.absoluteValue.coerceIn(0f, 1f)
                                val scale = 1f - (0.08f * fraction)
                                val alpha = 1f - (0.3f * fraction)
                                this.scaleX = scale
                                this.scaleY = scale
                                this.alpha = alpha
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .background(palette.surface)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) palette.accent1 else palette.text.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Theme Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = palette.name.uppercase(),
                                    color = palette.text,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isCurrent) "ACTIVE PALETTE" else "SWIPE TO ACTIVATE",
                                    color = if (isCurrent) palette.accent1 else palette.mutedText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // High Fidelity Miniature UI Preview Block!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.background)
                                    .border(1.dp, palette.text.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Mock UI Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(60.dp)
                                                .height(8.dp)
                                                .background(palette.text.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(palette.accent1, CircleShape)
                                        )
                                    }
                                    
                                    // Mock UI Track Preview Card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(palette.surface)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Box(
                                                    modifier = Modifier
                                                        .width(70.dp)
                                                        .height(10.dp)
                                                        .background(palette.text, RoundedCornerShape(4.dp))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .width(40.dp)
                                                        .height(6.dp)
                                                        .background(palette.mutedText, RoundedCornerShape(3.dp))
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(palette.accent2)
                                            )
                                        }
                                    }
                                    
                                    // Mock Control Capsule/Slider
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(palette.surface)
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(palette.accent3, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .background(palette.text.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.6f)
                                                    .fillMaxHeight()
                                                    .background(palette.accent4, RoundedCornerShape(2.dp))
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(palette.text.copy(alpha = 0.6f), CircleShape)
                                        )
                                    }
                                }
                            }

                            // Theme Colors Palette Swatches
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                PaletteColorSwatch(color = palette.background, label = "BG", textColor = palette.text)
                                PaletteColorSwatch(color = palette.surface, label = "SURF", textColor = palette.text)
                                PaletteColorSwatch(color = palette.accent1, label = "AC1", textColor = palette.background)
                                PaletteColorSwatch(color = palette.accent2, label = "AC2", textColor = palette.background)
                                PaletteColorSwatch(color = palette.accent3, label = "AC3", textColor = palette.background)
                                PaletteColorSwatch(color = palette.accent4, label = "AC4", textColor = palette.background)
                            }
                        }
                    }
                }

                // Right Chevron
                IconButton(
                    onClick = {
                        if (pagerState.currentPage < ThemePalettes.list.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage < ThemePalettes.list.size - 1,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Theme",
                        tint = if (pagerState.currentPage < ThemePalettes.list.size - 1) theme.text else theme.mutedText.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(ThemePalettes.list.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 6.dp)
                            .background(
                                color = if (isSelected) theme.accent1 else theme.mutedText.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            // Additional Settings Section: High-Fidelity switch
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surface)
                    .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)) else it }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HIFI AUDIO ENGINE",
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Enables 32-bit floating point processing",
                            color = theme.mutedText,
                            fontSize = 11.sp
                        )
                    }
                    var hifiEnabled by remember { mutableStateOf(true) }
                    Switch(
                        checked = hifiEnabled,
                        onCheckedChange = { hifiEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.background,
                            checkedTrackColor = theme.accent1,
                            uncheckedThumbColor = theme.mutedText,
                            uncheckedTrackColor = theme.surface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHAKE TO SHUFFLE",
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Shake device on cards screen to change song",
                            color = theme.mutedText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = viewModel.shakeToShuffleEnabled,
                        onCheckedChange = { viewModel.shakeToShuffleEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = theme.background,
                            checkedTrackColor = theme.accent1,
                            uncheckedThumbColor = theme.mutedText,
                            uncheckedTrackColor = theme.surface
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PLAYER CARD STYLE",
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Choose between wheel and flat list",
                            color = theme.mutedText,
                            fontSize = 11.sp
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.background.copy(alpha = 0.5f))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isWheel = viewModel.cardMode == CardMode.WHEEL
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isWheel) theme.accent1 else Color.Transparent)
                                .clickable { viewModel.cardMode = CardMode.WHEEL }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = "Wheel Mode",
                                tint = if (isWheel) theme.background else theme.text,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isWheel) theme.accent1 else Color.Transparent)
                                .clickable { viewModel.cardMode = CardMode.LIST }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "List Mode",
                                tint = if (!isWheel) theme.background else theme.text,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LYRICS STYLE",
                            color = theme.text,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Choose lyrics rendering style",
                            color = theme.mutedText,
                            fontSize = 11.sp
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.background.copy(alpha = 0.5f))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentStyle = viewModel.lyricsStyle
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentStyle == com.raj.kotlinmusic.LyricsStyle.WORD_BY_WORD) theme.accent1 else Color.Transparent)
                                .clickable { viewModel.lyricsStyle = com.raj.kotlinmusic.LyricsStyle.WORD_BY_WORD }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subject,
                                contentDescription = "Word by Word",
                                tint = if (currentStyle == com.raj.kotlinmusic.LyricsStyle.WORD_BY_WORD) theme.background else theme.text,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentStyle == com.raj.kotlinmusic.LyricsStyle.BUBBLE) theme.accent1 else Color.Transparent)
                                .clickable { viewModel.lyricsStyle = com.raj.kotlinmusic.LyricsStyle.BUBBLE }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Bubble Style",
                                tint = if (currentStyle == com.raj.kotlinmusic.LyricsStyle.BUBBLE) theme.background else theme.text,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentStyle == com.raj.kotlinmusic.LyricsStyle.TYPOGRAPHIC) theme.accent1 else Color.Transparent)
                                .clickable { viewModel.lyricsStyle = com.raj.kotlinmusic.LyricsStyle.TYPOGRAPHIC }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Typographic",
                                tint = if (currentStyle == com.raj.kotlinmusic.LyricsStyle.TYPOGRAPHIC) theme.background else theme.text,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // 3. FOLDER SCAN EXCLUSIONS Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surface)
                    .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)) else it }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "FOLDER SCAN EXCLUSIONS",
                    color = theme.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                // Add Folder Manual Input Row
                var newFolderInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = newFolderInput,
                        onValueChange = { newFolderInput = it },
                        placeholder = { Text("/path/to/exclude...", color = theme.mutedText.copy(alpha = 0.5f), fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = theme.background,
                            unfocusedContainerColor = theme.background,
                            focusedTextColor = theme.text,
                            unfocusedTextColor = theme.text,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            if (newFolderInput.isNotBlank()) {
                                viewModel.addExcludedFolder(newFolderInput)
                                newFolderInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(theme.accent1, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, "Add Folder", tint = theme.background)
                    }
                }

                // List of currently excluded folders
                if (viewModel.excludedFolders.isNotEmpty()) {
                    Text(
                        text = "EXCLUDED FOLDERS (${viewModel.excludedFolders.size})",
                        color = theme.mutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.excludedFolders.forEach { folderPath ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(theme.background)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folderPath,
                                    color = theme.text,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { viewModel.removeExcludedFolder(folderPath) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = theme.accent3,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Detected Folders list containing music
                val detectedList = viewModel.allDetectedFolders.filter { it !in viewModel.excludedFolders }
                if (detectedList.isNotEmpty()) {
                    Text(
                        text = "DETECTED MUSIC FOLDERS",
                        color = theme.mutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        detectedList.forEach { folderPath ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(theme.background)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folderPath,
                                    color = theme.text,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = { viewModel.addExcludedFolder(folderPath) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = theme.accent1),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("EXCLUDE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))



            Spacer(modifier = Modifier.height(24.dp))
            
            // Footer credits in Monospace
            Text(
                text = "MUZIK PLAYER v1.2.0 • BUILT WITH KOTLIN COMPOSE",
                color = theme.mutedText.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun PaletteColorSwatch(
    color: Color,
    label: String,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Display abbreviation inside swatch for premium feel
            Text(
                text = label.take(2),
                color = textColor.copy(alpha = 0.7f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ScrollAnimatedItem(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis.toLong())
        isVisible = true
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "alpha"
    )
    val offsetY by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else 40.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "offset"
    )
    
    Box(modifier = modifier.graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetY.toPx()
    }) {
        content()
    }
}

@Composable
fun PlaylistsScreen(viewModel: MusicViewModel) {
    val theme = viewModel.currentPalette
    val bgColor = theme.background
    val textColor = theme.text
    val subTextColor = theme.mutedText

    BackHandler(enabled = viewModel.homeViewState == HomeViewState.PLAYLISTS_OVERVIEW) {
        viewModel.homeViewState = HomeViewState.DASHBOARD
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.homeViewState = HomeViewState.DASHBOARD }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Text("Playlists", color = textColor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = { viewModel.homeViewState = HomeViewState.CREATE_PLAYLIST }) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = textColor)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.surface)
                            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)) else it }
                            .clickable {
                                viewModel.activeCategoryTitle = "All Songs"
                                viewModel.activeCategoryList = viewModel.playlist
                                viewModel.homeViewState = HomeViewState.CATEGORY_LIST
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = theme.accent1, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("All Songs", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${viewModel.playlist.size} songs", color = subTextColor, fontSize = 12.sp)
                        }
                    }
                }

                items(viewModel.customPlaylists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.surface)
                            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)) else it }
                            .clickable {
                                viewModel.activeCategoryTitle = playlist.name
                                viewModel.activeCategoryList = viewModel.playlist.filter { it.id in playlist.songIds }
                                viewModel.homeViewState = HomeViewState.CATEGORY_LIST
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = theme.accent1, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${playlist.songIds.size} songs", color = subTextColor, fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistScreen(viewModel: MusicViewModel) {
    val theme = viewModel.currentPalette
    var playlistName by remember { mutableStateOf("") }
    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    
    BackHandler(enabled = viewModel.homeViewState == HomeViewState.CREATE_PLAYLIST) {
        viewModel.homeViewState = HomeViewState.PLAYLISTS_OVERVIEW
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.homeViewState = HomeViewState.PLAYLISTS_OVERVIEW }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.text)
                }
                Text("New Playlist", color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Playlist Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accent1,
                    unfocusedBorderColor = theme.mutedText.copy(alpha = 0.5f),
                    focusedTextColor = theme.text,
                    unfocusedTextColor = theme.text,
                    focusedLabelColor = theme.accent1,
                    unfocusedLabelColor = theme.mutedText
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Songs (${selectedSongIds.size})", color = theme.text, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp) // space for bottom pills
            ) {
                items(viewModel.playlist) { song ->
                    val isSelected = selectedSongIds.contains(song.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent1.copy(alpha = 0.2f) else theme.surface)
                            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)) else it }
                            .clickable {
                                if (isSelected) selectedSongIds = selectedSongIds - song.id
                                else selectedSongIds = selectedSongIds + song.id
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) theme.accent1 else theme.mutedText, CircleShape)
                                .background(if (isSelected) theme.accent1 else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = theme.background, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = theme.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(song.artist, color = theme.mutedText, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        
        // Bottom Actions (Cancel / Confirm pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.homeViewState = HomeViewState.PLAYLISTS_OVERVIEW },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (theme.id == "white_candy") Color(0xFF1E272E) else theme.surface,
                    contentColor = if (theme.id == "white_candy") Color.White else theme.text
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = {
                    viewModel.createPlaylist(playlistName, selectedSongIds.toList())
                    viewModel.homeViewState = HomeViewState.PLAYLISTS_OVERVIEW
                },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (theme.id == "white_candy") Color(0xFF1E272E) else theme.accent1,
                    contentColor = if (theme.id == "white_candy") Color.White else theme.background
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EditPlaylistScreen(viewModel: MusicViewModel) {
    val theme = viewModel.currentPalette
    val playlist = viewModel.playlistToEdit ?: return
    
    var selectedSongIds by remember { mutableStateOf(playlist.songIds.toSet()) }
    
    BackHandler(enabled = viewModel.homeViewState == HomeViewState.EDIT_PLAYLIST) {
        viewModel.homeViewState = HomeViewState.CATEGORY_LIST
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.homeViewState = HomeViewState.CATEGORY_LIST }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = theme.text)
                }
                Text("Edit Playlist", color = theme.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Select Songs (${selectedSongIds.size})", color = theme.text, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp) // space for bottom pills
            ) {
                items(viewModel.playlist) { song ->
                    val isSelected = selectedSongIds.contains(song.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent1.copy(alpha = 0.2f) else theme.surface)
                            .let { if (theme.id == "liquid_glass") it.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)) else it }
                            .clickable {
                                if (isSelected) selectedSongIds = selectedSongIds - song.id
                                else selectedSongIds = selectedSongIds + song.id
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isSelected) theme.accent1 else theme.mutedText, CircleShape)
                                .background(if (isSelected) theme.accent1 else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = theme.background, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, color = theme.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(song.artist, color = theme.mutedText, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        
        // Bottom Actions (Cancel / Save pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.homeViewState = HomeViewState.CATEGORY_LIST },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (theme.id == "white_candy") Color(0xFF1E272E) else theme.surface,
                    contentColor = if (theme.id == "white_candy") Color.White else theme.text
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = {
                    viewModel.updatePlaylistSongs(playlist.id, selectedSongIds.toList())
                    viewModel.homeViewState = HomeViewState.CATEGORY_LIST
                },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (theme.id == "white_candy") Color(0xFF1E272E) else theme.accent1,
                    contentColor = if (theme.id == "white_candy") Color.White else theme.background
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}
