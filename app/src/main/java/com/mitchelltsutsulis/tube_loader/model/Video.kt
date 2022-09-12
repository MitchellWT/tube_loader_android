package com.mitchelltsutsulis.tube_loader.model

import android.os.Parcelable
import com.fasterxml.jackson.databind.JsonNode
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(
    val videoId: String,
    var title: String,
    val thumbnail: Thumbnail,
    var queued: Boolean = false,
    val downloaded: Boolean = false,
    val downloadedAt: String = "",
    val backendId: Int = -1,
    val directory: String = "",
) : Parcelable {
    companion object {
        fun jsonSeqToList(videos: Sequence<JsonNode>) = videos.map {
            val backendId = it.get("id").asInt()
            val downloaded = it.get("downloaded").asBoolean()
            val queued = it.get("queued").asBoolean()
            val videoId = it.get("video_id").asText()
            val title = it.get("title").asText()
            val thumbnail = it.get("thumbnail").asText()
            val downloadedAt = it.get("downloaded_at").asText()
            val directory = it.get("directory").asText()
            Video(
                videoId,
                title,
                Thumbnail(thumbnail),
                queued,
                downloaded,
                downloadedAt,
                backendId,
                directory
            )
        }.toList()
    }
}
