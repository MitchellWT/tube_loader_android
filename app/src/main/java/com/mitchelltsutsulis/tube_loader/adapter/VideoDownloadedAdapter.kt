package com.mitchelltsutsulis.tube_loader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mitchelltsutsulis.tube_loader.R
import com.mitchelltsutsulis.tube_loader.model.Video
import com.squareup.picasso.Picasso

class VideoDownloadedAdapter(
    private val data: MutableList<Video>,
    private val listener: (Video) -> Unit
) : RecyclerView.Adapter<VideoDownloadedAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.video_downloaded_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    fun getItemIndex(item: Video) = data.indexOf(item)

    fun removeItem(index: Int) = data.removeAt(index)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val delete = view.findViewById<Button>(R.id.delete_button)
        private val title = view.findViewById<TextView>(R.id.title)
        private val thumbnail = view.findViewById<ImageView>(R.id.thumbnail)

        fun bind(item: Video) {
            title.text = item.title
            Picasso.get().load(item.thumbnail.source)
                .placeholder(R.drawable.ic_black)
                .error(R.drawable.thumbnail_not_found)
                .into(thumbnail)
            delete.setOnClickListener { listener(item) }
        }
    }
}
