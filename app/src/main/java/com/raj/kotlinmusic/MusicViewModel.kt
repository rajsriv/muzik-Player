package com.raj.kotlinmusic

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.math.roundToInt
import com.raj.kotlinmusic.ui.theme.ColorPalette
import com.raj.kotlinmusic.ui.theme.ThemePalettes

enum class CardMode {
    WHEEL, LIST
}

enum class LiquidGlassBgStyle {
    SUNSET, BLUE, PINK, FOREST
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("muzik_prefs", Context.MODE_PRIVATE)

    private var _liquidGlassBgStyle = mutableStateOf(
        run {
            val saved = prefs.getString("liquid_glass_bg_style", LiquidGlassBgStyle.SUNSET.name)
            try { LiquidGlassBgStyle.valueOf(saved!!) } catch (e: Exception) { LiquidGlassBgStyle.SUNSET }
        }
    )
    var liquidGlassBgStyle: LiquidGlassBgStyle
        get() = _liquidGlassBgStyle.value
        set(value) {
            _liquidGlassBgStyle.value = value
            prefs.edit().putString("liquid_glass_bg_style", value.name).apply()
        }

    private var _cardMode = mutableStateOf(
        run {
            val saved = prefs.getString("card_mode", CardMode.WHEEL.name)
            try { CardMode.valueOf(saved!!) } catch (e: Exception) { CardMode.WHEEL }
        }
    )
    var cardMode: CardMode
        get() = _cardMode.value
        set(value) {
            _cardMode.value = value
            prefs.edit().putString("card_mode", value.name).apply()
        }
    
    private val _playlist = mutableStateOf(
        listOf(
            Song("1", "God's Plan", "Drake", lyricsAvailable = true, accentColor = Color(0xFFEE6557), playCount = 15, morningPlayCount = 4, afternoonPlayCount = 1, categories = listOf("Hip-Hop", "Pop")),
            Song("2", "Tuscan Leather", "Drake", lyricsAvailable = false, accentColor = Color(0xFFD3E382), playCount = 42, morningPlayCount = 0, afternoonPlayCount = 12, categories = listOf("Hip-Hop", "Rap")),
            Song("3", "Helen Keller", "Kai Ka\$h", lyricsAvailable = true, accentColor = Color(0xFFF5C754), playCount = 3, morningPlayCount = 1, afternoonPlayCount = 0, categories = listOf("Rap", "Underground")),
            Song("4", "Dark Fantasy", "Kanye West", lyricsAvailable = true, accentColor = Color(0xFFEE6557), playCount = 88, morningPlayCount = 15, afternoonPlayCount = 20, categories = listOf("Hip-Hop", "Classic")),
            Song("5", "Glimpse of Us", "Joji", lyricsAvailable = false, accentColor = Color(0xFFD3E382), playCount = 10, morningPlayCount = 0, afternoonPlayCount = 2, categories = listOf("Lo-Fi", "Pop", "Sad")),
            Song("6", "All My Friends", "Madeon", lyricsAvailable = true, accentColor = Color(0xFFF5C754), playCount = 27, morningPlayCount = 5, afternoonPlayCount = 10, categories = listOf("Electronic", "Pop")),
            Song("7", "Survival", "Drake", lyricsAvailable = false, accentColor = Color(0xFFEE6557), playCount = 5, morningPlayCount = 0, afternoonPlayCount = 3, categories = listOf("Hip-Hop"))
        )
    )


    private fun loadSongStats(songs: List<Song>): List<Song> {
        return songs.map { song ->
            song.copy(
                playCount = prefs.getInt("song_${song.id}_playCount", song.playCount),
                morningPlayCount = prefs.getInt("song_${song.id}_morningCount", song.morningPlayCount),
                afternoonPlayCount = prefs.getInt("song_${song.id}_afternoonCount", song.afternoonPlayCount),
                isFavorite = prefs.getBoolean("song_${song.id}_favorite", song.isFavorite)
            )
        }
    }

    private fun saveSongStats(song: Song) {
        prefs.edit()
            .putInt("song_${song.id}_playCount", song.playCount)
            .putInt("song_${song.id}_morningCount", song.morningPlayCount)
            .putInt("song_${song.id}_afternoonCount", song.afternoonPlayCount)
            .putBoolean("song_${song.id}_favorite", song.isFavorite)
            .apply()
    }

    val playlist: List<Song> get() = _playlist.value

    val favoritesList: List<Song> get() = playlist.filter { it.isFavorite }.sortedBy { it.title }
    val moodList: List<Song> get() = playlist.sortedByDescending { it.playCount }.take(7)
    val artistsList: List<Song> get() = playlist.filter { it.artist.isNotBlank() }.sortedBy { it.artist }
    val availableCategories: List<String> get() = playlist.flatMap { it.categories }.distinct().sorted()

    val morningSuggestions: List<Song> get() {
        val topPlayed = playlist.filter { it.morningPlayCount > 0 }.sortedByDescending { it.morningPlayCount }.take(5)
        return if (topPlayed.size >= 5) topPlayed else topPlayed + playlist.filter { it !in topPlayed }.shuffled().take(5 - topPlayed.size)
    }
    val afternoonSuggestions: List<Song> get() {
        val topPlayed = playlist.filter { it.afternoonPlayCount > 0 }.sortedByDescending { it.afternoonPlayCount }.take(3)
        return if (topPlayed.size >= 3) topPlayed else topPlayed + playlist.filter { it !in topPlayed }.shuffled().take(3 - topPlayed.size)
    }

    var homeViewState by mutableStateOf(HomeViewState.DASHBOARD)
    var activeCategoryTitle by mutableStateOf("")
    var activeCategoryList by mutableStateOf<List<Song>>(emptyList())
    var currentSong by mutableStateOf(playlist[1])

    var isPlaying by mutableStateOf(false)
    var currentAppScreen by mutableStateOf(AppScreen.HOME)
    var playerState by mutableStateOf(PlayerState.CARDS)
    var isCapsuleExpanded by mutableStateOf(false)
    var isCapsuleInitialized by mutableStateOf(true)

    var lyricsText by mutableStateOf<String?>(null)
    var syncedLyricsList by mutableStateOf<List<LyricLine>>(emptyList())
    var lyricsOffsetMs by mutableStateOf(0L)
    var isLyricsLoading by mutableStateOf(false)
    var playbackPositionMs by mutableStateOf(0L)

    var shuffleEnabled by mutableStateOf(false)
    var repeatEnabled by mutableStateOf(false)
    var isOverlayDismissedByUser by mutableStateOf(false)
    var showSettingsPage by mutableStateOf(false)
    var isLightMode by mutableStateOf(prefs.getBoolean("is_light_mode", true))
    var currentPalette by mutableStateOf(ThemePalettes.getById(prefs.getString("selected_palette_id", "white_candy") ?: "white_candy"))

    fun selectPalette(palette: ColorPalette) {
        currentPalette = palette
        prefs.edit().putString("selected_palette_id", palette.id).apply()
    }

    fun getLiquidGlassColors(theme: ColorPalette): List<Color> {
        return when (liquidGlassBgStyle) {
            LiquidGlassBgStyle.SUNSET -> listOf(
                theme.accent3, // Amber Glow
                theme.accent2, // Sunset Red/Orange
                theme.accent1  // Sunset Gold
            )
            LiquidGlassBgStyle.BLUE -> listOf(
                Color(0xFF1E3C72), // Indigo
                Color(0xFF0072FF), // Royal Blue
                Color(0xFF00C6FF)  // Ice Blue
            )
            LiquidGlassBgStyle.PINK -> listOf(
                Color(0xFF7000FF), // Violet
                Color(0xFFFF007F), // Neon Pink
                Color(0xFFFF758C)  // Rose Pink
            )
            LiquidGlassBgStyle.FOREST -> listOf(
                Color(0xFF0B4F30), // Forest Green
                Color(0xFF00F2FE), // Teal
                Color(0xFF00FF87)  // Neon Mint
            )
        }
    }



    private var _shakeToShuffleEnabled = mutableStateOf(
        prefs.getBoolean("shake_to_shuffle_enabled", false)
    )
    var shakeToShuffleEnabled: Boolean
        get() = _shakeToShuffleEnabled.value
        set(value) {
            _shakeToShuffleEnabled.value = value
            prefs.edit().putBoolean("shake_to_shuffle_enabled", value).apply()
        }

    private var _lyricsStyle = mutableStateOf(
        run {
            val saved = prefs.getString("lyrics_style", LyricsStyle.WORD_BY_WORD.name)
            try { LyricsStyle.valueOf(saved!!) } catch (e: Exception) { LyricsStyle.WORD_BY_WORD }
        }
    )
    var lyricsStyle: LyricsStyle
        get() = _lyricsStyle.value
        set(value) {
            _lyricsStyle.value = value
            prefs.edit().putString("lyrics_style", value.name).apply()
        }

    var showLyricsSyncControls by mutableStateOf(false)
    var showSaveLyricsButton by mutableStateOf(false)
    var rawFetchedLyrics by mutableStateOf<String?>(null)
    var isStopped by mutableStateOf(true)

    private var _customVideoUris = mutableStateOf(
        run {
            val saved = prefs.getString("custom_video_uris", "") ?: ""
            if (saved.isEmpty()) emptyList<String>() else saved.split("|||")
        }
    )
    var customVideoUris: List<String>
        get() = _customVideoUris.value
        set(value) {
            _customVideoUris.value = value
            prefs.edit().putString("custom_video_uris", value.joinToString("|||")).apply()
        }

    private var _customPlaylists = mutableStateOf<List<CustomPlaylist>>(
        run {
            val saved = prefs.getString("custom_playlists", "") ?: ""
            if (saved.isEmpty()) emptyList()
            else {
                saved.split("|||").mapNotNull {
                    val parts = it.split(";;;")
                    if (parts.size == 3) {
                        CustomPlaylist(parts[0], parts[1], if (parts[2].isEmpty()) emptyList() else parts[2].split(","))
                    } else null
                }
            }
        }
    )
    var customPlaylists: List<CustomPlaylist>
        get() = _customPlaylists.value
        set(value) {
            _customPlaylists.value = value
            val serialized = value.joinToString("|||") { "${it.id};;;${it.name};;;${it.songIds.joinToString(",")}" }
            prefs.edit().putString("custom_playlists", serialized).apply()
        }

    fun createPlaylist(name: String, songIds: List<String>) {
        val newPlaylist = CustomPlaylist(
            id = java.util.UUID.randomUUID().toString(),
            name = name.ifBlank { "My Playlist ${customPlaylists.size + 1}" },
            songIds = songIds
        )
        customPlaylists = customPlaylists + newPlaylist
    }

    fun deletePlaylist(id: String) {
        customPlaylists = customPlaylists.filter { it.id != id }
    }

    var playlistToEdit by mutableStateOf<CustomPlaylist?>(null)

    fun updatePlaylistSongs(id: String, newSongIds: List<String>) {
        customPlaylists = customPlaylists.map {
            if (it.id == id) it.copy(songIds = newSongIds) else it
        }
        // If we are currently viewing this playlist in CategoryList, refresh the view
        val editedPlaylist = customPlaylists.find { it.id == id }
        if (editedPlaylist != null && activeCategoryTitle == editedPlaylist.name) {
            activeCategoryList = playlist.filter { it.id in newSongIds }
        }
    }
    init {
        _playlist.value = loadSongStats(_playlist.value)
        
        // Auto-load real storage songs if permission is already granted
        val audioPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val isAudioGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            application,
            audioPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (isAudioGranted) {
            loadSongsFromStorage(application)
        }
        fetchLyrics(currentSong)
    }

    private var _excludedFolders = mutableStateOf(
        prefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()
    )
    val excludedFolders: Set<String> get() = _excludedFolders.value

    var allDetectedFolders by mutableStateOf<List<String>>(emptyList())
        private set

    fun addExcludedFolder(path: String) {
        val normalized = normalizePath(path)
        if (normalized.isNotBlank()) {
            val newSet = _excludedFolders.value + normalized
            _excludedFolders.value = newSet
            prefs.edit().putStringSet("excluded_folders", newSet).apply()
            loadSongsFromStorage(getApplication())
        }
    }

    fun removeExcludedFolder(path: String) {
        val normalized = normalizePath(path)
        val newSet = _excludedFolders.value - normalized
        _excludedFolders.value = newSet
        prefs.edit().putStringSet("excluded_folders", newSet).apply()
        loadSongsFromStorage(getApplication())
    }

    private fun normalizePath(path: String): String {
        return path.trim().removeSuffix("/")
    }



    fun getSongAccentColor(song: Song): Color {
        val defaultCoral = Color(0xFFEE6557)
        val defaultLime = Color(0xFFD3E382)
        val defaultYellow = Color(0xFFF5C754)
        
        return when (song.accentColor) {
            defaultCoral -> currentPalette.accent1
            defaultLime -> currentPalette.accent2
            defaultYellow -> currentPalette.accent3
            else -> {
                val hash = (song.title + song.artist).hashCode()
                val accents = listOf(currentPalette.accent1, currentPalette.accent2, currentPalette.accent3, currentPalette.accent4)
                val index = Math.abs(hash) % accents.size
                accents[index]
            }
        }
    }

    fun toggleLightMode() {
        isLightMode = !isLightMode
        prefs.edit().putBoolean("is_light_mode", isLightMode).apply()
    }

    fun toggleFavorite(song: Song) {
        val newList = _playlist.value.toMutableList()
        val index = newList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            val updatedSong = song.copy(isFavorite = !song.isFavorite)
            newList[index] = updatedSong
            _playlist.value = newList
            saveSongStats(updatedSong)
            // If the currently playing song is updated, update its reference
            if (currentSong.id == song.id) {
                currentSong = updatedSong
            }
        }
    }

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var currentlyLoadedSongId: String? = null
    private var progressJob: Job? = null
    private var collapseJob: Job? = null

    var currentProgress by mutableStateOf(0f)

    init {
        MusicPlayerController.onTogglePlayPause = {
            togglePlayPause()
        }
        MusicPlayerController.onNextSong = {
            nextSong()
        }
        MusicPlayerController.onPreviousSong = {
            previousSong()
        }
    }

    fun startForegroundService(context: android.content.Context) {
        val intent = android.content.Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_START
            putExtra("title", currentSong.title)
            putExtra("artist", currentSong.artist)
            putExtra("isPlaying", isPlaying)
            
            val composeColor = currentSong.accentColor
            val colorInt = android.graphics.Color.argb(
                (composeColor.alpha * 255).toInt(),
                (composeColor.red * 255).toInt(),
                (composeColor.green * 255).toInt(),
                (composeColor.blue * 255).toInt()
            )
            putExtra("accentColor", colorInt)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun startFloatingService(context: android.content.Context) {
        // No-op: Floating bar removed
    }

    @Suppress("UNUSED_PARAMETER")
    fun stopFloatingService(context: android.content.Context) {
        // No-op: Floating bar removed
    }

    private fun notifyServiceState() {
        MusicPlayerController.onServiceStateChanged?.invoke(currentSong, isPlaying)
        MusicPlayerController.onOverlayStateChanged?.invoke(currentSong, isPlaying)
    }

    val currentTimeString: String
        get() {
            val mp = mediaPlayer ?: return "0:00"
            val seconds = (mp.currentPosition / 1000) % 60
            val minutes = (mp.currentPosition / 1000) / 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val durationString: String
        get() {
            val mp = mediaPlayer ?: return "0:00"
            val seconds = (mp.duration / 1000) % 60
            val minutes = (mp.duration / 1000) / 60
            return String.format("%d:%02d", minutes, seconds)
        }

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    
    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            android.media.AudioManager.AUDIOFOCUS_LOSS -> {
                if (isPlaying) togglePlayPause()
                abandonAudioFocus()
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying) togglePlayPause()
            }
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                // Optionally resume playback if it was transiently paused
            }
        }
    }

    private val becomingNoisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: android.content.Intent) {
            if (intent.action == android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (isPlaying) {
                    togglePlayPause()
                }
            }
        }
    }
    
    init {
        application.registerReceiver(
            becomingNoisyReceiver,
            android.content.IntentFilter(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
    }

    private fun requestAudioFocus(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                android.media.AudioManager.STREAM_MUSIC,
                android.media.AudioManager.AUDIOFOCUS_GAIN
            ) == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    fun playSong(song: Song) {
        isStopped = false
        isOverlayDismissedByUser = false
        currentlyLoadedSongId = song.id
        fetchLyrics(song)
        if (song.path != null) {
            try {
                mediaPlayer?.release()
                mediaPlayer = null
                
                requestAudioFocus()
                
                mediaPlayer = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(song.path)
                    prepare()
                    setOnCompletionListener {
                        if (repeatEnabled) {
                            playSong(song)
                        } else {
                            nextSong()
                        }
                    }
                    start()
                }
                isPlaying = true
                startProgressTracker()
            } catch (e: Exception) {
                e.printStackTrace()
                isPlaying = false
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } else {
            // Mock playback state fallback
            isPlaying = true
            currentProgress = 0.4f
        }
        notifyServiceState()
    }
    
    fun stopSong() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.prepare() // Prepare it again so it can be played later from start
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isPlaying = false
        isStopped = true
        abandonAudioFocus()
        notifyServiceState()
    }

    fun togglePlayPause() {
        isOverlayDismissedByUser = false
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            abandonAudioFocus()
        } else {
            if (mediaPlayer == null || currentlyLoadedSongId != currentSong.id) {
                playSong(currentSong)
            } else {
                requestAudioFocus()
                mediaPlayer?.start()
                isPlaying = true
                startProgressTracker()
            }
        }
        notifyServiceState()
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let { mp ->
            val targetPosition = (progress * mp.duration).roundToInt()
            mp.seekTo(targetPosition)
            currentProgress = progress
            playbackPositionMs = targetPosition.toLong()
        }
    }

    fun seekToPosition(positionMs: Long) {
        mediaPlayer?.let { mp ->
            val target = positionMs.coerceIn(0L, mp.duration.toLong()).toInt()
            mp.seekTo(target)
            if (mp.duration > 0) {
                currentProgress = target.toFloat() / mp.duration.toFloat()
            }
            playbackPositionMs = target.toLong()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying && mp.duration > 0) {
                        currentProgress = mp.currentPosition.toFloat() / mp.duration.toFloat()
                        playbackPositionMs = mp.currentPosition.toLong()
                    }
                }
                delay(80)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {}
        progressJob?.cancel()
        collapseJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        abandonAudioFocus()
    }

    fun nextSong() {
        val activeQueue = if (currentPlaybackQueue.isNotEmpty()) currentPlaybackQueue else playlist
        if (activeQueue.isEmpty()) return
        val target = if (shuffleEnabled && activeQueue.size > 1) {
            var randomSong = activeQueue.random()
            while (randomSong == currentSong) {
                randomSong = activeQueue.random()
            }
            randomSong
        } else {
            val currentIndex = activeQueue.indexOfFirst { it.id == currentSong.id }
            if (currentIndex in activeQueue.indices && currentIndex < activeQueue.size - 1) {
                activeQueue[currentIndex + 1]
            } else {
                activeQueue[0] // Loop back to start
            }
        }
        currentSong = target
        playSong(target)
    }

    fun previousSong() {
        val activeQueue = if (currentPlaybackQueue.isNotEmpty()) currentPlaybackQueue else playlist
        if (activeQueue.isEmpty()) return
        val target = if (shuffleEnabled && activeQueue.size > 1) {
            var randomSong = activeQueue.random()
            while (randomSong == currentSong) {
                randomSong = activeQueue.random()
            }
            randomSong
        } else {
            val currentIndex = activeQueue.indexOfFirst { it.id == currentSong.id }
            if (currentIndex in activeQueue.indices && currentIndex > 0) {
                activeQueue[currentIndex - 1]
            } else {
                activeQueue[activeQueue.size - 1] // Wrap around to end
            }
        }
        currentSong = target
        playSong(target)
    }

    var currentPlaybackQueue by mutableStateOf<List<Song>>(emptyList())

    fun selectSong(song: Song, contextQueue: List<Song>? = null) {
        if (contextQueue != null && contextQueue.isNotEmpty()) {
            currentPlaybackQueue = contextQueue
        } else if (currentPlaybackQueue.isEmpty()) {
            currentPlaybackQueue = playlist
        }
        val wasAlreadyPlaying = (currentSong == song && isPlaying)
        currentSong = song
        isCapsuleInitialized = true
        if (!wasAlreadyPlaying) {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val updatedSong = song.copy(
                playCount = song.playCount + 1,
                morningPlayCount = if (hour in 5..11) song.morningPlayCount + 1 else song.morningPlayCount,
                afternoonPlayCount = if (hour in 12..16) song.afternoonPlayCount + 1 else song.afternoonPlayCount
            )
            val newList = _playlist.value.toMutableList()
            val index = newList.indexOfFirst { it.id == song.id }
            if (index != -1) {
                newList[index] = updatedSong
                _playlist.value = newList
                saveSongStats(updatedSong)
            }
            playSong(updatedSong)
        }
        notifyServiceState()
    }

    fun expandCapsule() {
        isCapsuleExpanded = true
        resetCollapseTimer()
    }

    fun collapseCapsule() {
        isCapsuleExpanded = false
        collapseJob?.cancel()
    }

    fun resetCollapseTimer() {
        if (!isCapsuleExpanded) return
        collapseJob?.cancel()
        collapseJob = CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            isCapsuleExpanded = false
        }
    }

    // Dynamic MediaStore Audio scanner to fetch real songs from user's storage
    fun loadSongsFromStorage(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val songList = mutableListOf<Song>()
            val foldersSet = mutableSetOf<String>()
            val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.DATA
            )
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${android.provider.MediaStore.Audio.Media.TITLE} ASC"

            try {
                context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn).toString()
                        val title = cursor.getString(titleColumn) ?: "Unknown Title"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val path = cursor.getString(dataColumn)

                        if (path != null) {
                            val parent = java.io.File(path).parentFile?.absolutePath?.trim()?.removeSuffix("/")
                            if (parent != null) {
                                foldersSet.add(parent)
                            }
                            
                            // Check if this file falls within any excluded folder
                            val parentPath = parent ?: ""
                            val isExcluded = excludedFolders.any { excluded ->
                                parentPath == excluded || parentPath.startsWith(excluded + "/")
                            }
                            if (isExcluded) {
                                continue
                            }
                        }

                        val accentColor = generatePremiumAccentColor(title, artist)

                        songList.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                path = path,
                                accentColor = accentColor
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                allDetectedFolders = foldersSet.toList().sorted()
                if (songList.isNotEmpty()) {
                    val statsLoadedList = loadSongStats(songList)
                    _playlist.value = statsLoadedList
                    currentSong = statsLoadedList[0]
                    fetchLyrics(currentSong)
                } else {
                    // Fall back to empty or default mocked songs if all storage songs are excluded
                    _playlist.value = emptyList()
                }
            }
        }
    }

    private fun generatePremiumAccentColor(title: String, artist: String): Color {
        val hash = (title + artist).hashCode()
        val curatedColors = listOf(
            Color(0xFFD3E382),
            Color(0xFFF5C754),
            Color(0xFFEE6557)
        )
        val index = Math.abs(hash) % curatedColors.size
        return curatedColors[index]
    }



    private var lyricsJob: Job? = null

    private fun sanitizeTitle(title: String): String {
        var clean = title
        // Remove parenthetical notes like (Official Video), [Lyrics], (prod. by ...)
        clean = clean.replace(Regex("\\s*\\([^)]*\\)"), "")
        clean = clean.replace(Regex("\\s*\\[[^]]*\\]"), "")
        // Remove common suffixes starting with dash
        clean = clean.replace(Regex("(?i)\\s+-\\s+.*"), "") // e.g. "Song - Remastered" -> "Song"
        // Remove feature tags
        clean = clean.replace(Regex("(?i)\\s+(feat|ft)\\.?\\s+.*"), "") // e.g. "Song feat. Artist" -> "Song"
        return clean.trim()
    }

    private fun sanitizeArtist(artist: String): String {
        var clean = artist
        // Split by feat, ft, &, x, comma, and take the first part
        val delimiters = listOf("(?i)\\s+(feat|ft)\\.?\\s+", "(?i)\\s+&\\s+", "(?i)\\s+x\\s+", "\\s*,\\s*")
        for (delim in delimiters) {
            clean = clean.split(Regex(delim))[0]
        }
        return clean.trim()
    }

    private fun executeGetRequest(urlString: String): List<org.json.JSONObject> {
        val list = mutableListOf<org.json.JSONObject>()
        try {
            val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "MuzikApp/1.0 (contact: developer@example.com)")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = org.json.JSONArray(responseText)
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getJSONObject(i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun getOfflineLyricsFile(song: Song): java.io.File {
        val dir = java.io.File(getApplication<android.app.Application>().filesDir, "lyrics")
        if (!dir.exists()) dir.mkdirs()
        val safeId = song.id.replace(Regex("[^a-zA-Z0-9_-]"), "")
        return java.io.File(dir, "$safeId.lrc")
    }

    fun saveLyricsOffline(song: Song) {
        val raw = rawFetchedLyrics ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = getOfflineLyricsFile(song)
                file.writeText(raw)
                withContext(Dispatchers.Main) {
                    showSaveLyricsButton = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateMatchScore(result: org.json.JSONObject, cleanTitle: String, cleanArtist: String): Int {
        val resultTitle = result.optString("trackName", result.optString("name", "")).lowercase()
        val resultArtist = result.optString("artistName", "").lowercase()
        
        val targetTitle = cleanTitle.lowercase()
        val targetArtist = cleanArtist.lowercase()
        
        var score = 0
        
        // Title Match Scoring
        if (resultTitle == targetTitle) {
            score += 50
        } else if (resultTitle.contains(targetTitle) || targetTitle.contains(resultTitle)) {
            score += 20
        }
        
        // Artist Match Scoring
        if (resultArtist == targetArtist) {
            score += 50
        } else if (resultArtist.contains(targetArtist) || targetArtist.contains(resultArtist)) {
            score += 20
        }
        
        // Heavy penalty for wrong artist match when artist is known
        val isTargetArtistUnknown = targetArtist.isBlank()
            || targetArtist.equals("unknown artist", ignoreCase = true)
            || targetArtist.equals("<unknown>", ignoreCase = true)
            || targetArtist.equals("unknown", ignoreCase = true)
            || targetArtist.equals("unknown_artist", ignoreCase = true)
            || targetArtist.equals("unkown", ignoreCase = true)
            || targetArtist.equals("null", ignoreCase = true)

        if (!isTargetArtistUnknown) {
            val hasArtistMatch = resultArtist.contains(targetArtist) || targetArtist.contains(resultArtist)
            if (!hasArtistMatch) {
                score -= 100
            }
        }
        
        return score
    }

    private fun performSearch(title: String, artist: String): List<org.json.JSONObject> {
        val query = java.net.URLEncoder.encode(title, "UTF-8")
        val urlString = if (artist.isNotBlank()) {
            val artistQuery = java.net.URLEncoder.encode(artist, "UTF-8")
            "https://lrclib.net/api/search?track_name=$query&artist_name=$artistQuery"
        } else {
            "https://lrclib.net/api/search?track_name=$query"
        }
        return executeGetRequest(urlString)
    }

    private fun performSearchByQ(q: String): List<org.json.JSONObject> {
        val urlString = "https://lrclib.net/api/search?q=$q"
        return executeGetRequest(urlString)
    }
    fun updateLyricsOffset(song: Song, offsetMs: Long) {
        lyricsOffsetMs = offsetMs
        prefs.edit().putLong("lyrics_offset_${song.id}", offsetMs).apply()
    }

    fun fetchLyrics(song: Song) {
        lyricsJob?.cancel()
        lyricsText = null
        syncedLyricsList = emptyList()
        lyricsOffsetMs = prefs.getLong("lyrics_offset_${song.id}", 0L)
        isLyricsLoading = true
        showSaveLyricsButton = false
        rawFetchedLyrics = null
        
        lyricsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = getOfflineLyricsFile(song)
                if (file.exists()) {
                    val content = file.readText()
                    val parsed = parseLrc(content)
                    withContext(Dispatchers.Main) {
                        if (parsed.isNotEmpty()) {
                            syncedLyricsList = parsed
                        } else {
                            lyricsText = content
                        }
                        isLyricsLoading = false
                    }
                    return@launch
                }

                val cleanTitle = sanitizeTitle(song.title)
                val cleanArtist = sanitizeArtist(song.artist)
                
                val isUnknownArtist = cleanArtist.equals("Unknown Artist", ignoreCase = true)
                    || cleanArtist.equals("<unknown>", ignoreCase = true)
                    || cleanArtist.equals("unknown", ignoreCase = true)
                    || cleanArtist.equals("unknown_artist", ignoreCase = true)
                    || cleanArtist.equals("unkown", ignoreCase = true)
                    || cleanArtist.equals("null", ignoreCase = true)
                    || cleanArtist.isBlank()
                
                // Try 1: Parameterized search with sanitized values
                var results = performSearch(cleanTitle, if (isUnknownArtist) "" else cleanArtist)
                
                // Try 2: General query fallback with artist and title
                if (results.isEmpty() && !isUnknownArtist) {
                    val q = java.net.URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")
                    results = performSearchByQ(q)
                }
                
                // Try 3: General query fallback with title only
                if (results.isEmpty()) {
                    val q = java.net.URLEncoder.encode(cleanTitle, "UTF-8")
                    results = performSearchByQ(q)
                }
                
                // Calculate match scores and filter out wrong matches
                val scoredResults = results.map { json ->
                    json to calculateMatchScore(json, cleanTitle, cleanArtist)
                }.filter { it.second >= 0 } // Drop matches with wrong artists
                 .sortedByDescending { it.second }
                
                if (scoredResults.isNotEmpty()) {
                    val bestMatch = scoredResults[0].first
                    val synced = bestMatch.optString("syncedLyrics", "")
                    val plain = bestMatch.optString("plainLyrics", "")
                    
                    withContext(Dispatchers.Main) {
                        if (!synced.isNullOrEmpty()) {
                            syncedLyricsList = parseLrc(synced)
                            rawFetchedLyrics = synced
                            showSaveLyricsButton = true
                        } else if (!plain.isNullOrEmpty()) {
                            lyricsText = plain
                            rawFetchedLyrics = plain
                            showSaveLyricsButton = true
                        } else {
                            lyricsText = "No lyrics text found."
                        }
                        isLyricsLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        lyricsText = "No matching lyrics found."
                        isLyricsLoading = false
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    lyricsText = "No lyrics found."
                    isLyricsLoading = false
                }
            }
        }
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = lrcText.split("\n")
        val result = mutableListOf<LyricLine>()
        val timeRegex = Regex("\\[(\\d+):(\\d+)(?:[.:](\\d+))?\\]")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val matches = timeRegex.findAll(trimmed).toList()
            if (matches.isEmpty()) continue

            var textIndex = 0
            for (match in matches) {
                textIndex = maxOf(textIndex, match.range.last + 1)
            }
            val lyricText = trimmed.substring(textIndex).trim()

            for (match in matches) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues.getOrNull(3) ?: "00"
                val ms = when (msStr.length) {
                    1 -> msStr.toLongOrNull()?.times(100) ?: 0L
                    2 -> msStr.toLongOrNull()?.times(10) ?: 0L
                    else -> msStr.take(3).toLongOrNull() ?: 0L
                }
                val timeMs = (min * 60 + sec) * 1000 + ms
                result.add(LyricLine(timeMs, lyricText))
            }
        }
        return result.sortedBy { it.timeMs }
    }

    fun deleteSavedLyrics() {
        val file = getOfflineLyricsFile(currentSong)
        if (file.exists()) {
            file.delete()
        }
        fetchLyrics(currentSong)
    }

    suspend fun generateShareBitmap(
        context: Context,
        lines: List<String>,
        bgColor1: Color,
        bgColor2: Color,
        aspectRatio: Int,
        fontId: Int,
        lyricsFontSize: Float = 72f
    ): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext null
        
        try {
            val width = when (aspectRatio) {
                2 -> 1920 // 16:9
                else -> 1080 // 1:1 or 9:16
            }
            val height = when (aspectRatio) {
                1 -> 1920 // 9:16
                else -> 1080 // 1:1 or 16:9
            }
            
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            val bgPaint = android.graphics.Paint().apply {
                if (bgColor1 == bgColor2) {
                    color = bgColor1.toArgb()
                } else {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        bgColor1.toArgb(),
                        bgColor2.toArgb(),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 3. Draw Artwork 
            val artSize = 150f
            val artPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                isAntiAlias = true
            }
            
            val resId = currentSong.artworkRes
            if (resId != null) {
                val rawBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                if (rawBitmap != null) {
                    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(rawBitmap, artSize.toInt(), artSize.toInt(), true)
                    val shader = android.graphics.BitmapShader(scaledBitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
                    artPaint.shader = shader
                }
            }
            
            val artRect = android.graphics.RectF(60f, 60f, 60f + artSize, 60f + artSize)
            canvas.drawRoundRect(artRect, 24f, 24f, artPaint)
            
            // 4. Draw Song Title and Artist
            val selectedTypeface = when (fontId) {
                1 -> androidx.core.content.res.ResourcesCompat.getFont(context, R.font.writer) ?: android.graphics.Typeface.SERIF
                2 -> androidx.core.content.res.ResourcesCompat.getFont(context, R.font.korvich_slam) ?: android.graphics.Typeface.DEFAULT_BOLD
                else -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            
            val textPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 48f
                isAntiAlias = true
                typeface = selectedTypeface
            }
            
            val maxTextWidth = width - (60f + artSize + 40f) - 60f
            val ellipsizedTitle = android.text.TextUtils.ellipsize(
                currentSong.title,
                textPaint,
                maxTextWidth,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            
            canvas.drawText(ellipsizedTitle, 60f + artSize + 40f, 110f, textPaint)
            
            val artistPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.LTGRAY
                textSize = 36f
                isAntiAlias = true
                typeface = selectedTypeface
            }
            
            val ellipsizedArtist = android.text.TextUtils.ellipsize(
                currentSong.artist,
                artistPaint,
                maxTextWidth,
                android.text.TextUtils.TruncateAt.END
            ).toString()
            
            canvas.drawText(ellipsizedArtist, 60f + artSize + 40f, 170f, artistPaint)
            
            // 5. Draw Lyrics using StaticLayout
            val lyricsText = lines.joinToString("\n")
            val lyricsTextPaint = android.text.TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = lyricsFontSize
                isAntiAlias = true
                typeface = selectedTypeface
            }
            
            val lyricsTextWidth = width - 120
            val staticLayout = android.text.StaticLayout.Builder.obtain(
                lyricsText, 0, lyricsText.length, lyricsTextPaint, lyricsTextWidth
            ).setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).build()
            
            val textHeight = staticLayout.height
            val startY = (height - textHeight) / 2f
            
            canvas.save()
            canvas.translate(60f, startY)
            staticLayout.draw(canvas)
            canvas.restore()
            
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun exportSharedBitmap(context: Context, bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 6. Save Bitmap to Cache
                val imagesDir = java.io.File(context.cacheDir, "images")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val imageFile = java.io.File(imagesDir, "shared_lyrics_${System.currentTimeMillis()}.png")
                
                val outputStream = java.io.FileOutputStream(imageFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                
                // 7. Fire Intent
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = android.content.Intent.createChooser(shareIntent, "Share lyrics to...")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main) {
                    context.startActivity(chooser)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error sharing: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
