package com.mitchelltsutsulis.tube_loader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import com.mitchelltsutsulis.tube_loader.model.Video
import com.squareup.picasso.Picasso
import okhttp3.*
import java.io.IOException

class VideoActivity : AppCompatActivity() {
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val thumbnail = findViewById<ImageView>(R.id.thumbnail)
        val title = findViewById<TextView>(R.id.title)
        val videoId = findViewById<TextView>(R.id.videoId)
        val queued = findViewById<CheckBox>(R.id.queued)
        val addButton = findViewById<Button>(R.id.addButton)
        val video = intent.getParcelableExtra<Video>("video")

        video?.let {
            title.text = it.title
            videoId.text = it.videoId
            Picasso.get().load(it.thumbnail.source)
                .placeholder(R.drawable.ic_black)
                .error(R.drawable.ic_black)
                .into(thumbnail)
            //(this.application as App).loadBitmap(it.videoId, thumbnail)
        }

        addButton.setOnClickListener {
            val urlBuilder = Uri.Builder()
                .scheme("http")
                .encodedAuthority(getString(R.string.server_ip))
                .appendPath("api")
                .appendPath("videos")
            val addUrl = urlBuilder.build().toString()

            val requestBody = FormBody.Builder()
                .add("video_id", video?.videoId.toString())
                .add("title", title.text.toString())
                .add("thumbnail", video?.thumbnail?.source.toString())
                .add("queued", (if (queued.isChecked) 1 else 0).toString())
                .build()

            val request = Request.Builder()
                .method("POST", requestBody)
                .header("Authorization", "Bearer " + getString(R.string.api_token))
                .url(addUrl)
                .build()

            httpClient.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("ADD VIDEO FAILED", e.printStackTrace().toString())
                    exit()
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i("ADD VIDEO RES", response.toString())
                    exit(response.code)
                }
            })
        }
    }

    override fun onBackPressed() {
        exit(300)
        super.onBackPressed()
    }

    private fun exit(status_code: Int = 400) {
        setResult(RESULT_OK, intent.putExtra("status_code", status_code))
        finish()
    }

    // Contract for activity
    class Contract: ActivityResultContract<Video, Int>() {
        override fun createIntent(context: Context, input: Video?): Intent {
            return Intent(context, VideoActivity::class.java).apply {
                putExtra("video", input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            return intent?.getIntExtra("status_code", 400)!!
        }
    }
}