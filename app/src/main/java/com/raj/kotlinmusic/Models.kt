package com.raj.kotlinmusic

import androidx.compose.ui.graphics.Color

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val path: String? = null,
    val artworkUrl: String? = null,
    val artworkRes: Int? = null, // Using resource IDs for mock data
    val lyricsAvailable: Boolean = false,
    val accentColor: Color = Color.Gray,
    val releaseDate: String = "2026-05",
    val compositionNote: String = "はじめて作ったピアノ曲。OP-1 Fieldのマルチトラックレコーダーで演奏しながらトラックを切り貼りして制作。",
    val gearSpecs: String = "Teenage Engineering OP-1 Field, CASIO CDP-S150 Electric Piano",
    var isFavorite: Boolean = false,
    var playCount: Int = 0,
    var morningPlayCount: Int = 0,
    var afternoonPlayCount: Int = 0,
    val categories: List<String> = emptyList()
)

enum class LyricsStyle {
    WORD_BY_WORD,
    BUBBLE,
    TYPOGRAPHIC
}

enum class PlayerState {
    CARDS,
    IMMERSIVE
}

enum class AppScreen {
    HOME,
    PLAYER,
    LYRICS
}

data class LyricLine(
    val timeMs: Long,
    val text: String
)

enum class HomeViewState {
    DASHBOARD,
    CATEGORY_LIST,
    PLAYLISTS_OVERVIEW,
    CREATE_PLAYLIST
}

data class CustomPlaylist(
    val id: String,
    val name: String,
    val songIds: List<String>
)

