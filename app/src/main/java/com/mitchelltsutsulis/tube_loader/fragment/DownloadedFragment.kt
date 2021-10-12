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
import okhttp3.*
import org.json.JSONArray
import org.json.JSONTokener
import java.io.IOException
import java.net.URL

class DownloadedFragment : Fragment() {
    private val httpClient = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.downloaded_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        getDownloaded()
    }

    private fun getDownloaded() {
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
                Log.i("GET DOWNLOADED REQUEST FAILED", e.printStackTrace().toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val downloadedResults = jsonConversion(res.body!!.string())
                    requireActivity().runOnUiThread {
                        updateRecycler(downloadedResults)
                    }
                }
            }
        })
    }

    private fun jsonConversion(jsonString: String): List<Video> {
        val downloadedResults = mutableListOf<Video>()
        val itemsJsonArray = JSONTokener(jsonString).nextValue() as JSONArray

        for (i in 0 until itemsJsonArray.length()) {
            val downloaded = itemsJsonArray.getJSONObject(i).getBoolean("downloaded")
            val queued = itemsJsonArray.getJSONObject(i).getBoolean("queued")

            if (downloaded && !queued) {
                val videoId = itemsJsonArray.getJSONObject(i).getString("video_id")
                val title = itemsJsonArray.getJSONObject(i).getString("title")
                val thumbnailUrl = itemsJsonArray.getJSONObject(i).getString("thumbnail")
                val url = URL(thumbnailUrl)

                downloadedResults.add(Video(videoId, title,
                    Thumbnail(thumbnailUrl), queued, downloaded))

                if (!((activity?.application as App).checkBitmap(videoId))) {
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    (activity?.application as App).storeBitmap(videoId, bitmap)
                }
            }
        }

        return downloadedResults
    }

    private fun updateRecycler(downloadedResults: List<Video>) {
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.delete_recycler)
        val layoutManager = LinearLayoutManager(context)
        val videoAdapter = VideoDownloadedAdapter(downloadedResults, activity?.application) {deleteVideo(it)}

        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    private fun deleteVideo(item: Video) {
        Log.i("TESTINGO", "DELETE")
    }
}