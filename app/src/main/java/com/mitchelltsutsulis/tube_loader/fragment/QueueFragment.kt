package com.mitchelltsutsulis.tube_loader.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoQueueAdapter
import com.mitchelltsutsulis.tube_loader.model.Thumbnail
import com.mitchelltsutsulis.tube_loader.model.Video
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

class QueueFragment : Fragment() {
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var videoAdapter: VideoQueueAdapter
    private val httpClient = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.queue_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        // Gets loading spinner
        loadingSpinner = view?.findViewById(R.id.loading_spinner)!!
        getQueue()
        getQueueStatus()

        // Starts coroutine for API polling, set to every 60 seconds
        lifecycleScope.launch {
            pollForQueueUpdates()
        }
    }

    // Gets queue status from API
    private fun getQueueStatus() {
        // URI for getting the queue status via API
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("queue")
        val showUrl = urlBuilder.build().toString()
        // GET method does not require a request body
        val request = Request.Builder()
            .method("GET", null)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(showUrl)
            .build()
        // API request with callback
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET QUEUE STATUS REQUEST FAILED", e.printStackTrace().toString())
                if (!isAdded) return
                // Presents toast to user stating that the request failed
                activity?.let {
                    Snackbar.make(
                        it.findViewById(R.id.queue_fragment),
                        "Unable to get queue status! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    // Converts received json into a boolean value
                    val queueStatus = queueStatusConversion(res.body!!.string())
                    val queueButton = view?.findViewById<Button>(R.id.queue_status)
                    // Set listener for queue button, set up so that If this initial
                    // request fails the button will be useless
                    queueButton?.let {
                        it.text = if (queueStatus) getString(R.string.queue_running)
                        else getString(R.string.queue_stop)

                        it.setOnClickListener { listenIt ->
                            setQueueStatus(listenIt)
                        }
                    }
                }
            }
        })
    }

    // Changes the queue status on the API
    private fun setQueueStatus(queueButton: View) {
        // URI for changing the queue status via API
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("queue")
        val toggleUrl = urlBuilder.build().toString()
        // Setup request with request body
        val request = Request.Builder()
            .method("PUT", "".toRequestBody())
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(toggleUrl)
            .build()
        // API request with callback
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("TOGGLE QUEUE STATUS REQUEST FAILED", e.printStackTrace().toString())
                // Presents toast to user stating that the request failed
                activity?.let {
                    Snackbar.make(
                        it.findViewById(R.id.queue_fragment),
                        "Unable to change queue status! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val text: String
                    // Check text of queue button and update text
                    if ((queueButton as Button).text == getString(R.string.queue_stop)) {
                        (queueButton).text = getString(R.string.queue_running)
                        text = "Queue started!"
                    } else {
                        (queueButton).text = getString(R.string.queue_stop)
                        text = "Queue stopped!"
                    }
                    // Presents toast to user that described the queue status
                    activity?.let {
                        Snackbar.make(
                            it.findViewById(R.id.queue_fragment),
                            text,
                            Snackbar.LENGTH_SHORT
                        ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                    }
                }
            }
        })
    }

    // Gets queue data from API
    private fun getQueue() {
        if (!isAdded) return
        // Makes the loading spinner visible
        requireActivity().runOnUiThread {
            loadingSpinner.visibility = View.VISIBLE
        }
        // URI for getting multiple videos via API
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("videos")
            .appendQueryParameter("amount", "50")
            .appendQueryParameter("page", "0")
        val showUrl = urlBuilder.build().toString()
        val request = Request.Builder()
            .method("GET", null)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(showUrl)
            .build()
        // API request with callback
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET QUEUE REQUEST FAILED", e.printStackTrace().toString())
                // Removes loading spinner
                if (!isAdded) return

                requireActivity().runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                }
                // Presents toast to user stating that the request failed
                requireActivity().let {
                    Snackbar.make(
                        it.findViewById(R.id.queue_fragment),
                        "Unable to get queue data! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    // Converts received JSON into a list of videos
                    val queueResults = jsonConversion(res.body!!.string())
                    // Removes loading spinner and updates recycler with received data
                    requireActivity().runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                        updateRecycler(queueResults)
                    }
                }
            }
        })
    }

    // Converts json string to boolean, denoting the queue status
    private fun queueStatusConversion(jsonString: String): Boolean {
        val active = JSONTokener(jsonString).nextValue() as JSONObject
        return active.getBoolean("active")
    }

    // Converts json string to list of queue-able videos (videos that are and are not queued)
    private fun jsonConversion(jsonString: String): List<Video> {
        // Set result and convert json string into json array for additional
        // parsing
        val queueResults = mutableListOf<Video>()
        val itemsJsonArray = JSONTokener(jsonString).nextValue() as JSONArray
        // Cycles through all items in the json array
        for (i in 0 until itemsJsonArray.length()) {
            // Gets video download boolean from json array
            val downloaded = itemsJsonArray.getJSONObject(i).getBoolean("downloaded")
            // As we only want to show videos that have not been downloaded ,
            // we enforce the below check
            if (!downloaded) {
                // Get video data from json array
                val videoId      = itemsJsonArray.getJSONObject(i).getString("video_id")
                val title        = itemsJsonArray.getJSONObject(i).getString("title")
                val queued       = itemsJsonArray.getJSONObject(i).getBoolean("queued")
                val backendId    = itemsJsonArray.getJSONObject(i).getInt("id")
                val thumbnailUrl = itemsJsonArray.getJSONObject(i).getString("thumbnail")
                // Old code
                // val url = URL(thumbnailUrl)

                // Constructs and adds video to downloaded results
                queueResults.add(
                    Video(videoId, title,
                    Thumbnail(thumbnailUrl),
                    queued,
                    backendId = backendId)
                )

                // Old code
                // if (!((activity?.application as App).checkBitmap(videoId))) {
                //     val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                //     (activity?.application as App).storeBitmap(videoId, bitmap)
                // }
            }
        }

        return queueResults
    }

    // Updates the queue recycler
    private fun updateRecycler(queueResults: List<Video>) {
        // Gets recycler and layout manager
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.queue_recycler)
        val layoutManager = LinearLayoutManager(context)
        // Set up video adapter with function for on click events
        videoAdapter = VideoQueueAdapter(queueResults
                        as MutableList<Video>, activity?.application) {toggleQueuedState(it)}
        // Set up recycler view
        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    // Used as a callback for the video queue button, toggles the queue state of said video
    private fun toggleQueuedState(item: Video) {
        // URI for changing the queue status of a video via API
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("video")
            .appendPath(item.backendId.toString())
            .appendPath("queued")
        val toggleUrl = urlBuilder.build().toString()
        // Setup request with request body
        val request = Request.Builder()
            .method("PUT",  "".toRequestBody())
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(toggleUrl)
            .build()
        // API request with callback
        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("TOGGLE REQUEST FAILED", e.printStackTrace().toString())
                // Presents toast to user stating that the request failed
                activity?.let {
                    Snackbar.make(
                        it.findViewById(R.id.queue_fragment),
                        item.title + " has NOT been updated! Please check you connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                }
            }

                override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val itemIndex = videoAdapter.getItemIndex(item)
                    // toggle video data
                    item.queued = !item.queued
                    // Update UI
                    requireActivity().runOnUiThread {
                        videoAdapter.notifyItemChanged(itemIndex)
                    }
                    // Get text for user toast
                    val text = if (item.queued) item.title + " has been queued!"
                    else item.title + " has been un-queued!"
                    // Present toast to user describing the change
                    requireActivity().let {
                        Snackbar.make(
                            it.findViewById(R.id.queue_fragment),
                            text,
                            Snackbar.LENGTH_SHORT
                        ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
                    }
                }
            }
        })
    }

    // Polling coroutine that checks for queue updates
    private suspend fun pollForQueueUpdates() {
        withContext(Dispatchers.IO) {
            while (true) {
                // Waits for 60 seconds
                delay(60000)
                // Gets data from API and update recycler view
                getQueue()
            }
        }
    }
}