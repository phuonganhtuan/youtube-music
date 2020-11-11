package com.example.youtubemusic.ui.playlist

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.youtubemusic.data.db.AppDatabase
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.data.repo.PlayListRepoImp
import com.example.youtubemusic.data.repo.VideoRepoImp
import com.example.youtubemusic.databinding.FragmentSelectVideosBinding
import com.example.youtubemusic.utils.gone
import com.example.youtubemusic.utils.show
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class AddVideoFragment : BottomSheetDialogFragment(), AddVideoAdapter.OnItemSelected {

    private val videoRepo by lazy { VideoRepoImp(AppDatabase.invoke(requireContext()).videoDao()) }

    private val playListRepo by lazy {
        PlayListRepoImp(
            AppDatabase.invoke(requireContext()).playListDao()
        )
    }

    private val viewModel by lazy { RecentViewModel(videoRepo, playListRepo) }

    private lateinit var viewBinding: FragmentSelectVideosBinding

    private var title = ""

    private var listener: OnVideoAdded? = null

    private val videoIds = mutableListOf<String>()

    private val adapter by lazy { AddVideoAdapter(this) }

    private var list = emptyList<Video>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentSelectVideosBinding.inflate(inflater)
        dialog?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        return viewBinding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener { dialog: DialogInterface ->
            val dialogc = dialog as BottomSheetDialog
            val bottomSheet =
                dialogc.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            val bottomSheetBehavior: BottomSheetBehavior<*> =
                BottomSheetBehavior.from(bottomSheet!!)
            bottomSheetBehavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
        return bottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        handleEvents()
        observeData()
    }

    override fun onStart() {
        super.onStart()
        val sheetContainer = requireView().parent as? ViewGroup ?: return
        sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onAdd(id: String) {
        videoIds.add(id)
        updateButton()
    }

    override fun onDelete(id: String) {
        videoIds.remove(id)
        updateButton()
    }

    private fun updateButton() = with(viewBinding) {
        buttonAdd.text = "Apply to $title (${videoIds.size})"
        buttonAdd.isEnabled = videoIds.isNotEmpty()
    }

    private fun setupViews() = with(viewBinding) {
        buttonAdd.isEnabled = false
        recyclerList.adapter = adapter
        viewModel.getAllRecent()
    }

    private fun handleEvents() = with(viewBinding) {
        buttonAdd.setOnClickListener {
            listener?.onAdded(videoIds)
            dialog?.cancel()
        }
    }

    private fun observeData() = with(viewModel) {
        recent.observe(viewLifecycleOwner, {
            val ids = list.map { vid -> vid.id }
            videoIds.addAll(ids)
            viewBinding.buttonAdd.text = "Apply to $title (${videoIds.size})"
            adapter.existList = ids
            adapter.list = it
            adapter.notifyDataSetChanged()
            if (it.isEmpty()) viewBinding.textEmpty.show() else viewBinding.textEmpty.gone()
        })
    }

    interface OnVideoAdded {
        fun onAdded(videoIds: List<String>)
    }

    companion object {
        fun newInstance(listener: OnVideoAdded, title: String, list: List<Video> = emptyList()) =
            AddVideoFragment().apply {
                this.listener = listener
                this.title = title
                this.list = list
            }
    }
}
