package com.example.videomusic.data.repo

import com.example.videomusic.data.entity.PlayList

interface PlayListRepo {

    suspend fun getAllPLs(): List<PlayList>
    suspend fun insertPL(playList: PlayList)
    suspend fun deletePL(id: Int)
    suspend fun deleteAllPLs()
    suspend fun updatePL(playList: PlayList)
}
