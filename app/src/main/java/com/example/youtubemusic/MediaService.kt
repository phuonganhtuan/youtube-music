package com.example.youtubemusic

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.youtubemusic.data.Video
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.yausername.youtubedl_android.mapper.VideoInfo

class MediaService : Service() {

    private val serviceBinder = LocalBinder()

    val exoPlayer by lazy { SimpleExoPlayer.Builder(applicationContext).build() }

    var audioInfo: VideoInfo? = null

    var recentVideo: Video? = null

    var scale = 0f

    private val notification: Notification
        get() = run {
            val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(
                    "100111",
                    "Media Service ", NotificationManager.IMPORTANCE_NONE
                )
            } else null
            val intentOpenApp = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this.applicationContext, 23425, intentOpenApp,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel?.let(service::createNotificationChannel)
            }
//            NotificationCompat.Builder(this, "100111")
//                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
//                .setContentTitle(audioInfo?.title ?: "No song is being play")
//                .setContentText(audioInfo?.uploader ?: "Author")
//                .setContentIntent(pendingIntent)
//                .setShowWhen(false)
//                .setOngoing(true)
//                .build()
            NotificationCompat.Builder(this, "100111")
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setContentTitle(audioInfo?.title ?: "No song is being play")
                .setContentText(audioInfo?.uploader ?: "Author")
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .setOngoing(true)
                .build()
//            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//            .setSmallIcon(R.mipmap.ic_launcher)
//            .setCustomContentView(notificationLayout)
//            .build();
//            NotificationManager notificationManager =
//            (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            notificationManager.notify(1, notification);
        }
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onCreate() {
        super.onCreate()
        startForeground(
            100111,
            notification
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return serviceBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return true
    }

    fun prepareNewDataNew() {
        recentVideo = null
        startForeground(
            100111,
            notification
        )
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
        }
        audioInfo?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(it.url)))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        scale = audioInfo!!.width / audioInfo!!.height.toFloat()
    }

    fun prepareRecentData() {
        audioInfo = null
        startForeground(
            100111,
            notification
        )
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
        }
        recentVideo?.let {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(it.url)))
            exoPlayer.prepare()
            exoPlayer.play()
        }
        scale = recentVideo!!.scale
    }

    fun seek(progress: Long) {
        exoPlayer.seekTo(progress)
    }

    fun pause() = exoPlayer.pause()

    fun play() = exoPlayer.play()

    fun repeatOne() {
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
    }

    fun repeatAll() {
        exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
    }

    fun noRepeat() {
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun getDuration() = exoPlayer.duration

    fun getCurrentProgress() = exoPlayer.currentPosition

    inner class LocalBinder : Binder() {
        internal val service: MediaService by lazy {
            this@MediaService
        }
    }
}
