package com.raj.kotlinmusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.raj.kotlinmusic.Song
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerStack(
    songs: List<Song>,
    initialSong: Song,
    onSongChanged: (Song) -> Unit,
    onPagerScroll: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialIndex = songs.indexOf(initialSong).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex)

    // Synchronize pagerState when the song changes externally (e.g. wheel click)
    LaunchedEffect(initialSong) {
        val targetIndex = songs.indexOf(initialSong).coerceAtLeast(0)
        if (pagerState.currentPage != targetIndex) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Notify song change only when the page actually changes, breaking circular feedback loops
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in songs.indices) {
                val targetSong = songs[page]
                if (targetSong != initialSong) {
                    onSongChanged(targetSong)
                }
            }
        }
    }

    // Notify scroll offset change for smooth overlay rendering
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }.collect { position ->
            onPagerScroll(position)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Vertical Pager for artwork stack
        VerticalPager(
            pageCount = songs.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 120.dp), // Adjust to see prev/next
            pageSpacing = (-80).dp // Overlap or tight spacing for "stack" feel
        ) { page ->
            // Pull ONLY the active page and its direct neighbors dynamically to optimize memory
            val distance = (page - pagerState.currentPage).absoluteValue
            if (distance <= 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(32.dp)
                )
            }
        }
    }
}
