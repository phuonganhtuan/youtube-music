package com.example.videomusic.ui.playlist

import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.videomusic.R
import com.example.videomusic.databinding.ItemSwipeableVideoBinding
import com.example.videomusic.utils.gone
import com.example.videomusic.utils.show
import java.io.File


class RecentAdapter(private val onRecentClick: OnRecentClick?) :
    RecyclerView.Adapter<RecentAdapter.RecentVH>() {

    var list = mutableListOf<String>()

    var color: Int = Color.WHITE

    override fun getItemCount() = list.size

    var currentPlayingPosition = -1

    var isCurrentPL = false

    override fun onBindViewHolder(holder: RecentVH, position: Int) {
        holder.bind(list[position], color, currentPlayingPosition, isCurrentPL)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentVH {
        val itemViewBinding = ItemSwipeableVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentVH(itemViewBinding, ::onItemRecentClick)
    }

    private fun onItemRecentClick(position: Int) {
        onRecentClick?.onRecentClick(list[position])
    }

    class RecentVH(
        private val itemViewBinding: ItemSwipeableVideoBinding,
        private val onRecentClick: (Int) -> Unit
    ) :
        RecyclerView.ViewHolder(itemViewBinding.root) {

        init {
            itemViewBinding.viewTouch.setOnClickListener {
                onRecentClick(adapterPosition)
            }
        }

        fun bind(itemData: String, color: Int, currentPos: Int, flag: Boolean) =
            with(itemViewBinding) {
                Glide
                    .with(itemView.context)
                    .asBitmap()
                    .load(Uri.fromFile(File(itemData)))
                    .into(imageThumbnail)
                textTitle.text = itemData
                textTitle.setTextColor(color)
                if (flag && currentPos == adapterPosition) {
                    if (color == Color.WHITE) viewBgLight.show() else viewBg.show()
                } else {
                    viewBg.gone()
                    viewBgLight.gone()
                }
            }
    }

    interface OnRecentClick {
        fun onRecentClick(item: String)
    }
}
