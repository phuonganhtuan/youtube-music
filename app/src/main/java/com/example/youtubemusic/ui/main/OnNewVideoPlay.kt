package com.example.youtubemusic.ui.main

interface OnNewVideoPlay {
    fun onNewVideoAdded(isReset: Boolean)
    fun onVideoColorChange(color: Int)
    fun onPLCreated()
    fun onNewTrackPlay(index: Int)
}
