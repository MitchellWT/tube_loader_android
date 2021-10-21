package com.mitchelltsutsulis.tube_loader.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoDownloadedAdapter
import com.mitchelltsutsulis.tube_loader.model.Thumbnail
import com.mitchelltsutsulis.tube_loader.model.Video
import okhttp3.*
import org.json.JSONArray
import org.json.JSONTokener
import java.io.IOException

class DownloadedFragment : Fragment() {
    private lateinit var  loadingSpinner: ProgressBar
    private val httpClient = OkHttpClient()
    private lateinit var videoAdapter: VideoDownloadedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.downloaded_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        loadingSpinner = view?.findViewById(R.id.loading_spinner)!!
        getDownloaded()
    }

    private fun getDownloaded() {
        loadingSpinner.visibility = View.VISIBLE
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("videos")
            .appendPath("multiple")
            .appendQueryParameter("amount", "50")
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
                requireActivity().runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                }
            }

                override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val downloadedResults = jsonConversion(res.body!!.string())
                    requireActivity().runOnUiThread {
                        loadingSpinner.visibility = View.GONE
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
                val backendId = itemsJsonArray.getJSONObject(i).getInt("id")
                val thumbnailUrl = itemsJsonArray.getJSONObject(i).getString("thumbnail")
                //val url = URL(thumbnailUrl)

                downloadedResults.add(
                    Video(videoId, title,
                    Thumbnail(thumbnailUrl),
                    downloaded = downloaded,
                    backendId = backendId)
                )

                //if (!((activity?.application as App).checkBitmap(videoId))) {
                //    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                //   (activity?.application as App).storeBitmap(videoId, bitmap)
                //}
            }
        }

        return downloadedResults
    }

    private fun updateRecycler(downloadedResults: List<Video>) {
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.delete_recycler)
        val layoutManager = LinearLayoutManager(context)
        videoAdapter = VideoDownloadedAdapter(downloadedResults as MutableList<Video>, activity?.application) {deleteVideo(it)}

        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    private fun deleteVideo(item: Video) {
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("videos")
        val deleteUrl = urlBuilder.build().toString()

        val requestBody = FormBody.Builder()
            .add("id", item.backendId.toString())
            .build()

        val request = Request.Builder()
            .method("DELETE",  requestBody)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(deleteUrl)
            .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("DELETE REQUEST FAILED", e.printStackTrace().toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val itemIndex = videoAdapter.getItemIndex(item)

                    videoAdapter.removeItem(itemIndex)
                    requireActivity().runOnUiThread {
                        videoAdapter.notifyItemRemoved(itemIndex)
                    }

                    activity?.let {
                        Snackbar.make(
                            it.findViewById(R.id.downloaded_fragment),
                            item.title + " has been deleted!",
                            Snackbar.LENGTH_SHORT
                        ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                    }
                }
            }
        })
    }
}