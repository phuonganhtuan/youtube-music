package com.example.youtubemusic.ui.main

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.youtubemusic.data.entity.PlayList
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.data.repo.PlayListRepo
import com.example.youtubemusic.data.repo.VideoRepo
import com.example.youtubemusic.utils.toFileName
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainViewModel(
    private val videoRepo: VideoRepo,
    private val playlistRepo: PlayListRepo
) : ViewModel() {

    val videoInfo = MutableLiveData<VideoInfo>()

    private val videoData = MutableLiveData<VideoInfo>()

    val errorMessage = MutableLiveData<String>()

    val notificationMessage = MutableLiveData<String>()

    val isUpdateDone = MutableLiveData<Boolean>().apply { value = false }

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

    fun getVideoData(id: String): Job = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest(id)
                request.addOption("-f", "best")
                val streamInfo = YoutubeDL.getInstance().getInfo(request)
                videoData.postValue(streamInfo)
            } catch (e: Exception) {
                notificationMessage.postValue("Something occured, trying again ...")
                getVideoData(id)
            }
        }
    }

    fun insertRecentNew() = viewModelScope.launch {
        videoInfo.value?.let {
            val video = Video(
                id = "https://youtu.be/${it.id}",
                title = it.fulltitle,
                thumbnail = it.thumbnails[2].url,
                url = it.url,
                description = it.description,
                duration = it.duration,
                scale = it.width / it.height.toFloat(),
                author = it.uploader
            )
            withContext(Dispatchers.IO) {
                videoRepo.insertRecent(video)
            }
        }
    }

    fun downloadVideo(id: String, location: File, name: String = "") = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val request = YoutubeDLRequest(id)
            request.addOption("-o", location.absolutePath + "/%(title)s.%(ext)s")
//            request.addOption("--extract-audio")
//            request.addOption("--audio-format", "mp3")
            try {
                YoutubeDL.getInstance().execute(request) { progress, _ ->
                    notificationMessage.postValue("Downloading $name: $progress%")
                }
            } catch (e: YoutubeDLException) {
                errorMessage.postValue("Please try again!")
            } catch (e: InterruptedException) {
                errorMessage.postValue("Please try again!")
            }
        }
    }

    fun updateVideo(video: Video) = viewModelScope.launch {
        videoRepo.updateVideo(video)
    }

    fun insertPL(title: String) = viewModelScope.launch {
        val playlist = PlayList(title = title)
        playlistRepo.insertPL(playlist)
    }

    fun updateLib(context: Context) = viewModelScope.launch {
        notificationMessage.value = "Preparing and checking for update ..."
        withContext(Dispatchers.IO) {
            YoutubeDL.getInstance().init(context)
            YoutubeDL.getInstance().updateYoutubeDL(context)
            isUpdateDone.postValue(true)
        }
    }

    fun checkAndDownloadVideos(location: File, names: List<String>) = viewModelScope.launch {
        val allVideos = videoRepo.getRecent()
        val needDownloadVideos = allVideos.filter {
            !names.contains(
                it.title.toFileName()
            )
        }
        if (needDownloadVideos.isNotEmpty()) {
            needDownloadVideos.forEach {
                downloadVideo(it.id, location, it.title)
            }
        }
    }
}
