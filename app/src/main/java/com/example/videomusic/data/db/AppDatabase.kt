package com.example.videomusic.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.videomusic.data.entity.PlayList
import com.example.videomusic.data.dao.PlayListDao
import com.example.videomusic.data.entity.Video
import com.example.videomusic.data.dao.VideoDao

@Database(entities = [Video::class, PlayList::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao
    abstract fun playListDao(): PlayListDao

    companion object {

        private const val DATABASE_NAME = "YTAUDIODB"

        @Volatile
        private var instance: AppDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            buildDatabase(context).also { instance = it }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context,
            AppDatabase::class.java, DATABASE_NAME
        ).build()
    }
}
