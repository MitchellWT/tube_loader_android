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
import com.mitchelltsutsulis.tube_loader.R
import com.mitchelltsutsulis.tube_loader.model.Video
import com.squareup.picasso.Picasso

class VideoQueueAdapter(
    private val data: MutableList<Video>,
    private val context: Context,
    private val listener: (Video) -> Unit
) : RecyclerView.Adapter<VideoQueueAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.video_queue_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    fun getItemIndex(item: Video) = data.indexOf(item)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val queued = view.findViewById<Button>(R.id.queue_button)
        private val title = view.findViewById<TextView>(R.id.title)
        private val thumbnail = view.findViewById<ImageView>(R.id.thumbnail)
        private val buttonText = MutableLiveData(context.getString(R.string.queued_button))

        fun bind(item: Video) {
            title.text = item.title
            buttonText.value = context.getString(
                if (item.queued) R.string.queued_button else R.string.un_queued_button
            )
            queued.text = buttonText.value
            Picasso.get().load(item.thumbnail.source)
                .placeholder(R.drawable.ic_black)
                .error(R.drawable.thumbnail_not_found)
                .into(thumbnail)
            queued.setOnClickListener { listener(item) }
        }
    }
}
