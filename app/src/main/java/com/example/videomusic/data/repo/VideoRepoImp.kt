package com.example.videomusic.data.repo

import com.example.videomusic.data.entity.Video
import com.example.videomusic.data.dao.VideoDao

class VideoRepoImp(private val videoDao: VideoDao) : VideoRepo {

    override suspend fun getRecent() = videoDao.getRecent()
    override suspend fun insertRecent(video: Video) = videoDao.insertRecent(video)
    override suspend fun deleteRecent(id: String) = videoDao.deleteRecent(id)
    override suspend fun deleteAll() = videoDao.deleteAll()
    override suspend fun getVideosByPL(ids: List<String>) = videoDao.getVideosByPL(ids)
    override suspend fun updateVideo(video: Video) = videoDao.updateVideo(video)
}
