package com.mitchelltsutsulis.tube_loader.fragment

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoQueueAdapter
import com.mitchelltsutsulis.tube_loader.model.Thumbnail
import com.mitchelltsutsulis.tube_loader.model.Video
import okhttp3.*
import org.json.JSONArray
import org.json.JSONTokener
import java.io.IOException
import java.net.URL

class QueueFragment : Fragment() {
    private val httpClient = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.queue_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        getQueue()
    }

    private fun getQueue() {
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("videos")
            .appendPath("multiple")
            .appendQueryParameter("amount", "10")
            .appendQueryParameter("offset", "0")
        val showUrl = urlBuilder.build().toString()

        val request = Request.Builder()
            .method("GET", null)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(showUrl)
            .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET QUEUE REQUEST FAILED", e.printStackTrace().toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val queueResults = jsonConversion(res.body!!.string())
                    requireActivity().runOnUiThread {
                        updateRecycler(queueResults)
                    }
                }
            }
        })
    }

    private fun jsonConversion(jsonString: String): List<Video> {
        val queueResults = mutableListOf<Video>()
        val itemsJsonArray = JSONTokener(jsonString).nextValue() as JSONArray

        Log.i("TESTINGO", jsonString);

        for (i in 0 until itemsJsonArray.length()) {
            val downloaded = itemsJsonArray.getJSONObject(i).getBoolean("downloaded")

            if (!downloaded) {
                val videoId = itemsJsonArray.getJSONObject(i).getString("video_id")
                val title = itemsJsonArray.getJSONObject(i).getString("title")
                val queued = itemsJsonArray.getJSONObject(i).getBoolean("queued")
                val backendId = itemsJsonArray.getJSONObject(i).getInt("id")
                val thumbnailUrl = itemsJsonArray.getJSONObject(i).getString("thumbnail")
                val url = URL(thumbnailUrl)

                queueResults.add(
                    Video(videoId, title,
                    Thumbnail(thumbnailUrl),
                    queued,
                    backendId = backendId)
                )

                if (!((activity?.application as App).checkBitmap(videoId))) {
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    (activity?.application as App).storeBitmap(videoId, bitmap)
                }
            }
        }

        return queueResults
    }

    private fun updateRecycler(queueResults: List<Video>) {
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.queue_recycler)
        val layoutManager = LinearLayoutManager(context)
        val videoAdapter = VideoQueueAdapter(queueResults, activity?.application) {changeQueuedState(it)}

        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    private fun changeQueuedState(item: Video) {
        Log.i("TESTINGO", "PRESSED")
    }
}