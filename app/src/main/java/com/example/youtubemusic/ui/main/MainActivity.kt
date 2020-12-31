package com.example.youtubemusic.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.CustomTarget
import com.example.youtubemusic.R
import com.example.youtubemusic.data.db.AppDatabase
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.data.repo.PlayListRepoImp
import com.example.youtubemusic.data.repo.VideoRepoImp
import com.example.youtubemusic.databinding.ActivityMainBinding
import com.example.youtubemusic.ui.createpl.CreatePLFragment
import com.example.youtubemusic.ui.playlist.RecentFragment
import com.example.youtubemusic.utils.NetworkUtil
import com.example.youtubemusic.utils.gone
import com.example.youtubemusic.utils.show
import com.example.youtubemusic.utils.toFileName
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.yausername.youtubedl_android.mapper.VideoInfo
import jp.wasabeef.glide.transformations.BlurTransformation
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), VideoFragment.OnVideoStateChange,
    CreatePLFragment.OnPLCreate {

    private lateinit var viewBinding: ActivityMainBinding

    private var isPlaying = false

    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(this).videoDao()) }
    private val playListRepo by lazy {
        PlayListRepoImp(
            AppDatabase.invoke(this).playListDao()
        )
    }
    private val viewModel by lazy { MainViewModel(videoRepo, playListRepo) }

    private var mediaService: MediaService? = null
    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    private var listener: OnNewVideoPlay? = null

    private var isRepeat = false

    private var isRepeatAll = false

    private var defaultUiVisibility = 0

    private var broadcastReceiver: BroadcastReceiver? = null
    private var isUsingHeadphone = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MediaService.LocalBinder
            mediaService = binder?.service
            mediaService?.exoPlayer?.addListener(object : Player.EventListener {
                override fun onPlaybackStateChanged(state: Int) {
                    super.onPlaybackStateChanged(state)
                    when (state) {
                        Player.STATE_IDLE -> {
                            viewBinding.progressBar.gone()
                        }
                        Player.STATE_BUFFERING -> {
                            viewBinding.progressBar.show()
                        }
                        Player.STATE_READY -> {
                            viewBinding.progressBar.gone()
                        }
                        Player.STATE_ENDED -> {
                            isPlaying = false
                            displayPauseState()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    this@MainActivity.isPlaying = isPlaying
                    if (isPlaying) {
                        displayPlayingState()
                    } else {
                        displayPauseState()
                    }
                }

                override fun onTracksChanged(
                    trackGroups: TrackGroupArray,
                    trackSelections: TrackSelectionArray
                ) {
                    super.onTracksChanged(trackGroups, trackSelections)
                    if (mediaService!!.audioInfo == null && mediaService!!.recentVideo == null) return
                    if (mediaService!!.isPreparing) return
                    val pos = mediaService?.exoPlayer?.currentWindowIndex ?: 0
                    val newVideo = currentList[pos]
                    setupNewVideoViews(newVideo)
                    listener?.onNewTrackPlay(pos)
                }

                override fun onPlayerError(error: ExoPlaybackException) {
                    super.onPlayerError(error)
                    Toast.makeText(
                        this@MainActivity,
                        "Something occured, trying again ...",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewBinding.progressBar.show()
                    val pos = mediaService?.exoPlayer?.currentWindowIndex
                    pos?.let {
                        val video = currentList[pos]
                        viewModel.getVideoData(video.id)
                    }
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        if (NetworkUtil.isInternetAvailable(this)) {
            viewModel.updateLib(applicationContext)
        }
        makeStatusBarTransparent()
        setupViews()
        handleEvents()
        observeData()
        listenToHeadphoneState()
        startMediaService()
        defaultUiVisibility =
            window?.decorView?.systemUiVisibility ?: View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
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

    override fun onDestroy() {
        super.onDestroy()
        mediaService?.stopForeground(true)
    }

    override fun onBackPressed() {
        if (viewBinding.layoutMainRoot.progress != 0f) {
            viewBinding.layoutMainRoot.transitionToStart()
            return
        }
        mediaService?.stopForeground(true)
        exitProcess(0)
    }

    override fun onStateChange() {
        isPlaying = mediaService?.exoPlayer?.isPlaying ?: false
        if (isPlaying) displayPlayingState() else displayPauseState()
    }

    override fun onPLCreate(title: String) {
        viewModel.insertPL(title)
        listener?.onPLCreated()
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
        val fragment = RecentFragment.newInstance(::openRecent, ::handleTabChange, ::onStopPlaying)
        listener = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContent, fragment)
            .commit()
        Glide.with(this@MainActivity)
            .load(R.drawable.bg_2)
            .apply(bitmapTransform(BlurTransformation(100, 3)))
            .into(imageBg)
        progressBar.gone()
        seekBar.gone()
        textProgress.gone()
        textDuration.gone()
    }

    private fun checkAndDownloadAllVideos() {
        val downloadDir = getDownloadLocation() ?: return
        if (downloadDir.isDirectory) {
            val listVideos =
                downloadDir.listFiles()?.toList()
            val listVideoName = listVideos?.filter { isValidVideo(it) }
                ?.map { it.name.substring(0, it.name.length - 4) }
                ?: return
            if (listVideoName.isNotEmpty()) {
                showDownloadingScreen()
                viewModel.checkAndDownloadVideos(downloadDir, listVideoName)
            }
        }
    }

    private fun showDownloadingScreen() {

    }

    private fun isValidVideo(file: File) = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, Uri.fromFile(file))
        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
        val isValid = "yes" == hasVideo
        if (isValid) {
            true
        } else {
            file.delete()
            false
        }
    } catch (e: Exception) {
        false
    }

    private fun onStopPlaying() {
        isPlaying = false
        disableSeekBarUpdate()
        displayPauseState()
        viewBinding.apply {
            textTitle.text = "Song title"
            textLyrics.text = ""
            layoutDes.background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_black)
            imageAvatar.setImageDrawable(null)
            seekBar.gone()
            textProgress.gone()
            textDuration.gone()
            Glide.with(this@MainActivity)
                .load(R.drawable.bg_2)
                .apply(bitmapTransform(BlurTransformation(100, 3)))
                .into(imageBg)
            progressBar.indeterminateDrawable.setColorFilter(
                getColor(R.color.pink),
                PorterDuff.Mode.SRC_ATOP
            )
            seekBar.progressDrawable.setColorFilter(
                getColor(R.color.pink),
                PorterDuff.Mode.SRC_ATOP
            )
            window?.decorView?.systemUiVisibility = defaultUiVisibility
            imageCreatePL.setColorFilter(
                Color.WHITE,
                PorterDuff.Mode.SRC_ATOP
            )
        }
        listener?.onVideoColorChange(Color.WHITE)
        mediaService?.resetPlayer()
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
        errorMessage.observe(this@MainActivity, androidx.lifecycle.Observer {
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
        videoInfo.observe(this@MainActivity, androidx.lifecycle.Observer {
            setAudioDataNew(it)
        })
//        videoData.observe(this@MainActivity, androidx.lifecycle.Observer {
//            currentList[mediaService!!.exoPlayer.currentWindowIndex].url = it.url
//            mediaService?.updateList(currentList)
//            val video = currentList[mediaService!!.exoPlayer.currentWindowIndex]
//            video.url = it.url
//            viewModel.updateVideo(video)
//        })
        notificationMessage.observe(this@MainActivity, androidx.lifecycle.Observer {
            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
        })
        isUpdateDone.observe(this@MainActivity, Observer {
            if (NetworkUtil.isInternetAvailable(this@MainActivity)) {
                if (it) checkAndDownloadAllVideos()
            }
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

    private fun setAudioDataNew(videoInfo: VideoInfo) {
        viewModel.insertRecentNew()
        viewBinding.editLink.text = null
        listener?.onNewVideoAdded(true)
        viewBinding.progressBar.gone()
    }

    private var currentList = listOf<Video>()

    private fun setAudioDataRecent(video: Video, list: List<Video>) {
        val downloadDir = getDownloadLocation() ?: return
        val listName = list.map {
            it.title.toFileName() + ".mp4"
        }
        if (downloadDir.isDirectory) {
            val listVideos =
                downloadDir.listFiles()?.toList()?.filter { listName.contains(it.name) } ?: return
            val sortedFiles = mutableListOf<File>()
            list.forEach {
                val file = listVideos.firstOrNull { item ->
                    item.name == it.title.toFileName() + ".mp4"
                }
                file?.let { sortedFiles.add(it) }
            }
            currentList = list
            mediaService?.recentVideo = video
            mediaService?.prepareDataFromLocal(list, sortedFiles)
            isPlaying = true
            displayPlayingState()
        }
    }

    private fun setupNewVideoViews(video: Video) {
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
            Glide.with(this@MainActivity)
                .asBitmap()
                .load(thumbnail)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        Palette.Builder(resource).generate {
                            it?.let { palette ->
                                val dominantColor = palette.getDominantColor(
                                    ContextCompat.getColor(
                                        this@MainActivity,
                                        R.color.white
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
                                imageCreatePL.setColorFilter(
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
        mediaService?.setupNewNotification(video)
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
                viewBinding.editLink.clearFocus()
                viewModel.getAudioData(editLink.text.toString())
                if (isStoragePermissionGranted()) {
                    getDownloadLocation()?.let {
                        viewModel.downloadVideo(editLink.text.toString(), it)
                    }
                } else {
//                    imageAdd.performClick()
                }
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
        imagePrevious.setOnClickListener {
            if (mediaService?.recentVideo == null && mediaService?.audioInfo == null) return@setOnClickListener
            var new = mediaService!!.exoPlayer.currentWindowIndex - 1
            if (new == -1) new = currentList.size - 1
            mediaService?.exoPlayer?.seekTo(new, 0)
        }
        imageNext.setOnClickListener {
            if (mediaService?.recentVideo == null && mediaService?.audioInfo == null) return@setOnClickListener
            var new = mediaService!!.exoPlayer.currentWindowIndex + 1
            if (new == currentList.size) new = 0
            mediaService?.exoPlayer?.seekTo(new, 0)
        }
        imageRepeat.setOnClickListener {
            if (isRepeat) {
                if (isRepeatAll) {
                    isRepeat = false
                    isRepeatAll = false
                } else {
                    isRepeatAll = true
                }
            } else {
                isRepeat = true
                isRepeatAll = false
            }
            when {
                !isRepeat -> {
                    mediaService?.noRepeat()
                    imageRepeat.setImageResource(R.drawable.exo_controls_repeat_off)
                }
                isRepeatAll -> {
                    mediaService?.repeatAll()
                    imageRepeat.setImageResource(R.drawable.exo_controls_repeat_all)
                }
                !isRepeatAll -> {
                    mediaService?.repeatOne()
                    imageRepeat.setImageResource(R.drawable.exo_controls_repeat_one)
                }
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
            VideoFragment.newInstance(mediaService, this@MainActivity)
                .show(supportFragmentManager, VideoFragment::class.java.simpleName)
        }
        imageCreatePL.setOnClickListener {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            CreatePLFragment.newInstance(this@MainActivity)
                .show(supportFragmentManager, CreatePLFragment::class.java.simpleName)
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

    private fun openRecent(item: Video, list: List<Video>) {
        viewBinding.progressBar.show()
        viewBinding.progressBar.show()
        setAudioDataRecent(item, list)
    }

    private fun handleTabChange(pos: Int) {
//        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
//        if (pos == 0) showRecentState() else showPLState()
    }

    private fun showRecentState() = with(viewBinding) {
        editLink.show()
        imageAdd.show()
        imageCreatePL.show()
    }

    private fun showPLState() = with(viewBinding) {
        editLink.gone()
        imageAdd.gone()
        imageCreatePL.gone()
    }

    private fun getDownloadLocation(): File? {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "YoutubeMusic")
        if (!youtubeDLDir.exists()) {
            youtubeDLDir.mkdir()
        }
        return youtubeDLDir
    }

    private fun isStoragePermissionGranted() = run {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            false
        }
    }
}
