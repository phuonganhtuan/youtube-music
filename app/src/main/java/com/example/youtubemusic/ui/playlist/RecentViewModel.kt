package com.example.youtubemusic.ui.playlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.youtubemusic.data.entity.PlayList
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.data.repo.PlayListRepo
import com.example.youtubemusic.data.repo.VideoRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.Exception

class RecentViewModel(
    private val videoRepo: VideoRepo,
    private val playListRepo: PlayListRepo
) : ViewModel() {

    var recent = MutableLiveData<List<Video>>()

    var playLists = MutableLiveData<List<PlayList>>()

    fun getAllRecent() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            recent.postValue(videoRepo.getRecent().reversed())
        }
    }

    fun deleteRecent(position: Int) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val item = recent.value!![position]
            videoRepo.deleteRecent(item.id)
        }
    }

    fun getAllPLs() = viewModelScope.launch {
        playLists.value = playListRepo.getAllPLs()
    }

    fun getVideosByPL(pos: Int) = viewModelScope.launch {
        if (pos == 0) {
            getAllRecent()
        } else {
            if (playLists.value == null) return@launch
            val playList = playLists.value!![pos - 1]
            val videos = playList.elements
            val ids = videos.split("*")
            recent.value = videoRepo.getVideosByPL(ids)
        }
    }

    fun addVideosToPL(pos: Int, ids: List<String>) {
        viewModelScope.launch {
            val elements = ids.joinToString("*")
            try {
                playLists.value!![pos].elements = elements
                playListRepo.updatePL(playLists.value!![pos])
                recent.value = videoRepo.getVideosByPL(ids)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    fun updatePL(playList: PlayList) = viewModelScope.launch {
        playListRepo.updatePL(playList)
    }

    fun deletePL(id: Int) = viewModelScope.launch {
        playListRepo.deletePL(id)
    }

    fun insertPL(playList: PlayList) = viewModelScope.launch {
        playListRepo.insertPL(playList)
    }

    fun deleteAllPL() = viewModelScope.launch {
        playListRepo.deleteAllPLs()
    }
}
