package com.example.youtubemusic.data.repo

import com.example.youtubemusic.data.entity.PlayList
import com.example.youtubemusic.data.dao.PlayListDao

class PlayListRepoImp(private val playListDao: PlayListDao) : PlayListRepo {

    override suspend fun getAllPLs() = playListDao.getAllPLs()
    override suspend fun insertPL(playList: PlayList) = playListDao.insertPL(playList)
    override suspend fun deletePL(id: Int) = playListDao.deletePL(id)
    override suspend fun deleteAllPLs() = playListDao.deleteAllPLs()
    override suspend fun updatePL(playList: PlayList) = playListDao.updatePL(playList)
}
