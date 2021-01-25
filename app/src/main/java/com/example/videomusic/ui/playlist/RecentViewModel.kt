package com.example.videomusic.ui.playlist

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videomusic.data.entity.PlayList
import com.example.videomusic.data.repo.PlayListRepo
import com.example.videomusic.data.repo.VideoRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentViewModel(
    private val videoRepo: VideoRepo,
    private val playListRepo: PlayListRepo,
    private val contentResolver: ContentResolver
) : ViewModel() {

//    var recent = MutableLiveData<List<Video>>()

//    var playLists = MutableLiveData<List<PlayList>>()

    var allVideo = listOf<String>()
    val videosLiveData: MutableLiveData<List<String>> = MutableLiveData()

//    fun getAllRecent() = viewModelScope.launch {
//        withContext(Dispatchers.IO) {
//            recent.postValue(videoRepo.getRecent().reversed())
//        }
//    }

//    fun deleteRecent(position: Int) = viewModelScope.launch {
//        withContext(Dispatchers.IO) {
//            val item = recent.value!![position]
//            videoRepo.deleteRecent(item.id)
//        }
//    }

    fun getAllPLs() = viewModelScope.launch {
        loadVideosFromSDCard()
        _allVideoAlbums.value = withContext(Dispatchers.IO) {
            loadAllVideoAlbum()
        }
    }

    fun getVideosByPL(pos: Int) = viewModelScope.launch {
        getVideoFromAlbum(pos)
    }

//    fun addVideosToPL(pos: Int, ids: List<String>) {
//        viewModelScope.launch {
//            val elements = ids.joinToString("*")
//            playLists.value!![pos].elements = elements
//            playListRepo.updatePL(playLists.value!![pos])
//            recent.value = videoRepo.getVideosByPL(ids)
//        }
//    }

    fun updatePL(playList: PlayList) = viewModelScope.launch {
        playListRepo.updatePL(playList)
    }

    fun deletePL(id: Int) = viewModelScope.launch {
        playListRepo.deletePL(id)
    }

    var currentVideoAlbumIndex = -1
    val currentVideoAlbum = MutableLiveData<String>().apply { value = "All" }
    val allVideoAbum: LiveData<List<String>> get() = _allVideoAlbums
    private val _allVideoAlbums = MutableLiveData<List<String>>()
    private var allAlbumTitle = ""

    fun getVideoFromAlbum(albumIndex: Int) {
        if (currentVideoAlbumIndex != albumIndex) {
            when (albumIndex) {
                -1 -> {
                    videosLiveData.value = allVideo
                    currentVideoAlbumIndex = -1
                    currentVideoAlbum.value = allAlbumTitle
                }
                else -> {
                    val album = allVideoAbum.value!![albumIndex]
                    val newVideos =
                        allVideo.filter { title -> title.contains("/$album/") }.shuffled()
                    videosLiveData.value = newVideos
                    if (newVideos.isNotEmpty()) {
                        currentVideoAlbumIndex = albumIndex
                        currentVideoAlbum.value = album
                    }
                }
            }
        }
    }

    private fun loadAllVideoAlbum(): List<String> {
        val projection =
            arrayOf("DISTINCT " + MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME)
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        val list = mutableListOf<String>()
        cursor?.let {
            while (it.moveToNext()) {
                list.add(it.getString((it.getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME))))
            }
        }
        cursor?.close()
        return list
    }

    private fun loadVideosFromSDCard(): List<String> {
        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val columnData: Int
        val listOfAllVideos = ArrayList<String>()
        var absolutePathOfImage: String?
        val projection =
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        cursor = contentResolver.query(uri, projection, null, null, null)
        columnData = cursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnData)
            listOfAllVideos.add(absolutePathOfImage)
        }
        cursor.close()
        allVideo = listOfAllVideos.shuffled()
        return allVideo
    }
}
