package com.example.youtubemusic.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Video(
    @PrimaryKey
    var id: String = "",
    @ColumnInfo(name = "thumbnail")
    var thumbnail: String = "",
    @ColumnInfo(name = "title")
    var title: String = ""
)
