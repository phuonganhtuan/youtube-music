package com.example.youtubemusic.ui.main

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.youtubemusic.databinding.ActivityVideoBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class VideoFragment : BottomSheetDialogFragment() {

    private lateinit var viewBinding: ActivityVideoBinding

    private var service: MediaService? = null

    private var listener: OnVideoStateChange? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = ActivityVideoBinding.inflate(inflater)
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
            // When using AndroidX the resource can be found at com.google.android.material.R.id.design_bottom_sheet
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
        viewBinding.videoView.player = service?.exoPlayer
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener?.onStateChange()
    }

    override fun onStart() {
        super.onStart()
        val sheetContainer = requireView().parent as? ViewGroup ?: return
        sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        service?.let {
            if (it.scale > 1) {
                val params = viewBinding.videoView.layoutParams
                val screenMetrics = activity?.resources?.displayMetrics
                screenMetrics?.let { metrics ->
                    val height = metrics.heightPixels
                    val width = metrics.widthPixels
                    params.apply {
                        this.height = width
                        this.width = height
                    }
                    viewBinding.videoView.layoutParams = params
                }
            } else {
                viewBinding.videoView.rotation = 0f
            }
        }
    }

    interface OnVideoStateChange {
        fun onStateChange()
    }

    companion object {
        fun newInstance(service: MediaService?, listener: OnVideoStateChange) =
            VideoFragment().apply {
                this.service = service
                this.listener = listener
            }
    }
}
