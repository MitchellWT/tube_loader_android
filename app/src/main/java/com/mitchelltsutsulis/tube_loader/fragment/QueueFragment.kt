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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.android.material.snackbar.Snackbar
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoQueueAdapter
import com.mitchelltsutsulis.tube_loader.model.Video
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class QueueFragment : Fragment() {
    private lateinit var videoAdapter: VideoQueueAdapter
    private val httpClient = OkHttpClient()
    private val objectMapper = jacksonObjectMapper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.queue_fragment, container, false)

    override fun onStart() {
        super.onStart()
        getQueue()
        getQueueStatus()
        lifecycleScope.launch { pollForQueueUpdate() }
    }

    private fun getQueueStatus() {
        val authToken = (requireActivity().application as App).basicAuthStr
        val url = Uri.Builder()
            .scheme(getString(R.string.server_protocol))
            .encodedAuthority(getString(R.string.server_address))
            .appendPath("queue")
            .build()
            .toString()
        val req = Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", "Basic $authToken")
            .build()
        httpClient.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET QUEUE STATE REQ FAIL", e.message.toString())
                if (!isAdded || view == null || activity == null) return
                val activity = requireActivity()
                Snackbar.make(
                    requireView(),
                    "Unable to get queue state! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded || view == null || activity == null) return
                if (!response.isSuccessful) {
                    Log.i(
                        "GET QUEUE STATE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        requireView(),
                        "Unable to get queue state! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(requireActivity().findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val queueState = objectMapper
                    .readValue<Map<String, String>>(response.body?.string() ?: "")
                    .getValue("active")
                    .toBoolean()
                val queueButton = requireView().findViewById<Button>(R.id.queue_status)
                queueButton.text = getString(if (queueState) R.string.queue_running else R.string.queue_stop)
                queueButton.setOnClickListener { setQueueStatus(it) }
            }
        })
    }

    private fun setQueueStatus(queueButton: View) {
        val authToken = (requireActivity().application as App).basicAuthStr
        val url = Uri.Builder()
            .scheme(getString(R.string.server_protocol))
            .encodedAuthority(getString(R.string.server_address))
            .appendPath("queue")
            .build()
            .toString()
        val req = Request.Builder()
            .put("".toRequestBody())
            .url(url)
            .addHeader("Authorization", "Basic $authToken")
            .build()
        httpClient.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("TOGGLE QUEUE STATE REQ FAIL", e.message.toString())
                if (!isAdded || view == null) return
                val activity = requireActivity()
                Snackbar.make(
                    requireView(),
                    "Unable to change queue state! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded || view == null || activity == null) return
                if (!response.isSuccessful) {
                    Log.i(
                        "GET QUEUE STATE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        requireView(),
                        "Unable to get queue state! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(requireActivity().findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val queueState = objectMapper
                    .readValue<Map<String, String>>(response.body?.string() ?: "")
                    .getValue("active")
                    .toBoolean()
                (queueButton as Button).text = getString(if (queueState) R.string.queue_running else R.string.queue_stop)
                Snackbar.make(
                    requireView(),
                    if (queueState) "Queue started!" else "Queue stopped!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(requireActivity().findViewById(R.id.bottom_navigation_bar)).show()
            }
        })
    }

    private fun getQueue() {
        if (!isAdded) return
        val loadingSpinner = requireView().findViewById<ProgressBar>(R.id.loading_spinner)
        requireActivity().runOnUiThread { loadingSpinner.visibility = View.VISIBLE }
        val authToken = (requireActivity().application as App).basicAuthStr
        val url = Uri.Builder()
            .scheme(getString(R.string.server_protocol))
            .encodedAuthority(getString(R.string.server_address))
            .appendPath("videos")
            .appendPath("not")
            .appendPath("downloaded")
            .appendQueryParameter("amount", "50")
            .appendQueryParameter("page", "0")
            .build()
            .toString()
        val req = Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", "Basic $authToken")
            .build()
        httpClient.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("GET QUEUE REQ FAIL", e.message.toString())
                if (!isAdded || view == null || activity == null) return
                val activity = requireActivity()
                activity.runOnUiThread { loadingSpinner.visibility = View.GONE }
                Snackbar.make(
                    requireView(),
                    "Unable to get queue data! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded || view == null || activity == null) return
                if (!response.isSuccessful) {
                    Log.i(
                        "GET QUEUE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        requireView(),
                        "Unable to get queue data! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(requireActivity().findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val videos = objectMapper.readTree(response.body?.string())
                val res = Video.jsonSeqToList(videos.asSequence())
                requireActivity().runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                    updateRecycler(res)
                }
            }
        })
    }

    private fun updateRecycler(videos: List<Video>) {
        val videoRecycler = requireView().findViewById<RecyclerView>(R.id.queue_recycler)
        val layoutManager = LinearLayoutManager(context)
        videoAdapter = VideoQueueAdapter(
            videos.toMutableList(),
            requireActivity().application
        ) { toggleQueuedState(it) }
        videoRecycler.layoutManager = layoutManager
        videoRecycler.adapter = videoAdapter
    }

    private fun toggleQueuedState(item: Video) {
        val authToken = (requireActivity().application as App).basicAuthStr
        val url = Uri.Builder()
            .scheme(getString(R.string.server_protocol))
            .encodedAuthority(getString(R.string.server_address))
            .appendPath("video")
            .appendPath(item.backendId.toString())
            .appendPath("queued")
            .build()
            .toString()
        val req = Request.Builder()
            .put("".toRequestBody())
            .url(url)
            .addHeader("Authorization", "Basic $authToken")
            .build()
        httpClient.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("TOGGLE REQ FAIL", e.message.toString())
                if (!isAdded || view == null || activity == null) return
                val activity = requireActivity()
                Snackbar.make(
                    requireView(),
                    "${item.title} has NOT been updated! Please check you connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded || view == null || activity == null) return
                val activity = requireActivity()
                if (!response.isSuccessful) {
                    Log.i(
                        "TOGGLE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        requireView(),
                        "Unable to get queue data! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val index = videoAdapter.getItemIndex(item)
                val queueState = objectMapper
                    .readValue<Map<String, String>>(response.body?.string() ?: "")
                    .getValue("active")
                    .toBoolean()
                item.queued = queueState
                activity.runOnUiThread { videoAdapter.notifyItemChanged(index) }
                Snackbar.make(
                    requireView(),
                    if (queueState) "${item.title} has been queued!" else "${item.title} has been un-queued",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            }
        })
    }

    private suspend fun pollForQueueUpdate() {
        withContext(Dispatchers.IO) {
            while (true) {
                delay(60000)
                getQueue()
            }
        }
    }
}