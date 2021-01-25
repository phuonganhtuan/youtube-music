package com.example.videomusic.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "title")
    var title: String = "",
    @ColumnInfo(name = "size")
    var size: Int = 0,
    @ColumnInfo(name = "elements")
    var elements: String = ""
)
