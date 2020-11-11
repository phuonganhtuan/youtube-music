package com.example.youtubemusic.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.youtubemusic.data.entity.Video
import com.example.youtubemusic.databinding.ItemVideoSelectableBinding


class AddVideoAdapter(private val onItemSelected: OnItemSelected?) :
    RecyclerView.Adapter<AddVideoAdapter.SelectableVideoVH>() {

    var list = listOf<Video>()

    var existList = listOf<String>()

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: SelectableVideoVH, position: Int) {
        holder.bind(list[position], existList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableVideoVH {
        val itemViewBinding = ItemVideoSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectableVideoVH(itemViewBinding, onItemSelected)
    }

    class SelectableVideoVH(
        private val itemViewBinding: ItemVideoSelectableBinding,
        private val onItemSelected: OnItemSelected?
    ) :
        RecyclerView.ViewHolder(itemViewBinding.root) {

        private var id = ""

        init {
            itemViewBinding.checkVideo.setOnClickListener {
                if (itemViewBinding.checkVideo.isChecked) {
                    onItemSelected?.onAdd(id)
                } else {
                    onItemSelected?.onDelete(id)
                }
            }
        }

        fun bind(itemData: Video, existList: List<String>) = with(itemViewBinding) {
            Glide.with(itemView.context)
                .load(itemData.thumbnail)
                .into(imageThumbnail)
            textTitle.text = itemData.title
            id = itemData.id
            itemViewBinding.checkVideo.isChecked = existList.contains(itemData.id)
        }
    }

    interface OnItemSelected {
        fun onAdd(id: String)
        fun onDelete(id: String)
    }
}
