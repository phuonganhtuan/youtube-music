package com.example.youtubemusic.data

class VideoRepoImp(private val videoDao: VideoDao) : VideoRepo {

    override suspend fun getRecent() = videoDao.getRecent()

    override suspend fun insertRecent(video: Video) = videoDao.insertRecent(video)

    override suspend fun deleteRecent(id: String) =
        videoDao.deleteRecent(id)

    override suspend fun deleteAll() = videoDao.deleteAll()
}
