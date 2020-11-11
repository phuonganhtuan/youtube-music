package com.example.youtubemusic.ui.playlist

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.youtubemusic.R
import com.example.youtubemusic.data.db.AppDatabase
import com.example.youtubemusic.data.entity.PlayList
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.data.repo.PlayListRepoImp
import com.example.youtubemusic.data.repo.VideoRepoImp
import com.example.youtubemusic.databinding.FragmentListBinding
import com.example.youtubemusic.ui.main.OnNewVideoPlay
import com.example.youtubemusic.utils.gone
import com.example.youtubemusic.utils.show
import com.google.android.material.tabs.TabLayout

class RecentFragment : Fragment(), OnNewVideoPlay, RecentAdapter.OnRecentClick,
    AddVideoFragment.OnVideoAdded {

    private val adapter by lazy { RecentAdapter(this) }
    private lateinit var viewBinding: FragmentListBinding
    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(requireContext()).videoDao()) }
    private val playListRepo by lazy {
        PlayListRepoImp(
            AppDatabase.invoke(requireContext()).playListDao()
        )
    }
    private val viewModel by lazy { RecentViewModel(videoRepo, playListRepo) }
    private var onRecentClickListener: ((Video, List<Video>) -> Unit)? = null
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
    ): View? {
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
        resetPosition = isReset
        viewBinding.tabPL.apply {
            Handler(Looper.getMainLooper()).postDelayed(
                { getTabAt(0)?.select() }, 100
            )
        }
    }

    override fun onVideoColorChange(color: Int) {
        viewBinding.tabPL.setTabTextColors(color, color)
        val indicator =
            if (color == Color.WHITE) R.drawable.bg_tab_selected_light else R.drawable.bg_tab_selected
        viewBinding.tabPL.setSelectedTabIndicator(indicator)
        viewBinding.tabPL.setSelectedTabIndicatorColor(color)
        viewBinding.imageAddVideo.setColorFilter(
            color,
            PorterDuff.Mode.SRC_ATOP
        )
        viewBinding.imageOptions.setColorFilter(
            color,
            PorterDuff.Mode.SRC_ATOP
        )
        viewBinding.textCount.setTextColor(color)
        adapter.color = color
        adapter.notifyDataSetChanged()
    }

    override fun onRecentDelete(position: Int) {
        viewModel.deleteRecent(position)
    }

    override fun onRecentClick(item: Video) {
        currentPlayingSongIndex = viewModel.recent.value!!.indexOf(item)
        currentPlayingTabIndex = viewBinding.tabPL.selectedTabPosition
        adapter.isCurrentPL = true
        adapter.currentPlayingPosition = currentPlayingSongIndex
        adapter.notifyDataSetChanged()
        onRecentClickListener?.let { it(item, viewModel.recent.value!!) }
    }

    override fun onPLCreated() {
        viewModel.getAllPLs()
    }

    override fun onAdded(videoIds: List<String>) {
        val pos = viewBinding.tabPL.selectedTabPosition
        viewModel.addVideosToPL(pos - 1, videoIds)
        if (currentPlayingTabIndex == pos) {
            currentPlayingSongIndex = -1
            adapter.currentPlayingPosition = currentPlayingSongIndex
            adapter.notifyDataSetChanged()
            onStopPlaying?.let { it() }
        }
    }

    override fun onNewTrackPlay(index: Int) {
        currentPlayingSongIndex = index
        adapter.currentPlayingPosition = currentPlayingSongIndex
        adapter.notifyDataSetChanged()
    }

    private fun setupViews() = with(viewBinding) {
        recyclerList.adapter = adapter
        layoutRecentRefresh.isEnabled = false
        tabPL.tabRippleColor = null
        viewModel.getAllRecent()
        viewModel.getAllPLs()
    }

    private fun observeData() = with(viewModel) {
        recent.observe(viewLifecycleOwner, {
            viewBinding.textCount.text = "Videos: ${it.size}"
            adapter.list = it.toMutableList()
            adapter.notifyDataSetChanged()
            viewBinding.recyclerList.scrollToPosition(0)
            if (it.isEmpty()) {
                viewBinding.apply {
                    buttonCreateEmpty.show()
                    cardAddVideos.gone()
                    cardOptions.show()
                }
            } else {
                viewBinding.apply {
                    buttonCreateEmpty.gone()
                    if (isRecent) {
                        cardAddVideos.gone()
                        cardOptions.gone()
                        textCount.gone()
                    } else {
                        cardAddVideos.show()
                        cardOptions.show()
                        textCount.show()
                    }
                }
            }
        })
        playLists.observe(viewLifecycleOwner, (::setupTabs))
    }

    private fun setupTabs(pls: List<PlayList>) = with(viewBinding.tabPL) {
        removeAllTabs()
        addTab(newTab().apply {
            text = "Recent"
        })
        pls.forEach {
            val tab = newTab().apply {
                text = it.title
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
            viewModel.getAllRecent()
        }
        tabPL.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val pos = tab?.position ?: 0
                isRecent = pos == 0
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
        buttonCreateEmpty.setOnClickListener {
            val currentTab = tabPL.getTabAt(tabPL.selectedTabPosition)
            currentTab ?: return@setOnClickListener
            val title = currentTab.text.toString()
            activity?.supportFragmentManager?.let {
                AddVideoFragment.newInstance(this@RecentFragment, title)
                    .show(it, AddVideoFragment::class.java.simpleName)
            }
        }
        imageAddVideo.setOnClickListener {
            val currentTab = tabPL.getTabAt(tabPL.selectedTabPosition)
            currentTab ?: return@setOnClickListener
            val title = currentTab.text.toString()
            activity?.supportFragmentManager?.let {
                AddVideoFragment.newInstance(this@RecentFragment, title, viewModel.recent.value!!)
                    .show(it, AddVideoFragment::class.java.simpleName)
            }
        }
    }

    companion object {
        fun newInstance(
            onRecentClick: (Video, List<Video>) -> Unit,
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
