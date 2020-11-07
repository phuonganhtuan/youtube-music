package com.example.youtubemusic

import android.os.Environment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.youtubemusic.data.Video
import com.example.youtubemusic.data.VideoRepo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Exception


class MainViewModel(private val videoRepo: VideoRepo) : ViewModel() {

    val videoInfo = MutableLiveData<VideoInfo>()

    val errorMessage = MutableLiveData<String>()

    fun getAudioData(id: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest(id)
                request.addOption("-f", "best")
                val streamInfo = YoutubeDL.getInstance().getInfo(request)
                videoInfo.postValue(streamInfo)
            } catch (e: Exception) {
                errorMessage.postValue("Please try again!")
            }
        }
    }

    fun insertRecentNew() = viewModelScope.launch {
        videoInfo.value?.let {
            val video = Video(
                id = "https://youtu.be/${it.id}",
                title = it.fulltitle,
                thumbnail = it.thumbnails[2].url
            )
            withContext(Dispatchers.IO) {
                videoRepo.insertRecent(video)
            }
        }
    }

    fun downloadFile() {
        videoInfo.value?.let {
            val youtubeDLDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Youtube music"
            )
            val request = YoutubeDLRequest("https://youtu.be/${it.id}")
            request.addOption(
                "-o",
                youtubeDLDir.absolutePath.toString() + "/%(title)s.%(ext)s"
            )
            YoutubeDL.getInstance().execute(
                request
            ) { progress: Float, etaInSeconds: Long -> println("$progress% (ETA $etaInSeconds seconds)") }
        }
    }
}
