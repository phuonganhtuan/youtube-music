package com.example.youtubemusic

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
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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

    private var recentBitmap: Bitmap? = null

    private var newBitmap: Bitmap? = null

    private val recentNotification
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
            val notificationLayout =
                RemoteViews(applicationContext.packageName, R.layout.item_notification_collapsed)
            notificationLayout.apply {
                setTextViewText(
                    R.id.textSongTitle,
                    recentVideo?.title ?: "No song is being play"
                )
                setTextViewText(
                    R.id.textAuthor,
                    recentVideo?.author ?: "Author"
                )
                setImageViewBitmap(R.id.imageThumb, recentBitmap)
            }
            NotificationCompat.Builder(this@MediaService, "100111")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setCustomContentView(notificationLayout)
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .setOngoing(true)
                .build()
        }

    private val newNotification
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
            val notificationLayout =
                RemoteViews(applicationContext.packageName, R.layout.item_notification_collapsed)
            notificationLayout.apply {
                setTextViewText(
                    R.id.textSongTitle,
                    audioInfo?.title ?: "No song is being play"
                )
                setTextViewText(R.id.textAuthor, audioInfo?.uploader ?: "Author")
                setImageViewBitmap(R.id.imageThumb, newBitmap)
                setOnClickPendingIntent(
                    R.id.imageAction,
                    onButtonNotificationClick(R.id.imageAction)
                )
            }

            NotificationCompat.Builder(this@MediaService, "100111")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_baseline_music_note_24)
                .setCustomContentView(notificationLayout)
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

    fun prepareNewDataNew() {
        recentVideo = null
        Glide.with(this)
            .asBitmap()
            .load(audioInfo!!.thumbnails[2].url)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    newBitmap = resource
                    startForeground(
                        100111,
                        newNotification
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

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    fun prepareRecentData() {
        audioInfo = null
        Glide.with(this)
            .asBitmap()
            .load(recentVideo!!.thumbnail)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    recentBitmap = resource
                    startForeground(
                        100111,
                        recentNotification
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

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
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
