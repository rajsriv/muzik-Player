package com.raj.kotlinmusic

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.media.MediaMetadata

class MusicService : Service() {

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "kotlin_music_channel"
        const val NOTIFICATION_ID = 999
        
        const val ACTION_START = "com.raj.kotlinmusic.ACTION_START"
        const val ACTION_UPDATE = "com.raj.kotlinmusic.ACTION_UPDATE"
        const val ACTION_STOP = "com.raj.kotlinmusic.ACTION_STOP"
        
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.raj.kotlinmusic.ACTION_TOGGLE_PLAY_PAUSE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSession(this, "KotlinMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    MusicPlayerController.onTogglePlayPause?.invoke()
                }

                override fun onPause() {
                    super.onPause()
                    MusicPlayerController.onTogglePlayPause?.invoke()
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    MusicPlayerController.onNextSong?.invoke()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    MusicPlayerController.onPreviousSong?.invoke()
                }
            })
            isActive = true
        }
        
        // Listen to state changes from the ViewModel
        MusicPlayerController.onServiceStateChanged = { song, isPlaying ->
            updateMediaSessionMetadata(song.title, song.artist)
            updateMediaSessionState(isPlaying)
            updateNotification(song, isPlaying)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val songTitle = intent.getStringExtra("title") ?: "Unknown"
                val artist = intent.getStringExtra("artist") ?: "Unknown"
                val accentColorInt = intent.getIntExtra("accentColor", Color.GRAY)
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                
                updateMediaSessionMetadata(songTitle, artist)
                updateMediaSessionState(isPlaying)

                // Show initial notification and start foreground service
                startForeground(NOTIFICATION_ID, buildCustomNotification(songTitle, accentColorInt, isPlaying))
            }
            ACTION_UPDATE -> {
                val songTitle = intent.getStringExtra("title") ?: "Unknown"
                val artist = intent.getStringExtra("artist") ?: "Unknown"
                val accentColorInt = intent.getIntExtra("accentColor", Color.GRAY)
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                
                updateMediaSessionMetadata(songTitle, artist)
                updateMediaSessionState(isPlaying)

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildCustomNotification(songTitle, accentColorInt, isPlaying))
            }
            ACTION_TOGGLE_PLAY_PAUSE -> {
                MusicPlayerController.onTogglePlayPause?.invoke()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateMediaSessionState(isPlaying: Boolean) {
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun updateMediaSessionMetadata(title: String, artist: String) {
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updateNotification(song: Song, isPlaying: Boolean) {
        val colorInt = try {
            val composeColor = song.accentColor
            Color.argb(
                (composeColor.alpha * 255).toInt(),
                (composeColor.red * 255).toInt(),
                (composeColor.green * 255).toInt(),
                (composeColor.blue * 255).toInt()
            )
        } catch (e: Exception) {
            Color.GRAY
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildCustomNotification(song.title, colorInt, isPlaying))
    }

    private fun buildCustomNotification(
        title: String,
        accentColorInt: Int,
        isPlaying: Boolean
    ): Notification {
        val collapsedView = RemoteViews(packageName, R.layout.notification_mini_capsule)
        
        // Style exactly like our mini capsule
        collapsedView.setTextViewText(R.id.notification_song_title, title)
        collapsedView.setInt(R.id.notification_artwork, "setBackgroundColor", accentColorInt)
        
        // Toggle play/pause icon
        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        collapsedView.setImageViewResource(R.id.notification_play_pause, playPauseIcon)
        
        // PendingIntent for play/pause click
        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            1,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        collapsedView.setOnClickPendingIntent(R.id.notification_play_pause, playPausePendingIntent)
        
        // PendingIntent for clicking the notification to open MainActivity
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setCustomContentView(collapsedView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(openPendingIntent)
            .setOngoing(isPlaying) // Stay persistent while music is playing
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        mediaSession?.sessionToken?.let { token ->
            builder.setExtras(android.os.Bundle().apply {
                putParcelable("android.mediaSession", token)
            })
        }
            
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music Control Notification"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        MusicPlayerController.onServiceStateChanged = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
