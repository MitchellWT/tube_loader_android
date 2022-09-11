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
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class VideoActivity : AppCompatActivity() {
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val thumbnail = findViewById<ImageView>(R.id.thumbnail)
        val videoId = findViewById<TextView>(R.id.videoId)
        val addButton = findViewById<Button>(R.id.add_button)
        val title = findViewById<TextView>(R.id.title)
        val queued = findViewById<CheckBox>(R.id.queued)
        val video = intent.getParcelableExtra<Video>("video") ?: return

        title.text = video.title
        videoId.text = video.videoId
        Picasso.get().load(video.thumbnail.source)
            .placeholder(R.drawable.ic_black)
            .error(R.drawable.ic_black)
            .into(thumbnail)
        addButton.setOnClickListener {
            video.title = title.text.toString()
            video.queued = queued.isChecked
            addToSystem(video)
        }
    }

    override fun onBackPressed() {
        exit(300)
        super.onBackPressed()
    }

    private fun addToSystem(video: Video) {
        val url = Uri.Builder()
            .scheme(getString(R.string.server_protocol))
            .encodedAuthority(getString(R.string.server_address))
            .appendPath("video")
            .build()
            .toString()
        val body = """{
            "video_id": "${video.videoId}",
            "title": "${video.title}",
            "thumbnail": "${video.thumbnail.source}",
            "queued": ${video.queued}
        }""".toRequestBody()
        val req = Request.Builder()
            .post(body)
            .url(url)
            .build()
        httpClient.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("ADD VIDEO FAIL", e.message.toString())
                exit()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i(
                    "ADD VIDEO RES",
                    "Status code: ${response.code}, message: ${response.message}"
                )
                exit(response.code)
            }
        })
    }

    private fun exit(statusCode: Int = 400) {
        setResult(RESULT_OK, intent.putExtra("statusCode", statusCode))
        finish()
    }

    class Contract : ActivityResultContract<Video, Int>() {
        override fun createIntent(context: Context, input: Video): Intent {
            return Intent(context, VideoActivity::class.java).apply {
                putExtra("video", input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            if (intent == null) return 400
            return intent.getIntExtra("statusCode", 400)
        }
    }
}
