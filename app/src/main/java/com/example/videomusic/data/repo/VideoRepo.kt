package com.example.videomusic.data.repo

import com.example.videomusic.data.entity.Video

interface VideoRepo {

    suspend fun getRecent(): List<Video>
    suspend fun insertRecent(video: Video)
    suspend fun deleteRecent(id: String)
    suspend fun deleteAll()
    suspend fun getVideosByPL(ids: List<String>): List<Video>
    suspend fun updateVideo(video: Video)
}
