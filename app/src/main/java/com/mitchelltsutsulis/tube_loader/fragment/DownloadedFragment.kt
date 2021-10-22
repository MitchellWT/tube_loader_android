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
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var videoAdapter: VideoDownloadedAdapter
    private val httpClient = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.downloaded_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        // Gets loading spinner
        loadingSpinner = view?.findViewById(R.id.loading_spinner)!!
        getDownloaded()
    }

    private fun getDownloaded() {
        // Makes loading spinner visible
        loadingSpinner.visibility = View.VISIBLE
        // URI for getting videos via API
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("videos")
            .appendPath("multiple")
            .appendQueryParameter("amount", "50")
            .appendQueryParameter("offset", "0")
        val showUrl = urlBuilder.build().toString()
        // GET method does not require a request body, data is passed as query parameters
        // in the URI
        val request = Request.Builder()
            .method("GET", null)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(showUrl)
            .build()
        // API request with callback
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET DOWNLOADED REQUEST FAILED", e.printStackTrace().toString())
                // Removes loading spinner
                requireActivity().runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                }
                // Presents toast to use describing the failure
                activity?.let {
                    Snackbar.make(
                        it.findViewById(R.id.downloaded_fragment),
                        "Failed to load downloaded videos! Please check you connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    // Converts received JSON into a list of videos
                    val downloadedResults = jsonConversion(res.body!!.string())
                    // Removes loading spinner and updates recycler with received data
                    requireActivity().runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                        updateRecycler(downloadedResults)
                    }
                }
            }
        })
    }

    // Converts data received from API into a list of videos
    private fun jsonConversion(jsonString: String): List<Video> {
        // Set result and convert json string into json array for additional
        // parsing
        val downloadedResults = mutableListOf<Video>()
        val itemsJsonArray    = JSONTokener(jsonString).nextValue() as JSONArray
        // Cycles through all items in the json array
        for (i in 0 until itemsJsonArray.length()) {
            // Gets video download and queued booleans from json array
            val downloaded = itemsJsonArray.getJSONObject(i).getBoolean("downloaded")
            val queued     = itemsJsonArray.getJSONObject(i).getBoolean("queued")
            // As we only want to show videos that have been downloaded and are not queued,
            // we enforce the below check
            if (downloaded && !queued) {
                // Gets video data from json array
                val videoId      = itemsJsonArray.getJSONObject(i).getString("video_id")
                val title        = itemsJsonArray.getJSONObject(i).getString("title")
                val backendId    = itemsJsonArray.getJSONObject(i).getInt("id")
                val thumbnailUrl = itemsJsonArray.getJSONObject(i).getString("thumbnail")
                // Old code
                // val url = URL(thumbnailUrl)

                // Constructs and adds video to downloaded results
                downloadedResults.add(
                    Video(videoId, title,
                    Thumbnail(thumbnailUrl),
                    downloaded = downloaded,
                    backendId = backendId)
                )

                // Old code
                // if (!((activity?.application as App).checkBitmap(videoId))) {
                //     val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                //    (activity?.application as App).storeBitmap(videoId, bitmap)
                // }
            }
        }

        return downloadedResults
    }

    // Updates the downloads recycler
    private fun updateRecycler(downloadedResults: List<Video>) {
        // Gets recycler and layout manager
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.delete_recycler)
        val layoutManager = LinearLayoutManager(context)
        // Set up video adapter with function for on click events
        videoAdapter = VideoDownloadedAdapter(downloadedResults
                        as MutableList<Video>, activity?.application) {deleteVideo(it)}
        // Set up recycler view
        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    // Callback function for recycler view item on click event
    private fun deleteVideo(item: Video) {
        // URI for deleting a video via the API
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("videos")
        val deleteUrl = urlBuilder.build().toString()
        // Request body with required key value pair
        val requestBody = FormBody.Builder()
            .add("id", item.backendId.toString())
            .build()
        // Bearer must be specified when using the API
        val request = Request.Builder()
            .method("DELETE",  requestBody)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(deleteUrl)
            .build()
        // API request with callback
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("DELETE REQUEST FAILED", e.printStackTrace().toString())
                // Present toast to user describing that the delete failed
                activity?.let {
                    Snackbar.make(
                        it.findViewById(R.id.downloaded_fragment),
                        item.title + " has NOT been deleted! Please try again!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    // Throws exception If response was unsuccessful
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    // Get item index
                    val itemIndex = videoAdapter.getItemIndex(item)
                    // Remove item from view
                    videoAdapter.removeItem(itemIndex)
                    // Update UI
                    requireActivity().runOnUiThread {
                        videoAdapter.notifyItemRemoved(itemIndex)
                    }
                    // Present toast to user describing the change
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