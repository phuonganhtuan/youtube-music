package com.example.youtubemusic

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.example.youtubemusic.data.Video
import com.example.youtubemusic.databinding.ItemSwipeableVideoBinding
import com.example.youtubemusic.utils.gone
import com.example.youtubemusic.utils.show


class RecentAdapter(private val onRecentClick: OnRecentClick?) :
    RecyclerView.Adapter<RecentAdapter.RecentVH>() {

    var list = mutableListOf<Video>()

    var color: Int = Color.WHITE

    private val viewBinderHelper = ViewBinderHelper()

    override fun getItemCount() = list.size

    var currentPlayingPosition = 0

    override fun onBindViewHolder(holder: RecentVH, position: Int) {
        viewBinderHelper.setOpenOnlyOne(true)
        viewBinderHelper.bind(
            holder.itemView.findViewById(R.id.swipeLayout), list[position].id
        )
        viewBinderHelper.closeLayout(list[position].id)
        holder.bind(list[position], color, currentPlayingPosition)
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
        currentPlayingPosition = position
        notifyDataSetChanged()
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

        fun bind(itemData: Video, color: Int, position: Int) = with(itemViewBinding) {
            Glide.with(itemView.context)
                .load(itemData.thumbnail)
                .into(imageThumbnail)
            textTitle.text = itemData.title
            textTitle.setTextColor(color)
            if (position == adapterPosition) viewBg.show() else viewBg.gone()
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
