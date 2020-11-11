package com.example.youtubemusic.ui.playlist

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.youtubemusic.R
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.databinding.ItemSwipeableVideoBinding
import com.example.youtubemusic.utils.gone
import com.example.youtubemusic.utils.show


class RecentAdapter(private val onRecentClick: OnRecentClick?) :
    RecyclerView.Adapter<RecentAdapter.RecentVH>() {

    var list = mutableListOf<Video>()

    var color: Int = Color.WHITE

    private val viewBinderHelper = ViewBinderHelper()

    override fun getItemCount() = list.size

    var currentPlayingPosition = -1

    var isCurrentPL = false

    override fun onBindViewHolder(holder: RecentVH, position: Int) {
        viewBinderHelper.setOpenOnlyOne(true)
        viewBinderHelper.bind(
            holder.itemView.findViewById(R.id.swipeLayout), list[position].id
        )
        viewBinderHelper.closeLayout(list[position].id)
        holder.bind(list[position], color, currentPlayingPosition, isCurrentPL)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentVH {
        val itemViewBinding = ItemSwipeableVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentVH(itemViewBinding, ::onItemRecentClick, ::deleteItem)
    }

    private fun deleteItem(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
        onRecentClick?.onRecentDelete(position)
    }

    private fun onItemRecentClick(position: Int) {
        onRecentClick?.onRecentClick(list[position])
    }

    class RecentVH(
        private val itemViewBinding: ItemSwipeableVideoBinding,
        private val onRecentClick: (Int) -> Unit,
        private val deleteItem: (Int) -> Unit
    ) :
        RecyclerView.ViewHolder(itemViewBinding.root) {

        init {
            itemViewBinding.viewTouch.setOnClickListener {
                onRecentClick(adapterPosition)
            }
            itemViewBinding.imageDelete.setOnClickListener {
                deleteItem(adapterPosition)
            }
        }

        fun bind(itemData: Video, color: Int, currentPos: Int, flag: Boolean) =
            with(itemViewBinding) {
                Glide.with(itemView.context)
                    .load(itemData.thumbnail)
                    .into(imageThumbnail)
                textTitle.text = itemData.title
                textTitle.setTextColor(color)
                if (flag && currentPos == adapterPosition) {
                    if (color == Color.WHITE) viewBgLight.show() else viewBg.show()
                } else {
                    viewBg.gone()
                    viewBgLight.gone()
                }
                imageDelete.setColorFilter(
                    color,
                    PorterDuff.Mode.SRC_ATOP
                )
            }
    }

    interface OnRecentClick {
        fun onRecentClick(item: Video)
        fun onRecentDelete(position: Int)
    }
}
