package com.example.videomusic.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videomusic.data.entity.PlayList
import com.example.videomusic.data.repo.PlayListRepo
import com.example.videomusic.data.repo.VideoRepo
import kotlinx.coroutines.launch


class MainViewModel(
    private val videoRepo: VideoRepo,
    private val playlistRepo: PlayListRepo
) : ViewModel() {

    val notificationMessage = MutableLiveData<String>()

    fun insertPL(title: String) = viewModelScope.launch {
        val playlist = PlayList(title = title)
        playlistRepo.insertPL(playlist)
    }
}
