package com.example.youtubemusic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.example.youtubemusic.data.entity.Video

@Dao
interface VideoDao {

    @Query("Select * from Video")
    suspend fun getRecent(): List<Video>

    @Insert(onConflict = REPLACE)
    suspend fun insertRecent(video: Video)

    @Query("DELETE FROM Video WHERE id = :id")
    suspend fun deleteRecent(id: String)

    @Query("DELETE FROM Video")
    suspend fun deleteAll()

    @Query("Select * from Video where id in (:ids)")
    suspend fun getVideosByPL(ids: List<String>): List<Video>
}
