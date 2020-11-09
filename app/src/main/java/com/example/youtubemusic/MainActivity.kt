package com.example.youtubemusic

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.youtubemusic.data.AppDatabase
import com.example.youtubemusic.data.Video
import com.example.youtubemusic.data.VideoRepoImp
import com.example.youtubemusic.databinding.ActivityMainBinding
import com.example.youtubemusic.utils.NetworkUtil
import com.example.youtubemusic.utils.gone
import com.example.youtubemusic.utils.show
import com.google.android.exoplayer2.Player
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import jp.wasabeef.glide.transformations.BlurTransformation
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), VideoActivity.OnVideoStateChange {

    private lateinit var viewBinding: ActivityMainBinding

    private var isPlaying = false

    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(this).videoDao()) }
    private val viewModel by lazy { MainViewModel(videoRepo) }

    private var mediaService: MediaService? = null
    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    private var listener: OnNewVideoPlay? = null

    private var isRepeat = false

    private var defaultUiVisibility = 0

    private var broadcastReceiver: BroadcastReceiver? = null
    private var isUsingHeadphone = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MediaService.LocalBinder
            mediaService = binder?.service
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        YoutubeDL.getInstance().init(applicationContext)
        makeStatusBarTransparent()
        setupViews()
        startMediaService()
        handleEvents()
        observeData()
        defaultUiVisibility =
            window?.decorView?.systemUiVisibility ?: View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        listenToHeadphoneState()
    }

    override fun onStart() {
        super.onStart()
        mediaService?.let {
            if (it.audioInfo == null && it.recentVideo == null) return
            viewBinding.seekBar.max = it.getDuration().toInt()
            viewBinding.seekBar.progress = it.getCurrentProgress().toInt()
            updateSeekBar()
        }
    }

    override fun onStop() {
        super.onStop()
        disableSeekBarUpdate()
    }

    override fun onStateChange() {
        isPlaying = mediaService?.exoPlayer?.isPlaying ?: false
        if (isPlaying) displayPlayingState() else displayPauseState()
    }

    private fun listenToHeadphoneState() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                val iii: Int
                if (Intent.ACTION_HEADSET_PLUG == action) {
                    iii = intent.getIntExtra("state", -1)
                    if (iii == 0) {
                        if (isUsingHeadphone) {
                            if (mediaService?.recentVideo == null && mediaService?.audioInfo == null) return
                            isPlaying = false
                            displayPauseState()
                            mediaService?.pause()
                        }
                        isUsingHeadphone = false
                    }
                    if (iii == 1) {
                        isUsingHeadphone = true
                    }
                }
            }
        }
        val receiverFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        registerReceiver(broadcastReceiver, receiverFilter)
    }

    private fun setupViews() = with(viewBinding) {
        progressBar.gone()
        seekBar.gone()
        textProgress.gone()
        textDuration.gone()
        Glide.with(this@MainActivity)
            .load(R.drawable.bg_2)
            .apply(bitmapTransform(BlurTransformation(100, 3)))
            .into(imageBg)
        val fragment = RecentFragment.newInstance(::openRecent)
        listener = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContent, fragment)
            .commit()
    }

    private fun updateSeekBar() {
        runnable = object : Runnable {
            override fun run() {
                viewBinding.seekBar.progress = mediaService?.getCurrentProgress()?.toInt() ?: 0
                handler.postDelayed(this, 1000L)
            }
        }
        runnable?.let { handler.postDelayed(it, 1000L) }
    }

    private fun disableSeekBarUpdate() {
        runnable?.let { handler.removeCallbacks(it) }
    }

    private fun observeData() = with(viewModel) {
        errorMessage.observe(this@MainActivity, {
            viewBinding.apply {
                progressBar.gone()
                if (viewModel.videoInfo.value == null) {
                    seekBar.gone()
                    textProgress.gone()
                    textDuration.gone()
                }
            }
            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
        })
        videoInfo.observe(this@MainActivity, {
            setAudioDataNew(it)
        })
    }

    private fun startMediaService() {
        val serviceIntent = Intent(this, MediaService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(Intent(serviceIntent), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        mediaService?.stopForeground(true)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (viewBinding.layoutMainRoot.progress != 0f) {
            viewBinding.layoutMainRoot.transitionToStart()
            return
        }
        exitProcess(0)
    }

    private fun setAudioDataNew(videoInfo: VideoInfo) {
        viewBinding.apply {
            textTitle.text = videoInfo.fulltitle
            val thumbnail = videoInfo.thumbnails[2]
            Glide.with(this@MainActivity)
                .load(thumbnail.url)
                .into(imageAvatar)
            Glide.with(this@MainActivity)
                .load(thumbnail.url)
                .apply(bitmapTransform(BlurTransformation(100, 3)))
                .into(imageBg)
            val duration = videoInfo.duration * 1000L
            textLyrics.text = videoInfo.description
            seekBar.show()
            textProgress.show()
            textDuration.show()
            seekBar.max = duration.toInt()
            seekBar.progress = 0
            textProgress.text = getString(R.string.title_zero_progress)
            textDuration.text = getDurationFromMillis(duration)
            progressBar.gone()
            viewModel.insertRecentNew()
            editLink.text = null
            listener?.onNewVideoAdded(true)
            Glide.with(this@MainActivity)
                .asBitmap()
                .load(videoInfo.thumbnails[0].url)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        Palette.Builder(resource).generate {
                            it?.let { palette ->
                                val dominantColor = palette.getDominantColor(
                                    ContextCompat.getColor(
                                        this@MainActivity,
                                        R.color.pink
                                    )
                                )
                                layoutDes.background = ColorDrawable(dominantColor)
                                progressBar.indeterminateDrawable.setColorFilter(
                                    dominantColor,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                seekBar.progressDrawable.setColorFilter(
                                    dominantColor,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                val red = Color.red(dominantColor)
                                val green = Color.green(dominantColor)
                                val blue = Color.blue(dominantColor)
                                var textColor = 0
                                if ((red * 0.299 + green * 0.587 + blue * 0.114) > 186) {
                                    textColor = Color.BLACK
                                    window?.decorView?.systemUiVisibility =
                                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                } else {
                                    textColor = Color.WHITE
                                    window?.decorView?.systemUiVisibility = defaultUiVisibility
                                }
                                textLyrics.setTextColor(textColor)
                                imageMore.setColorFilter(
                                    textColor,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                listener?.onVideoColorChange(textColor)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
        }
        mediaService?.audioInfo = videoInfo
        mediaService?.prepareNewDataNew()
        if (isRepeat) mediaService?.repeatOne() else mediaService?.noRepeat()
        isPlaying = true
        displayPlayingState()
        mediaService?.exoPlayer?.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    Player.STATE_IDLE -> {
                    }
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_READY -> {
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        displayPauseState()
                    }
                }
            }
        })

    }

    private fun setAudioDataRecent(video: Video) {
        viewBinding.apply {
            textTitle.text = video.title
            val thumbnail = video.thumbnail
            Glide.with(this@MainActivity)
                .load(thumbnail)
                .into(imageAvatar)
            Glide.with(this@MainActivity)
                .load(thumbnail)
                .apply(bitmapTransform(BlurTransformation(100, 3)))
                .into(imageBg)
            val duration = video.duration * 1000L
            textLyrics.text = video.description
            seekBar.show()
            textProgress.show()
            textDuration.show()
            seekBar.max = duration.toInt()
            seekBar.progress = 0
            textProgress.text = getString(R.string.title_zero_progress)
            textDuration.text = getDurationFromMillis(duration)
            progressBar.gone()
            viewModel.insertRecentNew()
            editLink.text = null
            Glide.with(this@MainActivity)
                .asBitmap()
                .load(thumbnail)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        Palette.Builder(resource).generate {
                            it?.let { palette ->
                                val dominantColor = palette.getDominantColor(
                                    ContextCompat.getColor(
                                        this@MainActivity,
                                        R.color.pink
                                    )
                                )
                                layoutDes.background = ColorDrawable(dominantColor)
                                progressBar.indeterminateDrawable.setColorFilter(
                                    dominantColor,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                seekBar.progressDrawable.setColorFilter(
                                    dominantColor,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                val red = Color.red(dominantColor)
                                val green = Color.green(dominantColor)
                                val blue = Color.blue(dominantColor)
                                var textColor = 0
                                if ((red * 0.299 + green * 0.587 + blue * 0.114) > 186) {
                                    textColor = Color.BLACK
                                    window?.decorView?.systemUiVisibility =
                                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                } else {
                                    textColor = Color.WHITE
                                    window?.decorView?.systemUiVisibility = defaultUiVisibility
                                }
                                textLyrics.setTextColor(textColor)
                                imageMore.setColorFilter(
                                    textColor,
                                    PorterDuff.Mode.SRC_ATOP
                                )
                                listener?.onVideoColorChange(textColor)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            viewBinding.progressBar.gone()
        }
        mediaService?.recentVideo = video
        mediaService?.prepareRecentData()
        if (isRepeat) mediaService?.repeatOne() else mediaService?.noRepeat()
        isPlaying = true
        displayPlayingState()
        mediaService?.exoPlayer?.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                when (state) {
                    Player.STATE_IDLE -> {
                    }
                    Player.STATE_BUFFERING -> {
                    }
                    Player.STATE_READY -> {
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        displayPauseState()
                    }
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleEvents() = with(viewBinding) {
        imagePlayAndPause.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                v.performClick()
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            true
        }
        imageVideo.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                v.performClick()
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            true
        }
        imageAdd.setOnClickListener {
            if (editLink.text.isNullOrBlank()) return@setOnClickListener
            if (NetworkUtil.isInternetAvailable(this@MainActivity)) {
                progressBar.show()
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                viewBinding.editLink.clearFocus()
                viewModel.getAudioData(editLink.text.toString())
            } else {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.title_no_internet),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        imagePlayAndPause.setOnClickListener {
            if (mediaService?.recentVideo == null && mediaService?.audioInfo == null) return@setOnClickListener
            isPlaying = !isPlaying
            if (isPlaying) {
                displayPlayingState()
                mediaService?.let {
                    if (it.exoPlayer.playbackState == Player.STATE_ENDED) {
                        it.exoPlayer.seekTo(0)
                    }
                    it.play()
                }
            } else {
                displayPauseState()
                mediaService?.pause()
            }
        }
        imageRepeat.setOnClickListener {
            isRepeat = !isRepeat
            if (isRepeat) {
                mediaService?.repeatOne()
                imageRepeat.setImageResource(R.drawable.exo_controls_repeat_one)
            } else {
                mediaService?.noRepeat()
                imageRepeat.setImageResource(R.drawable.exo_controls_repeat_off)
            }
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textProgress.text = getDurationFromMillis(progress.toLong())
                if (fromUser) mediaService?.seek(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        imageVideo.setOnClickListener {
            if (mediaService?.recentVideo == null && mediaService?.audioInfo == null) return@setOnClickListener
            VideoActivity.newInstance(mediaService, this@MainActivity)
                .show(supportFragmentManager, VideoActivity::class.java.simpleName)
        }
    }

    private fun getDurationFromMillis(millis: Long) =
        if (TimeUnit.MILLISECONDS.toHours(millis) != 0L) {
            String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
            )
        } else {
            String.format(
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
            )
        }

    private fun displayPlayingState() {
        startRotateCD()
        viewBinding.imagePlayAndPause.setImageResource(R.drawable.ic_baseline_pause_24)
        updateSeekBar()
    }

    private fun displayPauseState() {
        stopRotateCD()
        viewBinding.imagePlayAndPause.setImageResource(R.drawable.ic_baseline_play_arrow_24)
    }

    private fun startRotateCD() {
        val rotation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        rotation.fillAfter = true
        viewBinding.cardAvatar.startAnimation(rotation)
    }

    private fun stopRotateCD() {
        viewBinding.cardAvatar.clearAnimation()
    }

    private fun makeStatusBarTransparent() {
        window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    private fun openRecent(item: Video) {
        if (NetworkUtil.isInternetAvailable(this)) {
            viewBinding.progressBar.show()
            if (item.url == "") {
                viewModel.getAudioData(item.id)
            } else {
                viewBinding.progressBar.show()
                setAudioDataRecent(item)
//                viewModel.insertRecent(item)
            }
        } else {
            Toast.makeText(this, getString(R.string.title_no_internet), Toast.LENGTH_SHORT).show()
        }
    }
}
