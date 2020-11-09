package com.example.youtubemusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.youtubemusic.data.AppDatabase
import com.example.youtubemusic.data.Video
import com.example.youtubemusic.data.VideoRepoImp
import com.example.youtubemusic.databinding.FragmentListBinding

class RecentFragment : Fragment(), OnNewVideoPlay, RecentAdapter.OnRecentClick {

    private val adapter by lazy { RecentAdapter(this) }

    private lateinit var viewBinding: FragmentListBinding

    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(requireContext()).videoDao()) }

    private val viewModel by lazy { RecentViewModel(videoRepo) }

    private var onRecentClickListener: ((Video) -> Unit)? = null

    private var resetPosition = false

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
        viewModel.getAllRecent()
    }

    override fun onVideoColorChange(color: Int) {
        viewBinding.textTitle.setTextColor(color)
        adapter.color = color
        adapter.notifyDataSetChanged()
    }

    override fun onRecentDelete(position: Int) {
        viewModel.deleteRecent(position)
    }

    override fun onRecentClick(item: Video) {
        onRecentClickListener?.let { it(item) }
    }

    private fun setupViews() = with(viewBinding) {
        recyclerList.adapter = adapter
        layoutRecentRefresh.isEnabled = false
        viewModel.getAllRecent()
    }

    private fun observeData() = with(viewModel) {
        recent.observe(viewLifecycleOwner, {
            adapter.list = it.toMutableList()
            if (resetPosition) adapter.currentPlayingPosition = 0
            adapter.notifyDataSetChanged()
            viewBinding.layoutRecentRefresh.isRefreshing = false
            viewBinding.textTitle.text = "Recent(${it.size})"
        })
    }

    private fun handleEvents() = with(viewBinding) {
        layoutRecentRefresh.setOnRefreshListener {
            resetPosition = false
            viewModel.getAllRecent()
        }
    }

    companion object {
        fun newInstance(onRecentClick: (Video) -> Unit) = RecentFragment().apply {
            this.onRecentClickListener = onRecentClick
        }
    }
}
