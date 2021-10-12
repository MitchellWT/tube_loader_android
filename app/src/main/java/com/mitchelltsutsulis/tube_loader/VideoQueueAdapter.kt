package com.mitchelltsutsulis.tube_loader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoQueueAdapter(private val data: List<Video>,
                         private val context: Context?,
                         private val listener: (Video) -> Unit): RecyclerView.Adapter<VideoQueueAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoQueueAdapter.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.video_queue_layout, parent, false) as View

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VideoQueueAdapter.ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private val queued: Button = view.findViewById(R.id.queue_button)
        private val title: TextView = view.findViewById(R.id.title)
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)

        fun bind(item: Video) {
            if (!item.queued) {
                queued.text = context?.getString(R.string.un_queued_button)
            }
            title.text = item.title
            (context as App).loadBitmap(item.videoId, thumbnail)

            queued.setOnClickListener {
                listener(item)
            }
        }
    }
}
