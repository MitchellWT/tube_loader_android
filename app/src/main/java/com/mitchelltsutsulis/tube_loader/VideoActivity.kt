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
import org.json.JSONObject
import java.io.IOException

class VideoActivity : AppCompatActivity() {
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        // Getting all view elements and the video from the intent
        val thumbnail = findViewById<ImageView>(R.id.thumbnail)
        val videoId   = findViewById<TextView>(R.id.videoId)
        val addButton = findViewById<Button>(R.id.add_button)
        val title     = findViewById<TextView>(R.id.title)
        val queued    = findViewById<CheckBox>(R.id.queued)
        val video     = intent.getParcelableExtra<Video>("video")

        // Only executed when video is not null
        video?.let {
            // Set view elements
            title.text = it.title
            videoId.text = it.videoId
            Picasso.get().load(it.thumbnail.source)
                .placeholder(R.drawable.ic_black)
                .error(R.drawable.ic_black)
                .into(thumbnail)

            // Old code
            // (this.application as App).loadBitmap(it.videoId, thumbnail)

            // Create listener for adding item to the system
            addButton.setOnClickListener {
                video.title = title.text.toString()
                video.queued = queued.isChecked
                addToSystem(video)
            }
        }
    }

    override fun onBackPressed() {
        // Response code of 300 shows no toast to user
        exit(300)
        super.onBackPressed()
    }

    // Only call function when video is not null
    private fun addToSystem(video: Video) {
        video.let {
            // URI for creating videos via the API
            val urlBuilder = Uri.Builder()
                .scheme("http")
                .encodedAuthority(getString(R.string.server_ip))
                .appendPath("video")
            val addUrl = urlBuilder.build().toString()
            // Request body with required key value pairs
            val requestBody = """{
                "video_id": "${video.videoId}",
                "title": "${video.title}",
                "thumbnail": "${video.thumbnail.source}",
                "queued": ${video.queued}
            }""".toRequestBody()
            // Bearer must be specified when using the API
            val request = Request.Builder()
                .method("POST", requestBody)
                .header("Authorization", "Bearer " + getString(R.string.api_token))
                .url(addUrl)
                .build()
            // API request with callback
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("ADD VIDEO FAILED", e.printStackTrace().toString())
                    // Contract response without response code will show a error
                    // toast to the user
                    exit()
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.i("ADD VIDEO RES", response.toString())
                    exit(response.code)
                }
            })
        }
    }

    // Sets result for contract and finishes activity
    private fun exit(status_code: Int = 400) {
        setResult(RESULT_OK, intent.putExtra("status_code", status_code))
        finish()
    }

    // Contract for activity
    class Contract: ActivityResultContract<Video, Int>() {
        override fun createIntent(context: Context, input: Video): Intent {
            return Intent(context, VideoActivity::class.java).apply {
                // Parcelable video object
                putExtra("video", input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            // Result response code for toast to user
            return intent?.getIntExtra("status_code", 400)!!
        }
    }
}