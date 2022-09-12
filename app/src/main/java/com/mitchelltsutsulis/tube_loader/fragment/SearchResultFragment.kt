package com.mitchelltsutsulis.tube_loader.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.material.snackbar.Snackbar
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoSearchAdapter
import com.mitchelltsutsulis.tube_loader.model.Thumbnail
import com.mitchelltsutsulis.tube_loader.model.Video
import okhttp3.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class SearchResultFragment: Fragment() {
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var url: String
    private var searchString = ""
    private val httpClient = OkHttpClient()
    private val paused = AtomicBoolean(false)
    private val objectMapper = jacksonObjectMapper()

    private val videoContract = registerForActivityResult(VideoActivity.Contract()) { statusCode ->
        Snackbar.make(
            requireView(),
            if (statusCode in 200..299) "Video added!" else "Error occurred, try again!",
            Snackbar.LENGTH_SHORT
        ).setAnchorView(requireActivity().findViewById(R.id.bottom_navigation_bar)).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchString = arguments?.getString("searchString") ?: ""
        url = "https://www.googleapis.com/youtube/v3/search?" +
            "part=snippet&" +
            "fields=items(id(videoId),snippet(title,thumbnails(high)))&" +
            "q=$searchString&" +
            "type=video&" +
            "maxResults=20&" +
            "key=${getString(R.string.youtube_api_key)}"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.search_result_fragment, container, false)

    override fun onStart() {
        super.onStart()
        if (!paused.get() && searchString.isNotEmpty()) {
            loadingSpinner = requireView().findViewById(R.id.loading_spinner)
            youtubeSearch()
        }
    }

    override fun onPause() {
        super.onPause()
        paused.set(true)
    }

    private fun youtubeSearch() {
        loadingSpinner.visibility = View.VISIBLE
        val req = Request.Builder()
            .url(url)
            .build()
        httpClient.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.i("API REQ FAIL", e.message.toString())
                if (!isAdded || view == null || activity == null) return
                val activity = requireActivity()
                activity.runOnUiThread { loadingSpinner.visibility = View.GONE }
                Snackbar.make(
                    requireView(),
                    "Unable to search youtube! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!isAdded || view == null || activity == null) return
                val activity = requireActivity()
                if (!response.isSuccessful) {
                    Log.i(
                        "API REQ FAIL",
                        "Status code: ${response.code}, message: ${response.message}"
                    )
                    Snackbar.make(
                        requireView(),
                        "Unable to search youtube! Please check your connection!",
                        Snackbar.LENGTH_SHORT
                    ).setAnchorView(activity.findViewById(R.id.bottom_navigation_bar)).show()
                    return
                }
                val videos = objectMapper.readTree(response.body?.string()).get("items")
                val searchRes = youtubeResToVideoList(videos.asSequence())
                activity.runOnUiThread {
                    loadingSpinner.visibility = View.GONE
                    updateRecycler(searchRes)
                }
            }
        })
    }

    private fun youtubeResToVideoList(videos: Sequence<JsonNode>) = videos.map {
        val videoId = it.get("id").get("videoId").asText()
        val snippet = it.get("snippet")
        val title = snippet.get("title").asText()
        val thumbnail = snippet.get("thumbnails").get("high")
        Video(videoId, title, Thumbnail(
            thumbnail.get("url").asText(),
            thumbnail.get("width").asText(),
            thumbnail.get("height").asText()
        ))
    }.toList()

    private fun updateRecycler(searchResults: List<Video>) {
        val videoRecycler = requireView().findViewById<RecyclerView>(R.id.video_recycler)
        val layoutManager = LinearLayoutManager(context)
        val videoAdapter = VideoSearchAdapter(searchResults) { videoActivity(it) }
        videoRecycler.layoutManager = layoutManager
        videoRecycler.adapter = videoAdapter
    }

    private fun videoActivity(item: Video) = videoContract.launch(item)
}