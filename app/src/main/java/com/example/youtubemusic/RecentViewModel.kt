package com.example.youtubemusic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.youtubemusic.data.Video
import com.example.youtubemusic.data.VideoRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentViewModel(private val videoRepo: VideoRepo) : ViewModel() {

    var recent = MutableLiveData<List<Video>>()

    fun getAllRecent() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            recent.postValue(videoRepo.getRecent().reversed())
        }
    }
}
