package com.example.videomusic.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.example.videomusic.data.entity.PlayList

@Dao
interface PlayListDao {

    @Query("Select * from PlayList")
    suspend fun getAllPLs(): List<PlayList>

    @Insert(onConflict = REPLACE)
    suspend fun insertPL(playList: PlayList)

    @Query("DELETE FROM PlayList WHERE id = :id")
    suspend fun deletePL(id: Int)

    @Query("DELETE FROM PlayList")
    suspend fun deleteAllPLs()

    @Update(onConflict = REPLACE)
    suspend fun updatePL(playList: PlayList)
}
