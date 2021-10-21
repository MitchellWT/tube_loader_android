package com.mitchelltsutsulis.tube_loader.fragment

import android.graphics.BitmapFactory
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
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.lang.Thread.sleep
import java.net.URL

class QueueFragment : Fragment() {
    private lateinit var loadingSpinner: ProgressBar
    private val httpClient = OkHttpClient()
    private lateinit var videoAdapter: VideoQueueAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.queue_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        loadingSpinner = view?.findViewById(R.id.loading_spinner)!!
        getQueue()

        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("queue")
        val showUrl = urlBuilder.build().toString()

        val request = Request.Builder()
            .method("GET", null)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(showUrl)
            .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET QUEUE STATUS REQUEST FAILED", e.printStackTrace().toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val queueStatus = queueStatusConversion(res.body!!.string())
                    val queueButton = view?.findViewById<Button>(R.id.queue_status)

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

        lifecycleScope.launch {
            pollForQueueUpdates()
        }
    }

    private fun setQueueStatus(queueButton: View) {
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("queue")
        val toggleUrl = urlBuilder.build().toString()

        val requestBody = FormBody.Builder()
            .build()

        val request = Request.Builder()
            .method("PUT", requestBody)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(toggleUrl)
            .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("TOGGLE QUEUE STATUS REQUEST FAILED", e.printStackTrace().toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    var text: String

                    if ((queueButton as Button).text == getString(R.string.queue_stop)) {
                        (queueButton as Button).text = getString(R.string.queue_running)
                        text = "Queue started!"
                    } else {
                        (queueButton as Button).text = getString(R.string.queue_stop)
                        text = "Queue stopped!"
                    }

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

    private fun getQueue() {
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
                Log.i("GET QUEUE REQUEST FAILED", e.printStackTrace().toString())
                requireActivity().runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val queueResults = jsonConversion(res.body!!.string())
                    requireActivity().runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                        updateRecycler(queueResults)
                    }
                }
            }
        })
    }

    private fun queueStatusConversion(jsonString: String): Boolean {
        val active = JSONTokener(jsonString).nextValue() as JSONObject
        return active.getBoolean("active")
    }

    private fun jsonConversion(jsonString: String): List<Video> {
        val queueResults = mutableListOf<Video>()
        val itemsJsonArray = JSONTokener(jsonString).nextValue() as JSONArray

        for (i in 0 until itemsJsonArray.length()) {
            val downloaded = itemsJsonArray.getJSONObject(i).getBoolean("downloaded")

            if (!downloaded) {
                val videoId = itemsJsonArray.getJSONObject(i).getString("video_id")
                val title = itemsJsonArray.getJSONObject(i).getString("title")
                val queued = itemsJsonArray.getJSONObject(i).getBoolean("queued")
                val backendId = itemsJsonArray.getJSONObject(i).getInt("id")
                val thumbnailUrl = itemsJsonArray.getJSONObject(i).getString("thumbnail")
                //val url = URL(thumbnailUrl)

                queueResults.add(
                    Video(videoId, title,
                    Thumbnail(thumbnailUrl),
                    queued,
                    backendId = backendId)
                )

                //if (!((activity?.application as App).checkBitmap(videoId))) {
                //    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                //    (activity?.application as App).storeBitmap(videoId, bitmap)
                //}
            }
        }

        return queueResults
    }

    private fun updateRecycler(queueResults: List<Video>) {
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.queue_recycler)
        val layoutManager = LinearLayoutManager(context)
        videoAdapter = VideoQueueAdapter(queueResults as MutableList<Video>, activity?.application) {toggleQueuedState(it)}

        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    private fun toggleQueuedState(item: Video) {
        val urlBuilder = Uri.Builder()
            .scheme("http")
            .encodedAuthority(getString(R.string.server_ip))
            .appendPath("api")
            .appendPath("videos")
            .appendPath("queued")
        val toggleUrl = urlBuilder.build().toString()

        val requestBody = FormBody.Builder()
            .add("id", item.backendId.toString())
            .build()

        val request = Request.Builder()
            .method("PUT",  requestBody)
            .header("Authorization", "Bearer " + getString(R.string.api_token))
            .url(toggleUrl)
            .build()

        httpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("TOGGLE REQUEST FAILED", e.printStackTrace().toString())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    if (!res.isSuccessful) throw IOException("ERROR: $res")
                    val itemIndex = videoAdapter.getItemIndex(item)
                    item.queued = !item.queued

                    requireActivity().runOnUiThread {
                        videoAdapter.notifyItemChanged(itemIndex)
                    }

                    val text = if (item.queued) item.title + " has been queued!"
                    else item.title + " has been un-queued!"

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

    private suspend fun pollForQueueUpdates() {
        withContext(Dispatchers.IO) {
            while (true) {
                delay(60000)
                getQueue()
            }
        }
    }
}