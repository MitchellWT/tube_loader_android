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
import com.google.android.material.snackbar.Snackbar
import com.mitchelltsutsulis.tube_loader.*
import com.mitchelltsutsulis.tube_loader.adapter.VideoSearchAdapter
import com.mitchelltsutsulis.tube_loader.model.Thumbnail
import com.mitchelltsutsulis.tube_loader.model.Video
import okhttp3.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

class SearchResultFragment: Fragment() {
    private lateinit var  loadingSpinner: ProgressBar
    private var queryURL: String? = null
    private val httpClient = OkHttpClient()
    private var isPaused   = false

    // Contract for video activity, only used to change the toast message to user
    private val videoContract = registerForActivityResult(VideoActivity.Contract()) { status_code ->
        activity?.let {
            // Ideally remove magic numbers
            val text = when(status_code) {
                200 -> "Video added!"
                300 -> "300"
                else -> "Unknown error occurred, try again!"
            }
            // Present message only when the back button was not pressed on the video activity
            // screen (300 == back press)
            if (text != "300") {
                Snackbar.make(
                    it.findViewById(R.id.search_fragment),
                    text,
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(it.findViewById(R.id.bottom_navigation_bar)).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val searchString = arguments?.getString("searchString")
        // Setting query URL, should remove some data to res/strings
        searchString?.let {
            queryURL = "https://www.googleapis.com/youtube/v3/search?" +
                    "part=snippet&" +
                    "fields=items(id(videoId),snippet(title,thumbnails(high)))&" +
                    "q=$searchString&" +
                    "type=video&" +
                    "maxResults=20&" +
                    "key=${getString(R.string.youtube_api_key)}"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.search_result_fragment, container, false)
    }

    override fun onStart() {
        super.onStart()
        // only performs a youtube search If the fragment was not previously paused
        if (!isPaused) {
            // Gets loading spinner
            loadingSpinner = view?.findViewById(R.id.loading_spinner)!!
            youtubeSearch()
        }
    }

    // Sets boolean flag for being paused
    override fun onPause() {
        super.onPause()
        isPaused = true
    }

    // Performs a youtube search using the Youtube API
    private fun youtubeSearch() {
        // Only ran when the query URL is not null
        queryURL?.let {
            // Makes the loading spinner visible
            loadingSpinner.visibility = View.VISIBLE
            // Build request with the query URL and its URL parameters
            val request = Request.Builder()
                .url(it)
                .build()
            // API request with callback
            httpClient.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("API REQUEST FAILED", e.printStackTrace().toString())
                    // Presents toast to user stating that the request failed
                    requireActivity().runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                    }
                    // Presents toast to user stating that the request failed
                    activity?.let { act ->
                        Snackbar.make(
                            act.findViewById(R.id.search_fragment),
                            "Unable to search youtube! Please check your connection!",
                            Snackbar.LENGTH_SHORT
                        ).setAnchorView(act.findViewById(R.id.bottom_navigation_bar)).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        if (!res.isSuccessful) throw IOException("ERROR: $res")
                        // Converts received JSON into a list of videos
                        val searchResults = jsonConversion(res.body!!.string())
                        // Removes loading spinner and updates recycler with received data
                        requireActivity().runOnUiThread {
                            loadingSpinner.visibility = View.GONE
                            updateRecycler(searchResults)
                        }
                    }
                }
            })
        }
    }

    // Converts json string to list of videos
    private fun jsonConversion(jsonString: String): List<Video> {
        // Set result and convert json string into json array for additional
        // parsing
        val searchResults  = mutableListOf<Video>()
        val itemsJsonArray = (JSONTokener(jsonString).nextValue() as JSONObject)
                                .getJSONArray("items")
        // Cycles through all items in the json array
        for (i in 0 until itemsJsonArray.length()) {
            // Get video data from json array
            val videoId   = itemsJsonArray.getJSONObject(i).getJSONObject("id")
                                .getString("videoId")
            val snippet   = itemsJsonArray.getJSONObject(i).getJSONObject("snippet")
            val title     = snippet.getString("title")
            val thumbnail = snippet.getJSONObject("thumbnails").getJSONObject("high")
            // Old code
            // val url = URL(thumbnail.getString("url"))

            // Constructs and adds video to downloaded results
            searchResults.add(
                Video(videoId, title,
                Thumbnail(thumbnail.getString("url"),
                          thumbnail.getString("width"),
                          thumbnail.getString("height"))
                )
            )

            // Old code
            // val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            // (activity?.application as App).storeBitmap(videoId, bitmap)
        }

        return searchResults
    }

    // Updates the search recycler
    private fun updateRecycler(searchResults: List<Video>) {
        // Gets recycler and layout manager
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.video_recycler)
        val layoutManager = LinearLayoutManager(context)
        // Set up video adapter with function for on click events
        val videoAdapter = VideoSearchAdapter(searchResults) {videoActivity(it)}
        // Set up recycler view
        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    // Used as a callback for when a video is pressed, launches video activity
    private fun videoActivity(item: Video) {
        videoContract.launch(item)
    }
}