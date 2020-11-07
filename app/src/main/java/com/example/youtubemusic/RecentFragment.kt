package com.example.youtubemusic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.youtubemusic.data.AppDatabase
import com.example.youtubemusic.data.VideoRepoImp
import com.example.youtubemusic.databinding.FragmentListBinding

class RecentFragment : Fragment(), OnNewVideoPlay, RecentAdapter.OnRecentClick {

    private val adapter by lazy { RecentAdapter(this) }

    private lateinit var viewBinding: FragmentListBinding

    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(requireContext()).videoDao()) }

    private val viewModel by lazy { RecentViewModel(videoRepo) }

    private var onRecentClickListener: ((String) -> Unit)? = null

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

    override fun onNewVideoAdded() {
        viewModel.getAllRecent()
    }

    override fun onVideoColorChange(color: Int) {
        viewBinding.textTitle.setTextColor(color)
        adapter.color = color
        adapter.list = emptyList()
        adapter.notifyDataSetChanged()
        adapter.list = viewModel.recent.value ?: return
        adapter.notifyDataSetChanged()
    }

    override fun onRecentClick(id: String) {
        onRecentClickListener?.let { it(id) }
    }

    private fun setupViews() = with(viewBinding) {
        recyclerList.adapter = adapter
        viewModel.getAllRecent()
    }

    private fun observeData() = with(viewModel) {
        recent.observe(viewLifecycleOwner, {
            adapter.list = it
            adapter.notifyDataSetChanged()
            viewBinding.layoutRecentRefresh.isRefreshing = false
            viewBinding.textTitle.text = "Recent(${it.size})"
        })
    }

    private fun handleEvents() = with(viewBinding) {
        layoutRecentRefresh.setOnRefreshListener {
            viewModel.getAllRecent()
        }
    }

    companion object {
        fun newInstance(onRecentClick: (String) -> Unit) = RecentFragment().apply {
            this.onRecentClickListener = onRecentClick
        }
    }
}
