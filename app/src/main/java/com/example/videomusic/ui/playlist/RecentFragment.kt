package com.example.videomusic.ui.playlist

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.videomusic.R
import com.example.videomusic.data.db.AppDatabase
import com.example.videomusic.data.repo.PlayListRepoImp
import com.example.videomusic.data.repo.VideoRepoImp
import com.example.videomusic.databinding.FragmentListBinding
import com.example.videomusic.ui.main.OnNewVideoPlay
import com.example.videomusic.utils.gone
import com.example.videomusic.utils.show
import com.google.android.material.tabs.TabLayout

class RecentFragment : Fragment(), OnNewVideoPlay, RecentAdapter.OnRecentClick {

    private val adapter by lazy { RecentAdapter(this) }
    private lateinit var viewBinding: FragmentListBinding
    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(requireContext()).videoDao()) }
    private val playListRepo by lazy {
        PlayListRepoImp(
            AppDatabase.invoke(requireContext()).playListDao()
        )
    }
    private val viewModel by lazy { RecentViewModel(videoRepo, playListRepo, context!!.contentResolver) }
    private var onRecentClickListener: ((String, List<String>) -> Unit)? = null
    private var onTabChange: ((Int) -> Unit)? = null
    private var resetPosition = false
    private var isFirstTime = false
    private var isRecent = false

    private var currentPlayingTabIndex = -1
    private var currentPlayingSongIndex = -1

    private var onStopPlaying: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentListBinding.inflate(inflater)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
        handleEvents()
    }

    override fun onNewVideoAdded(isReset: Boolean) {

    }

    override fun onVideoColorChange(color: Int) {
        viewBinding.tabPL.setTabTextColors(color, color)
        val indicator =
            if (color == Color.WHITE) R.drawable.bg_tab_selected_light else R.drawable.bg_tab_selected
        viewBinding.tabPL.setSelectedTabIndicator(indicator)
        viewBinding.tabPL.setSelectedTabIndicatorColor(color)
        viewBinding.textCount.setTextColor(color)
        adapter.color = color
        adapter.notifyDataSetChanged()
    }

    override fun onPLCreated() {
    }

    override fun onRecentClick(item: String) {
        currentPlayingSongIndex = viewModel.videosLiveData.value!!.indexOf(item)
        currentPlayingTabIndex = viewBinding.tabPL.selectedTabPosition
        adapter.isCurrentPL = true
        adapter.currentPlayingPosition = currentPlayingSongIndex
        adapter.notifyDataSetChanged()
        onRecentClickListener?.let { it(item, viewModel.videosLiveData.value!!) }
    }

//    override fun onAdded(videoIds: List<String>) {
//        val pos = viewBinding.tabPL.selectedTabPosition
//        viewModel.addVideosToPL(pos - 1, videoIds)
//        if (currentPlayingTabIndex == pos) {
//            currentPlayingSongIndex = -1
//            adapter.currentPlayingPosition = currentPlayingSongIndex
//            adapter.notifyDataSetChanged()
//            onStopPlaying?.let { it() }
//        }
//    }

    override fun onNewTrackPlay(index: Int) {
        currentPlayingSongIndex = index
        adapter.currentPlayingPosition = currentPlayingSongIndex
        adapter.notifyDataSetChanged()
    }

//    override fun onPLRename(title: String) {
//        val pos = viewBinding.tabPL.selectedTabPosition
//        viewBinding.tabPL.getTabAt(pos)?.text = title
//        val playList = viewModel.playLists.value!![pos - 1]
//        playList.title = title
//        viewModel.updatePL(playList)
//    }

    private fun setupViews() = with(viewBinding) {
        recyclerList.adapter = adapter
        recyclerList.setHasFixedSize(true)
        layoutRecentRefresh.isEnabled = false
        tabPL.tabRippleColor = null
        viewModel.getAllPLs()
    }

    private fun observeData() = with(viewModel) {
        videosLiveData.observe(viewLifecycleOwner, Observer {
            viewBinding.textCount.text = "Videos: ${it.size}"
            adapter.list = it.toMutableList()
            adapter.notifyDataSetChanged()
            viewBinding.recyclerList.scrollToPosition(0)
            if (it.isNotEmpty()) {
                viewBinding.apply {
                    if (isRecent) {
                        textCount.gone()
                    } else {
                        textCount.show()
                    }
                }
            }
        })
        allVideoAbum.observe(viewLifecycleOwner, Observer(::setupTabs))
    }

    private fun setupTabs(pls: List<String>) = with(viewBinding.tabPL) {
        removeAllTabs()
        pls.forEach {
            val tab = newTab().apply {
                text = it
            }
            addTab(tab)
        }
        if (isFirstTime) {
            getTabAt(tabCount - 1)?.select()
            Handler(Looper.getMainLooper()).postDelayed(
                { getTabAt(tabCount - 1)?.select() }, 100
            )
        } else {
            getTabAt(0)?.select()
        }
        isFirstTime = true
    }

    private fun handleEvents() = with(viewBinding) {
        layoutRecentRefresh.setOnRefreshListener {
            resetPosition = false
            viewModel.getAllPLs()
        }
        tabPL.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                isRecent = pos == 0
                if (pos != 0) textCount.show()
                viewModel.getVideosByPL(pos)
                onTabChange?.let { it(pos) }
                adapter.isCurrentPL =
                    viewBinding.tabPL.selectedTabPosition == currentPlayingTabIndex
                adapter.currentPlayingPosition = currentPlayingSongIndex
                adapter.notifyDataSetChanged()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
        })
    }

    companion object {
        fun newInstance(
            onRecentClick: ((String, List<String>) -> Unit),
            onTabChange: (Int) -> Unit,
            onStopPlaying: () -> Unit
        ) =
            RecentFragment().apply {
                this.onRecentClickListener = onRecentClick
                this.onTabChange = onTabChange
                this.onStopPlaying = onStopPlaying
            }
    }
}
