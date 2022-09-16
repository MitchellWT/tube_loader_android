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
        try {
            val app = (requireActivity().application as App)
            val url = Uri.Builder()
                .scheme(app.getServerScheme())
                .encodedAuthority(app.getServerAuthority())
                .appendPath("queue")
                .build()
                .toString()
            val req = Request.Builder()
                .get()
                .url(url)
                .addHeader("Authorization", "Basic ${app.getAuthToken()}")
                .build()
            httpClient.newCall(req).enqueue(GetQueueStateCallback(this))
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    class GetQueueStateCallback(private val queueFrag: QueueFragment) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("GET QUEUE STATE REQ FAIL", e.message.toString())
            try {
                val activity = queueFrag.requireActivity()
                Snackbar.make(
                    queueFrag.requireView(),
                    "Unable to get queue state! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                if (!response.isSuccessful) {
                    Log.i(
                        "GET QUEUE STATE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        queueFrag.requireView(),
                        "Unable to get queue state! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(
                        queueFrag.requireActivity().findViewById(R.id.bottom_navigation_bar)
                    ).show()
                    return
                }
                val queueState = queueFrag.objectMapper
                    .readValue<Map<String, String>>(response.body?.string() ?: "")
                    .getValue("active")
                    .toBoolean()
                val queueButton = queueFrag.requireView().findViewById<Button>(R.id.queue_status)
                queueButton.text =
                    queueFrag.getString(if (queueState) R.string.queue_running else R.string.queue_stop)
                queueButton.setOnClickListener { queueFrag.setQueueStatus(it) }
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }
    }

    private fun setQueueStatus(queueButton: View) {
        try {
            val app = (requireActivity().application as App)
            val url = Uri.Builder()
                .scheme(app.getServerScheme())
                .encodedAuthority(app.getServerAuthority())
                .appendPath("queue")
                .build()
                .toString()
            val req = Request.Builder()
                .put("".toRequestBody())
                .url(url)
                .addHeader("Authorization", "Basic ${app.getAuthToken()}")
                .build()
            httpClient.newCall(req).enqueue(SetQueueStateCallback(this, (queueButton as Button)))
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    class SetQueueStateCallback(
        private val queueFrag: QueueFragment,
        private val queueButton: Button
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("TOGGLE QUEUE STATE REQ FAIL", e.message.toString())
            try {
                val activity = queueFrag.requireActivity()
                Snackbar.make(
                    queueFrag.requireView(),
                    "Unable to change queue state! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                if (!response.isSuccessful) {
                    Log.i(
                        "GET QUEUE STATE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        queueFrag.requireView(),
                        "Unable to get queue state! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(
                        queueFrag.requireActivity().findViewById(R.id.bottom_navigation_bar)
                    ).show()
                    return
                }
                val queueState = queueFrag.objectMapper
                    .readValue<Map<String, String>>(response.body?.string() ?: "")
                    .getValue("active")
                    .toBoolean()
                queueButton.text =
                    queueFrag.getString(if (queueState) R.string.queue_running else R.string.queue_stop)
                Snackbar.make(
                    queueFrag.requireView(),
                    if (queueState) "Queue started!" else "Queue stopped!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(
                    queueFrag.requireActivity().findViewById(R.id.bottom_navigation_bar)
                ).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }
    }

    private fun getQueue() {
        try {
            val loadingSpinner = requireView().findViewById<ProgressBar>(R.id.loading_spinner)
            requireActivity().runOnUiThread { loadingSpinner.visibility = View.VISIBLE }
            val app = (requireActivity().application as App)
            val url = Uri.Builder()
                .scheme(app.getServerScheme())
                .encodedAuthority(app.getServerAuthority())
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
                .addHeader("Authorization", "Basic ${app.getAuthToken()}")
                .build()
            httpClient.newCall(req).enqueue(GetQueueCallback(this, loadingSpinner))
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    class GetQueueCallback(
        private val queueFrag: QueueFragment,
        private val loadSpin: ProgressBar
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("GET QUEUE REQ FAIL", e.message.toString())
            try {
                val activity = queueFrag.requireActivity()
                activity.runOnUiThread { loadSpin.visibility = View.GONE }
                Snackbar.make(
                    queueFrag.requireView(),
                    "Unable to get queue data! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                if (!response.isSuccessful) {
                    Log.i(
                        "GET QUEUE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        queueFrag.requireView(),
                        "Unable to get queue data! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(
                        queueFrag.requireActivity().findViewById(R.id.bottom_navigation_bar)
                    ).show()
                    return
                }
                val videos = queueFrag.objectMapper.readTree(response.body?.string())
                val res = Video.jsonSeqToList(videos.asSequence())
                queueFrag.requireActivity().runOnUiThread {
                    loadSpin.visibility = View.GONE
                    queueFrag.updateRecycler(res)
                }
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }
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
        try {
            val app = (requireActivity().application as App)
            val url = Uri.Builder()
                .scheme(app.getServerScheme())
                .encodedAuthority(app.getServerAuthority())
                .appendPath("video")
                .appendPath(item.backendId.toString())
                .appendPath("queued")
                .build()
                .toString()
            val req = Request.Builder()
                .put("".toRequestBody())
                .url(url)
                .addHeader("Authorization", "Basic ${app.getAuthToken()}")
                .build()
            httpClient.newCall(req).enqueue(ToggleQueueStateCallback(this, item))
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    class ToggleQueueStateCallback(private val queueFrag: QueueFragment, private val item: Video) :
        Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("TOGGLE REQ FAIL", e.message.toString())
            try {
                val activity = queueFrag.requireActivity()
                Snackbar.make(
                    queueFrag.requireView(),
                    "${item.title} has NOT been updated! Please check you connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val activity = queueFrag.requireActivity()
                if (!response.isSuccessful) {
                    Log.i(
                        "TOGGLE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        queueFrag.requireView(),
                        "Unable to get queue data! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val index = queueFrag.videoAdapter.getItemIndex(item)
                val queueState = queueFrag.objectMapper
                    .readValue<Map<String, String>>(response.body?.string() ?: "")
                    .getValue("active")
                    .toBoolean()
                item.queued = queueState
                activity.runOnUiThread { queueFrag.videoAdapter.notifyItemChanged(index) }
                Snackbar.make(
                    queueFrag.requireView(),
                    if (queueState) "${item.title} has been queued!" else "${item.title} has been un-queued",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }
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