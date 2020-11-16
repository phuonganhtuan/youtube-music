package com.example.youtubemusic.ui.main

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.youtubemusic.R
import com.example.youtubemusic.data.entity.Video
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerNotificationManager
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
            NotificationCompat.Builder(this@MediaService, "100111")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setContentTitle("No song is being play")
                .setContentText("Author")
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .setOngoing(true)
                .build()
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

    private fun onButtonNotificationClick(@IdRes id: Int): PendingIntent? {
        val intent = Intent(ACTION_NOTIFICATION_BUTTON_CLICK)
        intent.putExtra(EXTRA_BUTTON_CLICKED, id)
        return PendingIntent.getBroadcast(this, id, intent, 0)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(EXTRA_BUTTON_CLICKED, -1)) {
                R.id.imageAction -> Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var isPreparing = false

    fun prepareRecentData(list: List<Video>) {
        vidList = list
        audioInfo = null
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
        }
        isPreparing = true
        exoPlayer.clearMediaItems()
        recentVideo?.let {
            val index = list.indexOf(it)
            val items = list.map { video ->
                MediaItem.fromUri(Uri.parse(video.url))
            }
            exoPlayer.setMediaItems(items)
            exoPlayer.seekTo(index, C.TIME_UNSET)
            isPreparing = false
            exoPlayer.prepare()
            exoPlayer.play()
        }
        scale = recentVideo!!.scale
    }

    private var vidList = listOf<Video>()

    fun updateList(list: List<Video>) {
        vidList = list
        isPreparing = true
        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
        }
        val currentProgress = exoPlayer.currentPosition
        val currentTrackIndex = exoPlayer.currentWindowIndex
        exoPlayer.clearMediaItems()
        val items = list.map { video ->
            MediaItem.fromUri(Uri.parse(video.url))
        }
        exoPlayer.setMediaItems(items)
        exoPlayer.seekTo(currentTrackIndex, C.TIME_UNSET)
        exoPlayer.seekTo(currentProgress)
        isPreparing = false
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun setupNewNotification(video: Video) {
        Glide.with(this)
            .asBitmap()
            .load(video.thumbnail)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel(
                            "100111",
                            "Media Service ", NotificationManager.IMPORTANCE_NONE
                        )
                    } else null
                    val intentOpenApp = Intent(this@MediaService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this@MediaService.applicationContext, 23425, intentOpenApp,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val service =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        channel?.let(service::createNotificationChannel)
                    }
                    val mediaDescriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter {

                        override fun getCurrentContentTitle(player: Player): String {
                            return vidList[exoPlayer.currentWindowIndex].title
                        }

                        override fun createCurrentContentIntent(player: Player): PendingIntent? {
                            return pendingIntent
                        }

                        override fun getCurrentContentText(player: Player): String {
                            return vidList[exoPlayer.currentWindowIndex].author
                        }

                        override fun getCurrentLargeIcon(
                            player: Player,
                            callback: PlayerNotificationManager.BitmapCallback
                        ): Bitmap? {
                            return resource
                        }
                    }

                    val playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
                        this@MediaService,
                        "1001112",
                        R.string.app_name,
                        100111,
                        mediaDescriptionAdapter,
                        object : PlayerNotificationManager.NotificationListener {
                            override fun onNotificationPosted(notificationId: Int, notification: Notification, ongoing: Boolean) {}

                            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {}
                        })

                    playerNotificationManager.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    playerNotificationManager.setSmallIcon(R.drawable.ic_baseline_music_note_24)
                    playerNotificationManager.setPlayer(exoPlayer)
//                    val notificationLayout =
//                        RemoteViews(
//                            applicationContext.packageName,
//                            R.layout.item_notification_collapsed
//                        )
//                    notificationLayout.apply {
//                        setTextViewText(
//                            R.id.textSongTitle,
//                            video.title
//                        )
//                        setTextViewText(R.id.textAuthor, video.author)
//                        setImageViewBitmap(R.id.imageThumb, resource)
//                        setOnClickPendingIntent(
//                            R.id.imageAction,
//                            onButtonNotificationClick(R.id.imageAction)
//                        )
//                    }
//
//                    val noti = NotificationCompat.Builder(this@MediaService, "100111")
//                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//                        .setSmallIcon(R.drawable.ic_baseline_music_note_24)
//                        .setCustomContentView(notificationLayout)
//                        .setContentIntent(pendingIntent)
//                        .setShowWhen(false)
//                        .setOngoing(true)
//                        .build()
//                    startForeground(
//                        100111,
//                        noti
//                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    fun resetPlayer() {
        audioInfo = null
        recentVideo = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        startForeground(
            100111,
            notification
        )
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

    companion object {
        const val ACTION_NOTIFICATION_BUTTON_CLICK = "click"
        const val EXTRA_BUTTON_CLICKED = "extra"
    }
}
