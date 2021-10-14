package com.mitchelltsutsulis.tube_loader.fragment

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.net.URL

class SearchResultFragment: Fragment() {
    private val httpClient = OkHttpClient()
    private var queryURL: String? = null
    private val videoContract = registerForActivityResult(VideoActivity.Contract()) { status_code ->
        activity?.let {
            val text = when(status_code) {
                201 -> "Video added!"
                300 -> "300"
                else -> "Unknown error occurred, try again!"
            }

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

        searchString?.let {
            queryURL = "https://www.googleapis.com/youtube/v3/search?" +
                    "part=snippet&" +
                    "fields=items(id(videoId),snippet(title,thumbnails(high)))&" +
                    "q=$searchString&" +
                    "type=video&" +
                    "key=AIzaSyCkh8pcOAd3yJ-QqkXEWnTYZqn8x9GMIP8"
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
        youtubeSearch()
    }

    private fun youtubeSearch() {
        queryURL?.let {
            val request = Request.Builder()
                .url(it)
                .build()

            httpClient.newCall(request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("API REQUEST FAILED", e.printStackTrace().toString())
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        if (!res.isSuccessful) throw IOException("ERROR: $res")
                        val searchResults = jsonConversion(res.body!!.string())
                        requireActivity().runOnUiThread {
                            updateRecycler(searchResults)
                        }
                    }
                }
            })
        }
    }

    private fun jsonConversion(jsonString: String): List<Video> {
        val searchResults = mutableListOf<Video>()
        val itemsJsonArray = (JSONTokener(jsonString).nextValue() as JSONObject)
            .getJSONArray("items")

        for (i in 0 until itemsJsonArray.length()) {
            val videoId = itemsJsonArray.getJSONObject(i).getJSONObject("id").getString("videoId")
            val snippet = itemsJsonArray.getJSONObject(i).getJSONObject("snippet")
            val title = snippet.getString("title")
            val thumbnail = snippet.getJSONObject("thumbnails").getJSONObject("high")
            val url = URL(thumbnail.getString("url"))
            searchResults.add(
                Video(videoId, title,
                Thumbnail(thumbnail.getString("url"),
                          thumbnail.getString("width"),
                          thumbnail.getString("height"))
                )
            )

            val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
            (activity?.application as App).storeBitmap(videoId, bitmap)
        }

        return searchResults
    }

    private fun updateRecycler(searchResults: List<Video>) {
        val videoRecycler = view?.findViewById<RecyclerView>(R.id.video_recycler)
        val layoutManager = LinearLayoutManager(context)
        val videoAdapter = VideoSearchAdapter(searchResults, activity?.application) {videoActivity(it)}

        videoRecycler?.let {
            it.layoutManager = layoutManager
            it.adapter = videoAdapter
        }
    }

    private fun videoActivity(item: Video) {
        videoContract.launch(item)
    }
}