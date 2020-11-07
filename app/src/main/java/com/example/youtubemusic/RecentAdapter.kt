package com.example.youtubemusic

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.youtubemusic.data.Video
import com.example.youtubemusic.databinding.ItemVideoBinding

class RecentAdapter(private val onRecentClick: OnRecentClick?) :
    RecyclerView.Adapter<RecentAdapter.RecentVH>() {

    var list = listOf<Video>()

    var color: Int = Color.WHITE

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: RecentVH, position: Int) {
        holder.bind(list[position], color)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentVH {
        val itemViewBinding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentVH(itemViewBinding, onRecentClick)
    }

    class RecentVH(
        private val itemViewBinding: ItemVideoBinding,
        private val onRecentClick: OnRecentClick?
    ) :
        RecyclerView.ViewHolder(itemViewBinding.root) {

        private var id = ""

        init {
            itemView.setOnClickListener {
                onRecentClick?.onRecentClick(id)
            }
        }

        fun bind(itemData: Video, color: Int) = with(itemViewBinding) {
            Glide.with(itemView.context)
                .load(itemData.thumbnail)
                .into(imageThumbnail)
            textTitle.text = itemData.title
            textTitle.setTextColor(color)
            this@RecentVH.id = itemData.id
        }
    }

    interface OnRecentClick {
        fun onRecentClick(id: String)
    }
}
