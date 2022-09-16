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
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoDownloadedAdapter
import com.mitchelltsutsulis.tube_loader.model.Video
import okhttp3.*
import java.io.IOException

class DownloadedFragment : Fragment() {
    private lateinit var videoAdapter: VideoDownloadedAdapter
    private val httpClient = OkHttpClient()
    private val objectMapper = jacksonObjectMapper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.downloaded_fragment, container, false)

    override fun onStart() {
        super.onStart()
        getDownloaded()
    }

    private fun getDownloaded() {
        try {
            val loadingSpinner = requireView().findViewById<ProgressBar>(R.id.loading_spinner)
            loadingSpinner.visibility = View.VISIBLE
            val app = (requireActivity().application as App)
            val url = Uri.Builder()
                .scheme(app.getServerScheme())
                .encodedAuthority(app.getServerAuthority())
                .appendPath("videos")
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
            httpClient.newCall(req).enqueue(GetDownloadedCallback(this, loadingSpinner))
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    class GetDownloadedCallback(
        private val downFrag: DownloadedFragment,
        private val loadSpin: ProgressBar
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("GET DOWNLOAD REQ FAIL", e.message.toString())
            try {
                val activity = downFrag.requireActivity()
                activity.runOnUiThread { loadSpin.visibility = View.GONE }
                Snackbar.make(
                    downFrag.requireView(),
                    "Failed to load downloaded videos! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val activity = downFrag.requireActivity()
                if (!response.isSuccessful) {
                    Log.i(
                        "GET DOWNLOAD REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        downFrag.requireView(),
                        "Failed to load downloaded videos! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val videos = downFrag.objectMapper.readTree(response.body?.string())
                val res = Video.jsonSeqToList(videos.asSequence())
                activity.runOnUiThread {
                    loadSpin.visibility = View.GONE
                    downFrag.updateRecycler(res)
                }
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }
    }

    private fun updateRecycler(videos: List<Video>) {
        val videoRecycler = requireView().findViewById<RecyclerView>(R.id.delete_recycler)
        val layoutManager = LinearLayoutManager(context)
        videoAdapter = VideoDownloadedAdapter(videos.toMutableList()) { deleteVideo(it) }
        videoRecycler.layoutManager = layoutManager
        videoRecycler.adapter = videoAdapter
    }

    private fun deleteVideo(item: Video) {
        try {
            val app = (requireActivity().application as App)
            val url = Uri.Builder()
                .scheme(app.getServerScheme())
                .encodedAuthority(app.getServerAuthority())
                .appendPath("video")
                .appendPath(item.backendId.toString())
                .build()
                .toString()
            val req = Request.Builder()
                .delete()
                .url(url)
                .addHeader("Authorization", "Basic ${app.getAuthToken()}")
                .build()
            httpClient.newCall(req).enqueue(DeleteVideoCallback(this, item))
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    class DeleteVideoCallback(private val downFrag: DownloadedFragment, private val item: Video) :
        Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("DELETE REQ FAIL", e.message.toString())
            try {
                val activity = downFrag.requireActivity()
                Snackbar.make(
                    downFrag.requireView(),
                    "${item.title} has NOT been deleted! Please try again!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val activity = downFrag.requireActivity()
                if (!response.isSuccessful) {
                    Log.i(
                        "DELETE REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        downFrag.requireView(),
                        "${item.title} has NOT been deleted! Please try again!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val index = downFrag.videoAdapter.getItemIndex(item)
                downFrag.videoAdapter.removeItem(index)
                activity.runOnUiThread { downFrag.videoAdapter.notifyItemRemoved(index) }
                Snackbar.make(
                    downFrag.requireView(),
                    "${item.title} has been deleted!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            } catch (e: Exception) {
                Log.i("EXCEPTION", e.message.toString())
            }
        }
    }
}
