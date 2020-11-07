package com.example.youtubemusic.data

interface VideoRepo {

    suspend fun getRecent(): List<Video>
    suspend fun insertRecent(video: Video)
    suspend fun deleteRecent(id: String)
    suspend fun deleteAll()
}
