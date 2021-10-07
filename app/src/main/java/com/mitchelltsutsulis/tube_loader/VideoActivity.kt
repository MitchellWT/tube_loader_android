package com.mitchelltsutsulis.tube_loader

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VideoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val thumbnail = findViewById<ImageView>(R.id.thumbnail)
        val title = findViewById<TextView>(R.id.title)
        val videoId = findViewById<TextView>(R.id.videoId)
        val video = intent.getParcelableExtra<Video>("video")

        video?.let {
            title.text = it.title
            videoId.text = it.videoId
            (this.application as App).loadBitmap(it.videoId, thumbnail)
        }
    }
}