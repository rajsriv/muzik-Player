package com.raj.kotlinmusic

object MusicPlayerController {
    var onTogglePlayPause: (() -> Unit)? = null
    var onNextSong: (() -> Unit)? = null
    var onPreviousSong: (() -> Unit)? = null
    var onServiceStateChanged: ((Song, Boolean) -> Unit)? = null
    var onOverlayStateChanged: ((Song, Boolean) -> Unit)? = null
    var onOverlayDismissed: (() -> Unit)? = null
}
