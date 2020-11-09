package com.example.youtubemusic

interface OnNewVideoPlay {
    fun onNewVideoAdded(isReset: Boolean)
    fun onVideoColorChange(color: Int)
}
