package com.mitchelltsutsulis.tube_loader.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.mitchelltsutsulis.tube_loader.App
import com.mitchelltsutsulis.tube_loader.R
import com.mitchelltsutsulis.tube_loader.model.Video
import com.squareup.picasso.Picasso

class VideoQueueAdapter(private val data: MutableList<Video>,
                        private val context: Context?,
                        private val listener: (Video) -> Unit): RecyclerView.Adapter<VideoQueueAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.video_queue_layout, parent, false) as View

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item)
    }

    fun getItemIndex(item: Video) = data.indexOf(item)

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private val queued: Button = view.findViewById(R.id.queue_button)
        private val title: TextView = view.findViewById(R.id.title)
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        private val buttonText = MutableLiveData(context?.getString(R.string.queued_button))

        fun bind(item: Video) {
            buttonText.value = if (!item.queued) context?.getString(R.string.un_queued_button)
            else context?.getString(R.string.queued_button)

            queued.text = buttonText.value
            title.text = item.title
            Picasso.get().load(item.thumbnail.source)
                .placeholder(R.drawable.ic_black)
                .error(R.drawable.thumbnail_not_found)
                .into(thumbnail)
            //(context as App).loadBitmap(item.videoId, thumbnail)

            queued.setOnClickListener {
                listener(item)
            }
        }
    }
}
