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
    var title: String = "",
    @ColumnInfo(name = "url")
    var url: String = "",
    @ColumnInfo(name = "description")
    var description: String = "",
    @ColumnInfo(name = "duration")
    var duration: Int = 0
)
